package logic; // package for game rules

import game.GameMap; // level map source

public class LevelManager { // tracks which level is active and holds this game's maps
    private int currentLevel; // active level number, 1-based
    private final String[][] maps = new String[GameMap.LEVEL_COUNT][]; // the three maps of the current game

    public LevelManager() { // start a game
        resetToLevel1(); // level 1 with freshly generated maps
    } // end of constructor

    /** Starts over: back to level 1 AND a brand-new random map for every level. */
    public final void resetToLevel1() { // new game (final: called from the constructor)
        this.currentLevel = 1; // back to level 1
        for (int i = 0; i < GameMap.LEVEL_COUNT; i++) { // each level
            maps[i] = GameMap.generate(i + 1); // generate a new random map (differs from the previous game)
        } // end of loop
    } // end of resetToLevel1

    public int getCurrentLevel() { // read the active level
        return currentLevel; // return it
    } // end of getCurrentLevel

    public boolean nextLevel() { // advance one level
        if (currentLevel < GameMap.LEVEL_COUNT) { // not on the last level
            currentLevel++; // go to the next level
            return true; // advanced
        } // end of limit check
        return false; // already on the last level
    } // end of nextLevel

    public String[] getCurrentMap() { // map of the active level
        return maps[currentLevel - 1]; // same map until the game is reset (so reloading a level keeps the same maze)
    } // end of getCurrentMap
} // end of LevelManager
