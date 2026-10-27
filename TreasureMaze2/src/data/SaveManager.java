package data; // package for saving and loading data

import java.io.IOException; // thrown on file read/write failure
import java.nio.charset.StandardCharsets; // fixed character set for the file
import java.nio.file.Files; // file read/write helpers
import java.nio.file.Path; // represents a file path
import java.nio.file.Paths; // builds a Path from a string
import java.util.logging.Level; // log severity levels
import java.util.logging.Logger; // logging facility
import java.util.regex.Matcher; // runs a regex on text
import java.util.regex.Pattern; // compiled regex

public final class SaveManager { // static helper for the high score file

    private static final Logger LOGGER = Logger.getLogger(SaveManager.class.getName()); // logger for warnings
    private static final String FILE_NAME = "highscore.txt"; // file kept in the launch folder
    private static final Pattern LEADING_DIGITS = Pattern.compile("^\\d+"); // matches digits at the start of the text

    private SaveManager() { // private: this class is never instantiated
    } // end of constructor

    /** Reads the saved high score. Returns 0 if the file is missing or unreadable. */
    public static int loadHighScore() { // returns the stored high score
        Path path = Paths.get(FILE_NAME); // path of the score file
        if (!Files.exists(path)) { // no file yet
            return 0; // so the high score is 0
        } // end of missing-file check
        try { // file access can fail
            String text = Files.readString(path, StandardCharsets.ISO_8859_1).trim(); // read whole file, strip whitespace
            Matcher m = LEADING_DIGITS.matcher(text); // look for leading digits
            if (m.find()) { // digits found
                return Integer.parseInt(m.group()); // convert them to an int
            } // end of digits check
        } catch (IOException | NumberFormatException e) { // unreadable file or number too large
            LOGGER.log(Level.WARNING, "Unable to load high score from file: " + FILE_NAME, e); // log the problem
        } // end of try/catch
        return 0; // fall back to 0
    } // end of loadHighScore

    /** Saves the score only if it beats the stored high score. */
    public static void saveHighScore(int score) { // stores score if it is a new record
        if (score <= loadHighScore()) { // not a new record
            return; // nothing to save
        } // end of record check
        try { // file access can fail
            Files.writeString(Paths.get(FILE_NAME), score + System.lineSeparator(), StandardCharsets.ISO_8859_1); // overwrite file with the score
        } catch (IOException e) { // write failed
            LOGGER.log(Level.WARNING, "Unable to save high score to file: " + FILE_NAME, e); // log the problem
        } // end of try/catch
    } // end of saveHighScore
} // end of SaveManager
