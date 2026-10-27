package game; // package for core game objects

public class Player { // the player position holder
    private int x; // column on the grid
    private int y; // row on the grid

    public Player(int startX, int startY) { // create a player at a start cell
        this.x = startX; // store start column
        this.y = startY; // store start row
    } // end of constructor

    public int getX() { // read the column
        return x; // return column
    } // end of getX

    public int getY() { // read the row
        return y; // return row
    } // end of getY

    public void setPosition(int x, int y) { // teleport to a cell
        this.x = x; // set column
        this.y = y; // set row
    } // end of setPosition

    public void move(int dx, int dy) { // move relative to the current cell
        this.x += dx; // shift column
        this.y += dy; // shift row
    } // end of move
} // end of Player
