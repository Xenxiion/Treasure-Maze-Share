package logic; // package for game rules

import java.awt.Point; // integer x/y coordinate
import java.util.ArrayList; // growable list
import java.util.Collections; // read-only wrapper helper
import java.util.List; // list interface

// Keeps three lists of cells: black bombs (hurt the player), blue pickups (free to collect) and blue traps the player placed.
// Removing a bomb from its list when it is used is what makes it work only once.
public class BombLogic { // keeps track of active bombs
    private final List<Point> bombPositions = new ArrayList<>(); // cells holding a live (black) bomb
    private final List<Point> bluePickups = new ArrayList<>(); // blue Mimic-trap bombs lying on the map ('U'), to be collected
    private final List<Point> traps = new ArrayList<>(); // blue bombs the player has placed (waiting for a mimic)

    public void loadBombsFromMap(String[] map) { // read bombs from a level map
        bombPositions.clear(); // forget old bombs
        bluePickups.clear(); // forget old blue pickups
        traps.clear(); // forget placed traps
        if (map == null) return; // nothing to read

        for (int r = 0; r < map.length; r++) { // for each row
            for (int c = 0; c < map[r].length(); c++) { // for each column
                if (map[r].charAt(c) == 'B') { // bomb symbol
                    bombPositions.add(new Point(c, r)); // remember its cell
                } else if (map[r].charAt(c) == 'U') { // blue bomb pickup symbol
                    bluePickups.add(new Point(c, r)); // remember its cell
                } // end of symbol check
            } // end of column loop
        } // end of row loop
    } // end of loadBombsFromMap

    /** Returns true if a bomb is at the position. The bomb is consumed (it explodes once). */
    public boolean triggerAt(int playerX, int playerY) { // explode a bomb at the player's cell
        for (int i = 0; i < bombPositions.size(); i++) { // for each bomb
            Point b = bombPositions.get(i); // current bomb
            if (b.x == playerX && b.y == playerY) { // player stands on it
                bombPositions.remove(i); // remove it after exploding
                return true; // hit
            } // end of match check
        } // end of bomb loop
        return false; // no bomb here
    } // end of triggerAt

    /** Picks up the blue bomb at the position (walking over it). Returns true if there was one; it is removed so it counts once. */
    public boolean collectBlueAt(int x, int y) { // collect a blue pickup
        for (int i = 0; i < bluePickups.size(); i++) { // each pickup
            Point b = bluePickups.get(i); // current pickup
            if (b.x == x && b.y == y) { // player stands on it
                bluePickups.remove(i); // gone from the map
                return true; // collected
            } // end of match check
        } // end of loop
        return false; // nothing here
    } // end of collectBlueAt

    /** Places a trap at the position unless one is already there. Returns true if placed. */
    public boolean placeTrap(int x, int y) { // put a blue bomb down
        for (Point t : traps) { // each placed trap
            if (t.x == x && t.y == y) return false; // already a trap here
        } // end of loop
        traps.add(new Point(x, y)); // place it
        return true; // placed
    } // end of placeTrap

    public void removeTrap(Point t) { // a trap went off: it is gone, so it cannot trigger again
        traps.remove(t); // remove it from the list
    } // end of removeTrap

    public List<Point> getBluePickups() { // blue pickups for drawing
        return Collections.unmodifiableList(bluePickups); // read-only view
    } // end of getBluePickups

    public List<Point> getTraps() { // placed traps for drawing and checking
        return Collections.unmodifiableList(traps); // read-only view
    } // end of getTraps

    public List<Point> getBombPositions() { // bombs for drawing
        return Collections.unmodifiableList(bombPositions); // read-only view
    } // end of getBombPositions
} // end of BombLogic
