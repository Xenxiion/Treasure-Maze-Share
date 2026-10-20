package data; // package for saving, loading and locating files

import java.io.File; // represents a file or folder path

/** Finds the "assets" folder (sprites, sound effects, music) no matter where the game was started from. */
public final class AssetPaths { // static helper, no instances

    // Folders tried in order: normal run from the project folder, run from src/, run from the parent folder.
    private static final String[] CANDIDATES = {"assets", "../assets", "TreasureMaze2/assets"}; // possible locations

    private AssetPaths() { // private: never instantiated
    } // end of constructor

    /** Returns the assets folder (the first candidate that exists, otherwise plain "assets"). */
    public static File dir() { // locate the folder
        for (String candidate : CANDIDATES) { // try each location
            File folder = new File(candidate); // build the path
            if (folder.isDirectory()) { // it exists
                return folder; // use it
            } // end of exists check
        } // end of loop
        return new File("assets"); // nothing found: callers will simply find no files
    } // end of dir

    /** Returns a file inside the assets folder, for example file("sfx/step.wav"). */
    public static File file(String relativePath) { // build a path inside assets
        return new File(dir(), relativePath); // assets folder + relative path
    } // end of file
} // end of AssetPaths
