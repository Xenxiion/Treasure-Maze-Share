package logic; // package for game rules

import java.awt.Point; // integer x/y coordinate
import java.util.ArrayDeque; // queue used by the search
import java.util.Arrays; // array fill helper

/** Breadth-first search helpers shared by map generation, move budgets and mimic AI. */
public final class Pathfinder { // static helper, no instances

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // right, left, down, up

    private Pathfinder() { // private: never instantiated
    } // end of constructor

    public static boolean walkable(String[] map, int x, int y, boolean avoidBombs) { // can a cell be entered
        if (y < 0 || y >= map.length || x < 0 || x >= map[y].length()) { // outside the grid
            return false; // not walkable
        } // end of bounds check
        char c = map[y].charAt(x); // symbol in the cell
        if (c == '#') { // wall
            return false; // not walkable
        } // end of wall check
        return !(avoidBombs && c == 'B'); // bombs block only when avoidBombs is set
    } // end of walkable

    /** Step distance from {@code from} to every cell, -1 where unreachable. Indexed [y][x]. */
    // Breadth-first search (BFS): start at one cell and spread outwards one step at a time, like water filling a maze.
    // The number written in each cell is how many steps it took to get there. -1 means "cannot be reached".
    // A queue (first in, first out) keeps the spreading in order, so the first time we reach a cell is the shortest way.
    public static int[][] distances(String[] map, Point from, boolean avoidBombs) { // BFS distance table
        int[][] dist = new int[map.length][]; // one row per map row
        for (int r = 0; r < map.length; r++) { // for each row
            dist[r] = new int[map[r].length()]; // allocate the row
            Arrays.fill(dist[r], -1); // -1 means not reached
        } // end of row loop
        if (!walkable(map, from.x, from.y, false)) { // start inside a wall
            return dist; // nothing reachable
        } // end of start check
        ArrayDeque<int[]> queue = new ArrayDeque<>(); // cells waiting to be expanded
        dist[from.y][from.x] = 0; // start is 0 steps from itself
        queue.add(new int[] {from.x, from.y}); // enqueue the start
        while (!queue.isEmpty()) { // until the search is exhausted
            int[] cur = queue.poll(); // take the next cell
            for (int[] d : DIRS) { // try 4 directions
                int nx = cur[0] + d[0]; // neighbor column
                int ny = cur[1] + d[1]; // neighbor row
                if (walkable(map, nx, ny, avoidBombs) && dist[ny][nx] < 0) { // enterable and not yet reached
                    dist[ny][nx] = dist[cur[1]][cur[0]] + 1; // one step farther than current
                    queue.add(new int[] {nx, ny}); // expand it later
                } // end of neighbor check
            } // end of direction loop
        } // end of search loop
        return dist; // finished table
    } // end of distances

    public static int distance(String[] map, Point from, Point to, boolean avoidBombs) { // distance between two cells
        return distances(map, from, avoidBombs)[to.y][to.x]; // look up target in the table
    } // end of distance

    /** Next cell on a shortest path from {@code from} toward {@code to}; null if none. */
    // Finds the next single step toward a target. We run BFS from the TARGET, then stand on our cell and pick the
    // neighbour whose number is exactly one smaller - that neighbour is one step closer. Repeating this walks the shortest path.
    public static Point nextStep(String[] map, Point from, Point to) { // one step of a chase
        int[][] dist = distances(map, to, false); // distances measured from the target
        int here = dist[from.y][from.x]; // current distance to the target
        if (here <= 0) { // unreachable or already there
            return null; // no step
        } // end of check
        for (int[] d : DIRS) { // try 4 directions
            int nx = from.x + d[0]; // neighbor column
            int ny = from.y + d[1]; // neighbor row
            if (walkable(map, nx, ny, false) && dist[ny][nx] >= 0 && dist[ny][nx] < here) { // neighbor is closer to the target
                return new Point(nx, ny); // step there
            } // end of neighbor check
        } // end of direction loop
        return null; // no closer cell found
    } // end of nextStep
} // end of Pathfinder
