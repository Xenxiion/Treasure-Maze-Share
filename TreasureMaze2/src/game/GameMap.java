package game; // package for core game objects

import java.awt.Point; // integer x/y coordinate
import java.util.ArrayDeque; // stack used by the maze carver
import java.util.ArrayList; // growable list
import java.util.Arrays; // array fill helper
import java.util.Collections; // shuffle helper
import java.util.List; // list interface
import java.util.Random; // random numbers
import logic.MimicLogic; // patrol route builder
import logic.Pathfinder; // BFS helpers

/**
 * Generates a NEW random map for each level every time a game starts, then VALIDATES it:
 * the start, treasure, exit and every coin must be reachable without stepping on a bomb,
 * and mimic patrol areas must keep away from the treasure, start and exit.
 * One small generator replaces hand-written maps, so there is no long list of fixed levels.
 *
 * Symbols: # wall, . floor, P start, T treasure chest, E exit, B bomb, M mimic home, C coin.
 */
// HOW MAPS ARE MADE: a map is an array of text rows. '#' is a wall, '.' is floor and letters are objects:
// P start, T real chest, E exit, C coin, M mimic, B black bomb, U blue bomb pickup.
// 1. carveMaze() digs a random maze (depth-first search) so no two games look the same.
// 2. openShortcuts() removes a few walls so there are loops, not only one path.
// 3. Objects are placed on free floor cells, then the map is CHECKED (a safe route to the chest, exit and coins must exist).
// If a check fails, the whole map is thrown away and a new one is built.
public final class GameMap { // static level factory

    public static final int LEVEL_COUNT = 3; // number of levels

    // width, height, shortcut openings (more = easier), bombs, mimics, coins
    private static final int[][] SPEC = { // one row of settings per level
        {15, 15, 12, 25, 1, 3}, // level 1: small: 25 bomb pickups (20 black + 5 blue), 1 regular mimic, 3 coins
        {31, 25, 14, 25, 1, 4}, // level 2: 25 bomb pickups, 1 regular mimic, 4 coins
        {35, 31, 18, 25, 2, 5} // level 3: 25 bomb pickups, 2 regular mimics, 5 coins
    }; // end of SPEC (every level also gets 1 disguised mimic chest, added by MimicLogic when the level loads)
    private static final int BLUE_BOMBS = 5; // how many of the 25 pickups are blue Mimic-trap bombs ('U' on the map)

    private static final int MAX_ATTEMPTS = 300; // tries before giving up on one map
    private static final int MIN_MIMIC_HOME_TO_TREASURE = 10; // mimic home must be this many steps from the chest
    private static final int MIN_PATROL_TO_TREASURE = 6; // every patrol cell must be this many steps from the chest
    private static final int MIN_PATROL_TO_START = 8; // ...and from the start, so the player is never ambushed at once
    private static final int MIN_PATROL_TO_EXIT = 3; // ...and from the exit

    private static final Random RND = new Random(); // not seeded: a different map each run
    private static final String[] LAST = new String[LEVEL_COUNT]; // previous layout per level (to avoid repeats)
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // right, left, down, up

    private GameMap() { // private: never instantiated
    } // end of constructor

    /** Generates a valid map for a level (1-3) that is different from the previous one of that level. */
    public static synchronized String[] generate(int level) { // public entry point
        int idx = Math.max(1, Math.min(LEVEL_COUNT, level)) - 1; // clamp to 1..3, make 0-based
        String[] result = null; // the map to return
        for (int round = 0; round < 20; round++) { // repeat if we happen to produce the same layout again
            result = buildValid(idx); // a validated map
            String key = String.join("\n", result); // layout as one string for comparison
            if (!key.equals(LAST[idx])) { // different from last time
                LAST[idx] = key; // remember it
                return result; // done
            } // end of repeat check
        } // end of rounds
        return result; // practically unreachable: 20 identical random maps in a row
    } // end of generate

    /** Position of the first cell holding {@code symbol}, or null. */
    public static Point find(String[] map, char symbol) { // locate a symbol on the map
        for (int r = 0; r < map.length; r++) { // for each row
            int c = map[r].indexOf(symbol); // column of the symbol in this row
            if (c >= 0) { // found
                return new Point(c, r); // return its position
            } // end of found check
        } // end of row loop
        return null; // symbol not on the map
    } // end of find

    private static String[] buildValid(int idx) { // keep trying until a map passes validation
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) { // limited number of tries
            String[] map = tryBuild(SPEC[idx], idx + 1); // build and validate
            if (map != null) { // passed
                return map; // use it
            } // end of pass check
        } // end of attempts
        throw new IllegalStateException("Could not generate a valid map for level " + (idx + 1)); // should never happen
    } // end of buildValid

    /** Builds one candidate map. Returns null if it fails validation (the caller then tries again). */
    private static String[] tryBuild(int[] spec, int level) { // generate and validate
        int w = spec[0]; // map width
        int h = spec[1]; // map height
        char[][] g = new char[h][w]; // editable grid
        for (char[] row : g) { // for each row
            Arrays.fill(row, '#'); // start with solid wall
        } // end of fill loop

        Point[] corners = {new Point(1, 1), new Point(w - 2, 1), new Point(1, h - 2), new Point(w - 2, h - 2)}; // possible starts
        Point start = corners[RND.nextInt(corners.length)]; // random corner start
        carveMaze(g, w, h, start); // dig a perfect maze (random walls)
        openShortcuts(g, w, h, spec[2]); // knock out some walls to create loops

        String[] base = toMap(g); // snapshot for BFS
        int[][] fromStart = Pathfinder.distances(base, start, false); // distance from start to each cell
        int maxD = maxOf(fromStart); // farthest cell from the start

        // Treasure: random cell in a distance band. Level 1 is closer to the start (calmer).
        double lo = level == 1 ? 0.35 : 0.45; // band start (fraction of the longest distance)
        double hi = level == 1 ? 0.60 : 0.75; // band end
        Point treasure = pickInBand(fromStart, (int) (maxD * lo), (int) (maxD * hi), Collections.singletonList(start)); // chest cell
        if (treasure == null) return null; // no cell in the band
        int[][] fromTreasure = Pathfinder.distances(base, treasure, false); // distance from the chest to each cell
        int maxT = maxOf(fromTreasure); // farthest cell from the chest
        List<Point> taken = new ArrayList<>(); // cells already used by objects
        taken.add(start); // start is used
        taken.add(treasure); // chest is used
        Point exit = pickInBand(fromTreasure, (int) (maxT * 0.7), maxT, taken); // exit: far from the chest
        if (exit == null) return null; // none found
        taken.add(exit); // exit is used
        int[][] fromExit = Pathfinder.distances(base, exit, false); // distance from the exit to each cell

        // Coins (the alternative way to unlock the exit): dead ends and far corners, spread apart.
        List<Point> coins = pickCoins(g, fromStart, spec[5], taken); // coin cells
        if (coins.size() < spec[5]) return null; // not enough good spots
        taken.addAll(coins); // coins are used

        // Mimics: patrol areas must be valid and away from chest, start and exit.
        List<Point> homes = new ArrayList<>(); // chosen mimic homes
        if (spec[4] > 0) { // this level has mimics
            int radius = MimicLogic.patrolRadiusFor(level); // patrol area size
            List<Point> candidates = new ArrayList<>(); // possible homes
            for (int y = 1; y < h - 1; y++) { // inner rows
                for (int x = 1; x < w - 1; x++) { // inner columns
                    if (g[y][x] == '.' && fromStart[y][x] >= maxD * 0.4 // far from the start
                        && fromTreasure[y][x] >= MIN_MIMIC_HOME_TO_TREASURE // far from the chest
                        && fromExit[y][x] >= 6) { // far from the exit
                        candidates.add(new Point(x, y)); // keep it
                    } // end of filter
                } // end of column loop
            } // end of row loop
            Collections.shuffle(candidates, RND); // random order
            for (Point cand : candidates) { // test candidates
                if (homes.size() >= spec[4]) break; // enough mimics
                if (!farFromOtherHomes(cand, homes)) continue; // keep mimics apart
                List<Point> route = MimicLogic.buildPatrolRoute(base, cand, radius); // its patrol path
                if (route == null || route.size() < 5) continue; // too short to be a real patrol
                if (!patrolIsValid(route, taken, fromStart, fromTreasure, fromExit)) continue; // too close to something important
                homes.add(cand); // accept this mimic
            } // end of candidate loop
            if (homes.size() < spec[4]) return null; // could not place all mimics
        } // end of mimic placement

        // Write all markers into the grid first, so bomb validation sees the final layout.
        g[start.y][start.x] = 'P'; // mark start
        g[treasure.y][treasure.x] = 'T'; // mark chest
        g[exit.y][exit.x] = 'E'; // mark exit
        for (Point c : coins) { // each coin
            g[c.y][c.x] = 'C'; // mark coin
        } // end of coin loop
        for (Point m : homes) { // each mimic home
            g[m.y][m.x] = 'M'; // mark mimic
        } // end of mimic loop

        // Bombs: only on empty floor, never closing the safe routes.
        int minBombDist = level == 1 ? 5 : 3; // level 1 keeps bombs away from the start
        List<Point> bombCandidates = new ArrayList<>(); // possible bomb cells
        for (int y = 1; y < h - 1; y++) { // inner rows
            for (int x = 1; x < w - 1; x++) { // inner columns
                if (g[y][x] == '.' && fromStart[y][x] >= minBombDist // empty floor, not next to the start
                    && Math.abs(x - treasure.x) + Math.abs(y - treasure.y) > 1 // never touching the real chest
                    && Math.abs(x - exit.x) + Math.abs(y - exit.y) > 1) { // never touching the exit
                    bombCandidates.add(new Point(x, y)); // keep it
                } // end of filter
            } // end of column loop
        } // end of row loop
        Collections.shuffle(bombCandidates, RND); // random order
        int placed = 0; // bombs placed so far
        for (Point p : bombCandidates) { // try each candidate
            if (placed >= spec[3]) break; // enough bombs
            g[p.y][p.x] = 'B'; // tentatively place a bomb
            if (safeRoutesExist(g, start, treasure, exit, coins)) { // everything still reachable without bombs
                placed++; // keep the bomb
            } else { // a route would be blocked
                g[p.y][p.x] = '.'; // undo it
            } // end of check
        } // end of candidate loop
        if (placed < spec[3]) return null; // could not place all bombs
        if (!makeBlueBombs(g, start)) return null; // turn 5 of the pickups into blue ones

        return toMap(g); // validated map
    } // end of tryBuild

    /**
     * Converts BLUE_BOMBS of the placed black bombs ('B') into blue pickups ('U'). Only bombs next to a cell the player can
     * reach without touching a black bomb are used (so every blue bomb is reachable), and they are picked far apart (spread out).
     */
    private static boolean makeBlueBombs(char[][] g, Point start) { // returns false if not enough reachable bombs
        int h = g.length; // rows
        int w = g[0].length; // columns
        boolean[][] seen = new boolean[h][w]; // cells reachable from the start without touching a black bomb
        java.util.ArrayDeque<Point> queue = new java.util.ArrayDeque<>(); // BFS queue
        queue.add(start); // begin at the start
        seen[start.y][start.x] = true; // visited
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // four directions
        while (!queue.isEmpty()) { // flood fill
            Point c = queue.poll(); // next cell
            for (int[] d : dirs) { // each neighbour
                int nx = c.x + d[0]; // neighbour column
                int ny = c.y + d[1]; // neighbour row
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || seen[ny][nx] || g[ny][nx] == '#' || g[ny][nx] == 'B') continue; // wall, bomb or visited
                seen[ny][nx] = true; // reachable
                queue.add(new Point(nx, ny)); // continue from there
            } // end of neighbour loop
        } // end of flood fill
        List<Point> candidates = new ArrayList<>(); // black bombs that can be reached
        for (int y = 1; y < h - 1; y++) { // each row
            for (int x = 1; x < w - 1; x++) { // each column
                if (g[y][x] != 'B') continue; // only black bombs
                boolean next = seen[y][x - 1] || seen[y][x + 1] || seen[y - 1][x] || seen[y + 1][x]; // a reachable neighbour exists
                if (next) candidates.add(new Point(x, y)); // usable
            } // end of column loop
        } // end of row loop
        if (candidates.size() < BLUE_BOMBS) return false; // not enough
        List<Point> chosen = new ArrayList<>(); // picked blue bombs
        chosen.add(candidates.remove(RND.nextInt(candidates.size()))); // first one at random
        while (chosen.size() < BLUE_BOMBS) { // pick the rest as far from the others as possible
            Point best = null; // best candidate so far
            int bestDist = -1; // its distance to the nearest chosen bomb
            for (Point c : candidates) { // each candidate
                int nearest = Integer.MAX_VALUE; // distance to the closest chosen one
                for (Point o : chosen) nearest = Math.min(nearest, Math.abs(c.x - o.x) + Math.abs(c.y - o.y)); // Manhattan distance
                if (nearest > bestDist) { bestDist = nearest; best = c; } // keep the most isolated
            } // end of candidate loop
            candidates.remove(best); // used
            chosen.add(best); // chosen
        } // end of spread
        for (Point p : chosen) g[p.y][p.x] = 'U'; // blue pickup marker
        return true; // done
    } // end of makeBlueBombs

    /** True when the start can reach the chest, the exit and every coin without stepping on a bomb. */
    private static boolean safeRoutesExist(char[][] g, Point start, Point treasure, Point exit, List<Point> coins) { // validation
        int[][] d = Pathfinder.distances(toMap(g), start, true); // BFS that treats bombs as walls
        if (d[treasure.y][treasure.x] < 0 || d[exit.y][exit.x] < 0) return false; // chest or exit cut off
        for (Point c : coins) { // each coin
            if (d[c.y][c.x] < 0) return false; // a coin cut off
        } // end of coin loop
        return true; // all reachable (paths are two-way, so chest -> exit works too)
    } // end of safeRoutesExist

    private static boolean patrolIsValid(List<Point> route, List<Point> taken, // every patrol cell must be safe
                                         int[][] fromStart, int[][] fromTreasure, int[][] fromExit) { // distance tables
        for (Point p : route) { // each patrol cell
            if (taken.contains(p)) return false; // on top of start, chest, exit or a coin
            if (fromTreasure[p.y][p.x] < MIN_PATROL_TO_TREASURE) return false; // too close to the chest
            if (fromStart[p.y][p.x] < MIN_PATROL_TO_START) return false; // too close to the start
            if (fromExit[p.y][p.x] < MIN_PATROL_TO_EXIT) return false; // too close to the exit
        } // end of loop
        return true; // route is fine
    } // end of patrolIsValid

    private static boolean farFromOtherHomes(Point cand, List<Point> homes) { // keep mimics apart
        for (Point other : homes) { // each existing mimic
            if (Math.abs(cand.x - other.x) + Math.abs(cand.y - other.y) < 10) return false; // too close
        } // end of loop
        return true; // far enough
    } // end of farFromOtherHomes

    private static List<Point> pickCoins(char[][] g, int[][] fromStart, int count, List<Point> taken) { // choose coin cells
        List<Point> deadEnds = new ArrayList<>(); // corridor ends (a corridor with one exit)
        List<Point> others = new ArrayList<>(); // ordinary floor cells
        for (int y = 1; y < g.length - 1; y++) { // inner rows
            for (int x = 1; x < g[y].length - 1; x++) { // inner columns
                Point p = new Point(x, y); // this cell
                if (g[y][x] != '.' || fromStart[y][x] < 6 || taken.contains(p)) continue; // must be free floor, not near the start
                int open = 0; // number of walkable neighbours
                for (int[] d : DIRS) { // each direction
                    if (g[y + d[1]][x + d[0]] != '#') open++; // neighbour is walkable
                } // end of direction loop
                if (open == 1) { // dead end
                    deadEnds.add(p); // best coin spot
                } else { // ordinary cell
                    others.add(p); // fallback spot
                } // end of type check
            } // end of column loop
        } // end of row loop
        Collections.shuffle(deadEnds, RND); // random order
        Collections.shuffle(others, RND); // random order
        List<Point> pool = new ArrayList<>(deadEnds); // dead ends first
        pool.addAll(others); // then other cells
        List<Point> chosen = new ArrayList<>(); // result
        for (Point p : pool) { // each candidate
            if (chosen.size() >= count) break; // enough coins
            boolean farEnough = true; // spacing check
            for (Point c : chosen) { // each coin already chosen
                if (Math.abs(p.x - c.x) + Math.abs(p.y - c.y) < 6) { // too close to another coin
                    farEnough = false; // reject
                    break; // stop checking
                } // end of distance check
            } // end of chosen loop
            if (farEnough) { // accepted
                chosen.add(p); // use it
            } // end of accept
        } // end of pool loop
        return chosen; // coin cells
    } // end of pickCoins

    // Randomized depth-first search: stand on a cell, jump two cells in a random direction to a cell not yet dug,
    // knock down the wall between, and continue from there. When stuck, step back (the "stack") and try another direction.
    private static void carveMaze(char[][] g, int w, int h, Point start) { // randomized depth-first maze carving
        ArrayDeque<int[]> stack = new ArrayDeque<>(); // cells on the current path
        g[start.y][start.x] = '.'; // open the start cell
        stack.push(new int[] {start.x, start.y}); // begin at the start
        while (!stack.isEmpty()) { // until every reachable cell is visited
            int[] cur = stack.peek(); // current cell
            List<int[]> dirs = new ArrayList<>(List.of(DIRS)); // directions to try
            Collections.shuffle(dirs, RND); // random direction order
            boolean moved = false; // did we dig somewhere
            for (int[] d : dirs) { // try each direction
                int nx = cur[0] + d[0] * 2; // target column, two cells away
                int ny = cur[1] + d[1] * 2; // target row, two cells away
                if (nx >= 1 && nx <= w - 2 && ny >= 1 && ny <= h - 2 && g[ny][nx] == '#') { // inside the border and still wall
                    g[cur[1] + d[1]][cur[0] + d[0]] = '.'; // open the wall between
                    g[ny][nx] = '.'; // open the target cell
                    stack.push(new int[] {nx, ny}); // continue from the new cell
                    moved = true; // we dug
                    break; // leave the direction loop
                } // end of dig check
            } // end of direction loop
            if (!moved) { // dead end
                stack.pop(); // backtrack
            } // end of dead-end check
        } // end of carving loop
    } // end of carveMaze

    private static void openShortcuts(char[][] g, int w, int h, int count) { // remove some walls to create loops
        int opened = 0; // walls removed so far
        int attempts = 0; // tries so far
        while (opened < count && attempts < count * 200) { // until enough or too many tries
            attempts++; // count the try
            int x = 1 + RND.nextInt(w - 2); // random inner column
            int y = 1 + RND.nextInt(h - 2); // random inner row
            boolean wallBetweenCells = (x % 2 == 1 && y % 2 == 0) || (x % 2 == 0 && y % 2 == 1); // wall that separates two floor cells
            if (g[y][x] == '#' && wallBetweenCells) { // removable wall
                g[y][x] = '.'; // open it
                opened++; // count it
            } // end of wall check
        } // end of loop
    } // end of openShortcuts

    private static Point pickInBand(int[][] dist, int lo, int hi, List<Point> excluded) { // random cell with lo <= distance <= hi
        List<Point> options = new ArrayList<>(); // matching cells
        for (int y = 0; y < dist.length; y++) { // for each row
            for (int x = 0; x < dist[y].length; x++) { // for each column
                int d = dist[y][x]; // distance of this cell
                if (d >= lo && d <= hi && d > 0 && !excluded.contains(new Point(x, y))) { // inside the band, reachable, not excluded
                    options.add(new Point(x, y)); // candidate
                } // end of band check
            } // end of column loop
        } // end of row loop
        if (options.isEmpty()) return null; // nothing matched
        return options.get(RND.nextInt(options.size())); // pick one at random
    } // end of pickInBand

    private static int maxOf(int[][] dist) { // largest value in a distance table
        int max = 0; // best so far
        for (int[] row : dist) { // for each row
            for (int d : row) { // for each cell
                max = Math.max(max, d); // track the maximum
            } // end of cell loop
        } // end of row loop
        return max; // result
    } // end of maxOf

    private static String[] toMap(char[][] g) { // convert the char grid to strings
        String[] out = new String[g.length]; // one string per row
        for (int i = 0; i < g.length; i++) { // for each row
            out[i] = new String(g[i]); // build the row string
        } // end of row loop
        return out; // string map
    } // end of toMap
} // end of GameMap
