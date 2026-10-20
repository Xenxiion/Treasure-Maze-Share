package logic; // package for game rules

import java.awt.Point; // integer x/y coordinate

public class TreasureLogic { // treasure state of a level
    private Point position; // treasure cell, null if none
    private boolean claimed; // true once picked up

    public void loadTreasureFromMap(String[] map) { // read the treasure from a level map
        position = null; // forget old treasure
        claimed = false; // not claimed yet
        if (map == null) return; // nothing to read

        for (int r = 0; r < map.length; r++) { // for each row
            int c = map[r].indexOf('T'); // column of the treasure in this row
            if (c >= 0) { // found
                position = new Point(c, r); // remember its cell
                return; // only one treasure per level
            } // end of found check
        } // end of row loop
    } // end of loadTreasureFromMap

    public boolean claimIfSteppedOn(int playerX, int playerY) { // pick up when the player stands on it
        if (!claimed && position != null && position.x == playerX && position.y == playerY) { // unclaimed and same cell
            claimed = true; // mark as picked up
            return true; // newly claimed
        } // end of check
        return false; // nothing claimed
    } // end of claimIfSteppedOn

    public boolean isClaimed() { // is the treasure taken
        return claimed; // return the flag
    } // end of isClaimed

    public Point getPosition() { // treasure cell for drawing
        return position; // may be null
    } // end of getPosition
} // end of TreasureLogic
