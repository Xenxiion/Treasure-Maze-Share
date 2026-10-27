import javax.swing.SwingUtilities; // Swing helper that runs code on the UI thread
import ui.GameUI; // window controller from the ui package

public class Main { // program entry class

    public static void main(String[] args) { // the JVM starts here
        SwingUtilities.invokeLater(() -> { // build the UI on the Swing event thread
            GameUI ui = new GameUI(); // create the window controller
            ui.showWindow(); // build and show the window
        }); // end of the UI-thread task
    } // end of main
} // end of Main
