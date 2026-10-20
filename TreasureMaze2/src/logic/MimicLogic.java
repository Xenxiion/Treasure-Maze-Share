package logic; // package for game rules

import game.GameMap; // finds the start, chest and exit on the map
import java.awt.Point; // integer x/y coordinate
import java.util.ArrayList; // growable list
import java.util.Collections; // read-only wrapper helper
import java.util.List; // list interface
import java.util.Random; // random patrol goals and search spots

/**
 * Real-time Mimic AI. It is updated by the game timer, so mimics move even when the player stands still.
 * States: PATROL (wander to random places all over the map) -> CHASE (sees the player, walls block sight)
 *   -> INVESTIGATE (walk to the last known position or to a noise) -> SEARCH (look around 3 spots) -> PATROL.
 * Hearing: the player walking near the chest, or opening it, makes noise that only nearby mimics (by path) hear.
 * Fairness: mimics are always slower than a player who keeps moving, and they give up after a short search.
 */
// HOW THE MIMIC AI WORKS: every mimic is a tiny "state machine" - it is always in exactly one state:
//   PATROL      walks to a random goal anywhere on the map, then picks a new goal.
//   CHASE       sees the player (close enough and no wall in the way): walks toward them.
//   INVESTIGATE lost sight or heard a noise: walks to the LAST place it knew about (not to the player's real position).
//   SEARCH      looks at 3 random spots near that place, then goes back to PATROL.
//   WAKING      a fake chest just opened: stands still for 0.5 s so the player can run.
// Each update() call decides the state first, then takes at most one step. A step is allowed only when the mimic's
// own timer says so (every 340-650 ms), which is why mimics are slower than a walking player.
public class MimicLogic { // controls all mimics on a level

    /** One mimic. */
    private static final class Mimic { // data holder
        final Point pos; // current cell (changes as it moves)
        final Point home; // spawn cell
        int state = PATROL; // current behaviour state
        final Point last = new Point(); // last known player position (or noise position)
        Point goal; // current patrol goal or search spot (null = pick a new one)
        int searchLeft; // search spots still to visit
        long nextMove; // time (ms) of the next step, 0 = not scheduled yet
        boolean disguised; // true: looks like a closed treasure chest and does not move until the player tries to open it
        int slow = 1; // step time multiplier: 1 for regular mimics, 3 for the former fake chest (3 times slower)
        long stunUntil; // game time (ms) until which the mimic is stunned (0 = not stunned)
        long wakeEnd; // time (ms) when a just-revealed mimic starts chasing (the player's reaction time)

        Mimic(Point home) { // create a mimic
            this.home = new Point(home); // keep a copy of the spawn cell
            this.pos = new Point(home); // starts at home
        } // end of constructor
    } // end of Mimic

    private static final int PATROL = 0; // wandering
    private static final int CHASE = 1; // sees the player
    private static final int INVESTIGATE = 2; // walking to the last known position or noise
    private static final int SEARCH = 3; // looking around that position
    private static final int WAKING = 4; // just revealed: shows itself for a moment before it chases
    private static final long WAKE_MILLIS = 500; // reaction time: a revealed mimic stands still this long so the player can run
    private static final int DECOY_SLOW = 3; // the former fake chest moves 3 times slower than regular mimics
    private static final int INTERCEPT_PERCENT = 35; // chance that a chasing mimic aims ahead of the player instead of at them
    private static final int GUARD_PERCENT = 30; // chance that a new patrol goal is near the chest or the exit
    private static final int SEARCH_SPOTS = 3; // spots checked before giving up
    private static final int SEARCH_RADIUS = 4; // search area around the last known position
    private static final int MIN_GOAL_DISTANCE = 8; // patrol goals are at least this far away, so mimics cover the map

    private final Random random = new Random(); // random choices
    private final List<Mimic> mimics = new ArrayList<>(); // all mimics of the level
    private final List<Point> guardSpots = new ArrayList<>(); // places a patrolling mimic likes to check: the chest and the exit
    private int prevPx = -1; // player column at the previous update (-1 = unknown)
    private int prevPy = -1; // player row at the previous update
    private int heading = 0; // player's last move: 0 none, 1 right, 2 left, 3 down, 4 up
    private long clockNow; // game time of the latest update (used to check stuns)
    private int sightRange = 7; // how far (in cells) a mimic can see along a clear line
    private int patrolMillis = 650; // milliseconds per step while patrolling
    private int alertMillis = 420; // milliseconds per step while chasing, investigating or searching (still slower than the player)

    /** Patrol radius (in steps from the home cell) for a level. Used by the map generator too. */
    public static int patrolRadiusFor(int level) { // area size
        return level >= 3 ? 7 : 6; // level 3 mimics roam a little more
    } // end of patrolRadiusFor

    /**
     * Builds a patrol route inside the area of {@code radius} steps around {@code home}:
     * edge A = farthest cell from home, edge B = the cell of the area farthest from A.
     * Returns the cells from A to B (all walkable, because it is a real shortest path), or null.
     */
    public static List<Point> buildPatrolRoute(String[] map, Point home, int radius) { // compute a route
        int[][] fromHome = Pathfinder.distances(map, home, false); // distances from home
        Point a = null; // edge A
        int bestA = -1; // its distance from home
        for (int y = 0; y < fromHome.length; y++) { // each row
            for (int x = 0; x < fromHome[y].length; x++) { // each column
                int d = fromHome[y][x]; // distance of this cell
                if (d >= 0 && d <= radius && d > bestA) { // inside the area and farther than before
                    bestA = d; // remember the distance
                    a = new Point(x, y); // remember the cell
                } // end of improvement check
            } // end of column loop
        } // end of row loop
        if (a == null) return null; // no cells at all
        int[][] fromA = Pathfinder.distances(map, a, false); // distances from edge A
        Point b = null; // edge B
        int bestB = -1; // its distance from A
        for (int y = 0; y < fromA.length; y++) { // each row
            for (int x = 0; x < fromA[y].length; x++) { // each column
                if (fromHome[y][x] >= 0 && fromHome[y][x] <= radius && fromA[y][x] > bestB) { // inside the area, farther from A
                    bestB = fromA[y][x]; // remember the distance
                    b = new Point(x, y); // remember the cell
                } // end of improvement check
            } // end of column loop
        } // end of row loop
        if (b == null) return null; // should not happen
        List<Point> route = new ArrayList<>(); // path A -> B
        Point cur = a; // start at A
        route.add(new Point(cur)); // first cell
        int guard = 0; // protects against endless loops
        while (!cur.equals(b) && guard++ < 200) { // walk toward B
            Point next = Pathfinder.nextStep(map, cur, b); // one step along the shortest path
            if (next == null) return null; // path broke
            cur = next; // move on
            route.add(new Point(cur)); // add the cell
        } // end of walk loop
        return route; // finished route
    } // end of buildPatrolRoute

    public void loadMimicsFromMap(String[] map, int level) { // read mimics from a level map
        mimics.clear(); // forget old mimics
        sightRange = level >= 3 ? 8 : 7; // level 3 mimics see a little farther
        patrolMillis = level >= 3 ? 500 : 650; // patrol pace
        alertMillis = level >= 3 ? 340 : 420; // alert pace (slightly faster than patrol)
        guardSpots.clear(); // forget old guard spots
        prevPx = -1; // forget the old player position
        heading = 0; // no heading yet
        clockNow = 0; // fresh level
        if (map == null) return; // nothing to read
        Point chestCell = GameMap.find(map, 'T'); // the chest
        Point exitCell = GameMap.find(map, 'E'); // the exit
        if (chestCell != null) guardSpots.add(chestCell); // patrols sometimes check the chest
        if (exitCell != null) guardSpots.add(exitCell); // and the exit, where the player must go
        for (int r = 0; r < map.length; r++) { // for each row
            for (int c = 0; c < map[r].length(); c++) { // for each column
                if (map[r].charAt(c) == 'M') { // mimic symbol = spawn cell
                    mimics.add(new Mimic(new Point(c, r))); // register the mimic
                } // end of symbol check
            } // end of column loop
        } // end of row loop
        addDisguised(map, chestCell, exitCell); // every level hides exactly one fake chest (it counts as one of the level's mimics)
    } // end of loadMimicsFromMap

    /**
     * Hides one mimic that looks like a closed chest. It is placed in a dead end (so it can never block a route),
     * far from the start, and away from the real chest and the exit. It does not move until the player comes close.
     */
    private void addDisguised(String[] map, Point chestCell, Point exitCell) { // place the fake chest
        Point start = GameMap.find(map, 'P'); // where the player starts
        if (start == null) return; // incomplete map
        int[][] fromStart = Pathfinder.distances(map, start, false); // distances from the start
        int[][] fromChest = chestCell == null ? null : Pathfinder.distances(map, chestCell, false); // distances from the real chest
        int[][] fromExit = exitCell == null ? null : Pathfinder.distances(map, exitCell, false); // distances from the exit
        // Try strict rules first; if a (small) map has no such cell, relax the distances step by step so the level always gets its fake chest.
        int[][] tiers = {{12, 6, 4, 1}, {8, 4, 3, 1}, {4, 2, 2, 1}, {2, 1, 2, 1}, {2, 1, 2, 2}}; // start, chest, exit distance, max open neighbours
        List<Point> spots = new ArrayList<>(); // possible cells
        for (int[] tier : tiers) { // strict to relaxed
            for (int y = 1; y < map.length - 1; y++) { // each row
                for (int x = 1; x < map[y].length() - 1; x++) { // each column
                    if (map[y].charAt(x) != '.' || fromStart[y][x] < tier[0]) continue; // plain floor (no bomb, coin, chest, exit, start), far from the start
                    if (fromChest != null && fromChest[y][x] < tier[1]) continue; // away from the real chest
                    if (fromExit != null && fromExit[y][x] < tier[2]) continue; // away from the exit
                    if (bombNear(map, x, y)) continue; // never covered by, or hidden behind, a bomb: no bomb in the 8 cells around it
                    int open = 0; // walkable neighbours
                    if (Pathfinder.walkable(map, x + 1, y, false)) open++; // right
                    if (Pathfinder.walkable(map, x - 1, y, false)) open++; // left
                    if (Pathfinder.walkable(map, x, y + 1, false)) open++; // down
                    if (Pathfinder.walkable(map, x, y - 1, false)) open++; // up
                    if (open <= tier[3]) spots.add(new Point(x, y)); // a dead end (last tier also allows corridors)
                } // end of column loop
            } // end of row loop
            if (!spots.isEmpty()) break; // found cells at this strictness
        } // end of tiers
        if (spots.isEmpty()) return; // practically impossible: no free floor at all
        Mimic m = new Mimic(spots.get(random.nextInt(spots.size()))); // create it
        m.disguised = true; // looks like a chest
        m.slow = DECOY_SLOW; // and is slow once revealed
        mimics.add(m); // register it
    } // end of addDisguised

    /** True when no wall blocks the straight line between two cells (simple line walk). */
    // Line of sight: walk along the straight line between two cells and look at each cell in between.
    // If one of them is a wall the view is blocked. This is why mimics cannot see you through walls.
    private static boolean canSee(String[] map, Point a, Point b) { // line of sight test
        int steps = Math.max(Math.abs(b.x - a.x), Math.abs(b.y - a.y)); // number of samples along the line
        for (int i = 1; i < steps; i++) { // each cell strictly between the two
            int x = a.x + Math.round((float) (b.x - a.x) * i / steps); // sampled column
            int y = a.y + Math.round((float) (b.y - a.y) * i / steps); // sampled row
            if (!Pathfinder.walkable(map, x, y, false)) return false; // a wall blocks the view
        } // end of loop
        return true; // clear view
    } // end of canSee

    /**
     * Called by the game timer (not by key presses). Moves each mimic when its step time has come.
     * Returns how many mimics moved (the game plays a footstep sound when it is greater than zero).
     */
    // update() is called by the game timer. For every mimic it does: (1) skip it if it is a sleeping fake chest or stunned,
    // (2) check sight, (3) change state if needed, (4) wait until its step time arrives, (5) take ONE step with Pathfinder.nextStep.
    public int update(int playerX, int playerY, String[] map, long now) { // one AI update
        if (mimics.isEmpty() || map == null) return 0; // no mimics on this level
        Point player = new Point(playerX, playerY); // where the player is
        if (prevPx >= 0 && Math.abs(playerX - prevPx) + Math.abs(playerY - prevPy) == 1) { // the player made one step
            heading = playerX > prevPx ? 1 : playerX < prevPx ? 2 : playerY > prevPy ? 3 : 4; // remember the direction
        } // end of heading
        prevPx = playerX; // remember for the next update
        prevPy = playerY; // remember for the next update
        clockNow = now; // remember the game time
        int moved = 0; // mimics that moved
        for (Mimic m : mimics) { // each mimic
            if (m.disguised) continue; // a fake chest never moves or reacts: only revealAt() (the player opening it) wakes it
            if (now < m.stunUntil) continue; // stunned: frozen until the stun ends, then the normal AI continues
            if (m.state == WAKING) { // just revealed
                if (now < m.wakeEnd) continue; // still frozen: the reaction time
                m.state = CHASE; // now it hunts
                m.last.setLocation(player); // from where it saw the player
                m.nextMove = 0; // schedule the first step normally
            } // end of waking
            boolean sees = Math.abs(m.pos.x - playerX) + Math.abs(m.pos.y - playerY) <= sightRange // close enough
                && canSee(map, m.pos, player); // and no wall in between
            if (sees) { // spotted
                m.state = CHASE; // chase
                m.last.setLocation(player); // remember where
            } else if (m.state == CHASE) { // just lost the player
                m.state = INVESTIGATE; // go to the last known position, not straight to the player
            } // end of sight check

            long interval = (m.state == PATROL ? patrolMillis : alertMillis) * m.slow; // time between steps (x3 for the former fake chest)
            if (m.nextMove == 0) m.nextMove = now + interval; // first update: schedule, so the player gets a head start
            if (now < m.nextMove) continue; // not time to step yet
            m.nextMove = now + interval; // schedule the next step

            if (m.state == INVESTIGATE && m.pos.equals(m.last)) { // arrived
                m.state = SEARCH; // start searching
                m.searchLeft = SEARCH_SPOTS; // spots to check
                m.goal = null; // pick the first spot below
            } // end of arrival check
            Point next = null; // the cell to step to
            if (m.state == CHASE) { // chasing
                Point target = player; // by default go straight for the player
                if (Math.abs(m.pos.x - playerX) + Math.abs(m.pos.y - playerY) > 3 && random.nextInt(100) < INTERCEPT_PERCENT) { // sometimes cut them off
                    target = ahead(map, player); // aim at the cell the player is heading to
                } // end of intercept
                next = Pathfinder.nextStep(map, m.pos, target); // one step toward the target
                if (next == null) next = Pathfinder.nextStep(map, m.pos, player); // fall back to the player
            } else if (m.state == INVESTIGATE) { // going to the last known position
                next = Pathfinder.nextStep(map, m.pos, m.last); // one step toward it
                if (next == null) m.state = PATROL; // unreachable: give up
            } else if (m.state == SEARCH) { // looking around
                if (m.goal == null || m.pos.equals(m.goal)) { // need a new spot
                    if (m.searchLeft <= 0) { // search period is over
                        m.state = PATROL; // resume patrolling
                        m.goal = null; // pick a patrol goal below
                    } else { // next spot
                        m.searchLeft--; // one fewer to go
                        m.goal = randomCell(map, m.last, 1, SEARCH_RADIUS); // random cell near the last known position
                    } // end of spot check
                } // end of new spot
                if (m.state == SEARCH && m.goal != null) next = Pathfinder.nextStep(map, m.pos, m.goal); // step to the spot
            } // end of state handling
            if (m.state == PATROL) { // patrolling (also right after a finished search)
                if (m.goal == null || m.pos.equals(m.goal)) { // need a new goal
                    if (!guardSpots.isEmpty() && random.nextInt(100) < GUARD_PERCENT) { // sometimes guard the chest or the exit
                        m.goal = randomCell(map, guardSpots.get(random.nextInt(guardSpots.size())), 1, 5); // a cell near it
                    } // end of guard goal
                    if (m.goal == null || m.pos.equals(m.goal)) m.goal = randomCell(map, m.pos, MIN_GOAL_DISTANCE, 999); // otherwise anywhere reachable on the map
                } // end of goal check
                if (m.goal != null) next = Pathfinder.nextStep(map, m.pos, m.goal); // one step toward the goal
                if (next == null) m.goal = null; // blocked: choose another goal next time
            } // end of patrol
            if (next != null) { // a step exists
                m.pos.setLocation(next); // take it (always a walkable cell)
                moved++; // count it
            } // end of step check
        } // end of mimic loop
        return moved; // report
    } // end of update

    /** The cell up to 3 steps ahead of the player in the direction they last moved (the player's own cell if blocked). */
    private Point ahead(String[] map, Point player) { // interception target
        int dx = heading == 1 ? 1 : heading == 2 ? -1 : 0; // column step
        int dy = heading == 3 ? 1 : heading == 4 ? -1 : 0; // row step
        for (int k = 3; k >= 1 && (dx != 0 || dy != 0); k--) { // farthest first
            if (Pathfinder.walkable(map, player.x + dx * k, player.y + dy * k, false)) { // open cell
                return new Point(player.x + dx * k, player.y + dy * k); // intercept here
            } // end of open check
        } // end of loop
        return player; // no heading or blocked
    } // end of ahead

    /** True if a black bomb ('B') or a blue bomb pickup ('U') is in the cell or in one of the 8 cells around it. */
    private static boolean bombNear(String[] map, int x, int y) { // keeps bombs away from the fake chest
        for (int dy = -1; dy <= 1; dy++) { // rows around the cell
            for (int dx = -1; dx <= 1; dx++) { // columns around the cell
                char c = map[y + dy].charAt(x + dx); // symbol there
                if (c == 'B' || c == 'U') return true; // a bomb is too close
            } // end of column loop
        } // end of row loop
        return false; // clear
    } // end of bombNear

    /** A random cell whose path distance from the centre is between min and max (falls back to any reachable cell), or null. */
    private Point randomCell(String[] map, Point center, int min, int max) { // choose a goal
        int[][] d = Pathfinder.distances(map, center, false); // distances from the centre
        List<Point> good = new ArrayList<>(); // cells in the wanted range
        List<Point> any = new ArrayList<>(); // any other reachable cell
        for (int y = 0; y < d.length; y++) { // each row
            for (int x = 0; x < d[y].length; x++) { // each column
                if (d[y][x] < 1) continue; // unreachable or the centre itself
                if (d[y][x] >= min && d[y][x] <= max) good.add(new Point(x, y)); // wanted range
                else any.add(new Point(x, y)); // fallback
            } // end of column loop
        } // end of row loop
        List<Point> pick = good.isEmpty() ? any : good; // prefer the wanted range
        return pick.isEmpty() ? null : pick.get(random.nextInt(pick.size())); // random choice
    } // end of randomCell

    /**
     * A noise at (x, y) is heard by mimics within {@code range} PATH steps (walls matter), so far-away mimics hear nothing.
     * They investigate the noise position. Returns how many mimics heard it.
     */
    public int hearNoise(int x, int y, String[] map, int range) { // alert nearby mimics
        int heard = 0; // mimics alerted
        if (map == null) return 0; // nothing to do
        int[][] d = Pathfinder.distances(map, new Point(x, y), false); // distances from the noise
        for (Mimic m : mimics) { // each mimic
            int dist = d[m.pos.y][m.pos.x]; // how far this mimic is
            if (!m.disguised && m.state != WAKING && clockNow >= m.stunUntil && dist >= 0 && dist <= range && m.state != CHASE) { // heard it and not already chasing
                m.state = INVESTIGATE; // go and look
                m.last.setLocation(x, y); // at the noise position
                heard++; // count it
            } // end of hearing check
        } // end of mimic loop
        return heard; // report
    } // end of hearNoise

    public boolean checkCollision(int playerX, int playerY) { // is a mimic on the player's cell
        for (Mimic m : mimics) { // for each mimic
            if (!m.disguised && clockNow >= m.stunUntil && m.pos.x == playerX && m.pos.y == playerY) { // same cell, and not a stunned or sleeping one
                return true; // caught
            } // end of match check
        } // end of mimic loop
        return false; // safe
    } // end of checkCollision

    /** True if any mimic is within {@code range} cells (Manhattan distance) of the position. */
    public boolean anyWithin(int x, int y, int range) { // used to decide if a footstep is audible
        for (Mimic m : mimics) { // for each mimic
            if (Math.abs(m.pos.x - x) + Math.abs(m.pos.y - y) <= range) { // close enough
                return true; // audible
            } // end of distance check
        } // end of mimic loop
        return false; // too far away
    } // end of anyWithin

    public void resetToSpawn() { // send every mimic back to its spawn cell
        for (Mimic m : mimics) { // for each mimic
            if (m.disguised) continue; // a hidden fake chest stays where it is
            m.pos.setLocation(m.home); // back to the spawn cell
            m.state = PATROL; // calm again
            m.goal = null; // new goal later
            m.nextMove = 0; // reschedule (gives the player a head start)
        } // end of loop
    } // end of resetToSpawn

    public List<Point> getPositions() { // mimic cells for drawing
        List<Point> list = new ArrayList<>(); // collect copies
        for (Mimic m : mimics) { // for each mimic
            if (!m.disguised) list.add(new Point(m.pos)); // disguised ones are drawn as chests instead
        } // end of loop
        return Collections.unmodifiableList(list); // read-only view
    } // end of getPositions

    public List<Point> getDisguisedPositions() { // cells that must be drawn as ordinary closed chests
        List<Point> list = new ArrayList<>(); // collect copies
        for (Mimic m : mimics) { // for each mimic
            if (m.disguised) list.add(new Point(m.pos)); // copy of its cell
        } // end of loop
        return list; // the fake chests
    } // end of getDisguisedPositions

    public boolean isDisguisedAt(int x, int y) { // is a fake chest on this cell
        for (Mimic m : mimics) { // each mimic
            if (m.disguised && m.pos.x == x && m.pos.y == y) return true; // found
        } // end of loop
        return false; // none here
    } // end of isDisguisedAt

    /**
     * The player tried to open the chest at (x, y). If it is a fake chest the mimic is revealed: it stands still for
     * WAKE_MILLIS (the reaction time) and then hunts. Returns true if a mimic was revealed.
     */
    public boolean revealAt(int x, int y, long now) { // reveal a fake chest
        for (Mimic m : mimics) { // each mimic
            if (m.disguised && m.pos.x == x && m.pos.y == y) { // this is the one
                m.disguised = false; // the disguise drops
                m.state = WAKING; // standing still for a moment
                m.wakeEnd = now + WAKE_MILLIS; // then the chase starts
                return true; // revealed
            } // end of match
        } // end of loop
        return false; // that cell is not a fake chest
    } // end of revealAt

    /** Stuns every visible mimic within {@code radius} cells (Manhattan) of (x, y) until the given game time. Returns how many. */
    public int stunNear(int x, int y, int radius, long until) { // freeze nearby mimics
        int count = 0; // stunned mimics
        for (Mimic m : mimics) { // each mimic
            if (m.disguised) continue; // sleeping fake chests are not affected
            if (Math.abs(m.pos.x - x) + Math.abs(m.pos.y - y) <= radius) { // inside the blast
                m.stunUntil = until; // frozen until then
                count++; // count it
            } // end of range check
        } // end of loop
        return count; // report
    } // end of stunNear

    /**
     * A placed blue bomb at (x, y) goes off when a visible mimic comes within 1 cell: every visible mimic within
     * {@code radius} cells is stunned for {@code stunMillis}. Returns true if it went off (the game then removes the bomb).
     */
    public boolean triggerTrap(int x, int y, long now, int radius, long stunMillis) { // check one trap
        clockNow = now; // use the caller's game time
        boolean near = false; // a mimic is next to the bomb
        for (Mimic m : mimics) { // each mimic
            if (!m.disguised && Math.abs(m.pos.x - x) + Math.abs(m.pos.y - y) <= 1) near = true; // close enough to set it off
        } // end of loop
        if (!near) return false; // nothing happens
        stunNear(x, y, radius, now + stunMillis); // stun everyone in the blast
        return true; // it exploded
    } // end of triggerTrap

    public List<Point> getStunnedPositions() { // stunned mimics (the game tints them)
        List<Point> list = new ArrayList<>(); // collect copies
        for (Mimic m : mimics) { // each mimic
            if (!m.disguised && clockNow < m.stunUntil) list.add(new Point(m.pos)); // still stunned
        } // end of loop
        return list; // result
    } // end of getStunnedPositions

    public boolean isActive() { // are there any mimics
        return !mimics.isEmpty(); // true if at least one
    } // end of isActive
} // end of MimicLogic
