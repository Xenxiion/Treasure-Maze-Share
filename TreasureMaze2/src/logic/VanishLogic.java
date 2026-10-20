package logic; // package for game rules

import java.awt.Point; // integer x/y coordinate
import java.util.HashMap; // cell -> time lookup table
import java.util.Iterator; // lets us remove entries while looping
import java.util.Map; // map interface

/**
 * Level 1 special rule: floor tiles vanish after the player steps OFF them.
 * 1. The player leaves a tile -> it starts to crumble (shown cracked).
 * 2. After CRUMBLE_MILLIS it vanishes and cannot be walked on.
 * 3. After REGROW_MILLIS it comes back, so the player can never be stuck for good.
 * Stepping back onto a tile that is still only crumbling saves it. The exit and the chest never vanish.
 */
// Level 1 floor tiles: when the player leaves a tile it starts to crumble, then vanishes, then grows back.
// Each tile stores the time of its next change. update(now) compares those times with the clock.
public class VanishLogic { // vanishing tile state

    public static final long CRUMBLE_MILLIS = 2200; // time between leaving a tile and it vanishing
    public static final long REGROW_MILLIS = 5000; // time a vanished tile stays gone

    private final Map<Point, Long> crumblingUntil = new HashMap<>(); // cell -> time when it vanishes
    private final Map<Point, Long> vanishedUntil = new HashMap<>(); // cell -> time when it grows back
    private final Map<Point, Boolean> protectedCells = new HashMap<>(); // cells that never vanish
    private boolean enabled; // false on levels 2 and 3

    /** Prepares the rule for a level. Pass enabled=false for levels without vanishing tiles. */
    public void load(boolean enabled, Point... protect) { // reset for a new level
        this.enabled = enabled; // remember the setting
        crumblingUntil.clear(); // no crumbling tiles
        vanishedUntil.clear(); // no vanished tiles
        protectedCells.clear(); // forget old protected cells
        for (Point p : protect) { // each protected cell
            if (p != null) { // skip missing ones
                protectedCells.put(new Point(p), true); // never vanishes
            } // end of null check
        } // end of protect loop
    } // end of load

    public boolean isEnabled() { // is the rule active on this level
        return enabled; // flag
    } // end of isEnabled

    /** Called when the player steps off a tile: the tile starts to crumble. */
    public void onLeave(int x, int y, long now) { // player left a cell
        if (!enabled) return; // rule not active
        Point cell = new Point(x, y); // the cell that was left
        if (protectedCells.containsKey(cell)) return; // exit and chest never vanish
        crumblingUntil.put(cell, now + CRUMBLE_MILLIS); // start the timer
    } // end of onLeave

    /** Called when the player steps onto a tile: a crumbling tile is saved while someone stands on it. */
    public void onEnter(int x, int y) { // player entered a cell
        crumblingUntil.remove(new Point(x, y)); // cancel its crumble timer
    } // end of onEnter

    public boolean isVanished(int x, int y) { // can this cell not be walked on
        return enabled && vanishedUntil.containsKey(new Point(x, y)); // gone while its regrow timer runs
    } // end of isVanished

    public boolean isCrumbling(int x, int y) { // is this cell about to vanish
        return enabled && crumblingUntil.containsKey(new Point(x, y)); // crumble timer running
    } // end of isCrumbling

    /** Advances the timers. Returns true if any tile changed (so the screen should be redrawn). */
    public boolean update(long now) { // called often by the game timer
        if (!enabled) return false; // nothing to do
        boolean changed = false; // did anything change
        Iterator<Map.Entry<Point, Long>> crumbleIt = crumblingUntil.entrySet().iterator(); // walk crumbling tiles
        while (crumbleIt.hasNext()) { // each crumbling tile
            Map.Entry<Point, Long> entry = crumbleIt.next(); // current entry
            if (now >= entry.getValue()) { // its time is up
                vanishedUntil.put(entry.getKey(), now + REGROW_MILLIS); // it vanishes and will regrow later
                crumbleIt.remove(); // no longer crumbling
                changed = true; // redraw needed
            } // end of due check
        } // end of crumbling loop
        Iterator<Map.Entry<Point, Long>> vanishIt = vanishedUntil.entrySet().iterator(); // walk vanished tiles
        while (vanishIt.hasNext()) { // each vanished tile
            Map.Entry<Point, Long> entry = vanishIt.next(); // current entry
            if (now >= entry.getValue()) { // regrow time reached
                vanishIt.remove(); // the tile is back
                changed = true; // redraw needed
            } // end of due check
        } // end of vanished loop
        return changed; // report
    } // end of update
} // end of VanishLogic
