package logic; // package for game rules

import java.awt.Point; // integer x/y coordinate
import java.util.ArrayList; // growable list
import java.util.Collections; // read-only wrapper helper
import java.util.List; // list interface

/**
 * Alternative way to unlock the exit: collect ALL coins ('C' on the map) instead of opening the chest.
 * Coins sit in far corners and dead ends, so this costs more moves and exploring than the chest.
 */
// A list of coin cells. collectAt() removes a coin when the player stands on it and returns true,
// so a coin can be collected only once. isComplete() is true when the list is empty again.
public class CoinLogic { // coin state of a level
    private final List<Point> coins = new ArrayList<>(); // coins still on the map
    private int total; // coins the level started with

    public void loadCoinsFromMap(String[] map) { // read coins from a level map
        coins.clear(); // forget old coins
        total = 0; // reset the count
        if (map == null) return; // nothing to read

        for (int r = 0; r < map.length; r++) { // for each row
            for (int c = 0; c < map[r].length(); c++) { // for each column
                if (map[r].charAt(c) == 'C') { // coin symbol
                    coins.add(new Point(c, r)); // remember its cell
                    total++; // count it
                } // end of symbol check
            } // end of column loop
        } // end of row loop
    } // end of loadCoinsFromMap

    /** Picks up the coin at the position, if any. Returns true when a coin was collected. */
    public boolean collectAt(int playerX, int playerY) { // pick up a coin under the player
        for (int i = 0; i < coins.size(); i++) { // for each coin
            Point p = coins.get(i); // current coin
            if (p.x == playerX && p.y == playerY) { // same cell
                coins.remove(i); // take it
                return true; // collected
            } // end of match check
        } // end of coin loop
        return false; // no coin here
    } // end of collectAt

    public boolean isComplete() { // were all coins collected
        return total > 0 && coins.isEmpty(); // at least one existed and none are left
    } // end of isComplete

    public int getCollected() { // coins picked up so far
        return total - coins.size(); // started minus remaining
    } // end of getCollected

    public int getTotal() { // coins the level started with
        return total; // stored count
    } // end of getTotal

    public List<Point> getPositions() { // coins for drawing
        return Collections.unmodifiableList(coins); // read-only view
    } // end of getPositions
} // end of CoinLogic
