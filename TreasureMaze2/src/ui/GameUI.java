package ui; // package for screens

import audio.SoundManager; // sound effects and music
import data.SaveManager; // high score storage
import game.TreasureMaze; // the game panel
import java.awt.CardLayout; // layout that shows one screen at a time
import java.awt.Dimension; // width/height pair
import javax.swing.JFrame; // application window
import javax.swing.JPanel; // container
import javax.swing.SwingUtilities; // run code on the UI thread

public class GameUI { // owns the window and switches screens

    private JFrame window; // the application window
    private JPanel mainPanel; // holds all screens
    private CardLayout cardLayout; // switches between screens

    private MainMenu mainMenu; // title screen
    private StartScreen startScreen; // how-to-play screen
    private GamePreview gamePreview; // level overview screen
    private TreasureMaze treasureMaze; // the game itself
    private ResultScreen resultScreen; // end-of-game screen
    private SoundManager soundManager; // sound effects and background music

    public void showWindow() { // build and show the window
        window = new JFrame("TREASURE MAZE"); // window with a title
        cardLayout = new CardLayout(); // screen switcher
        mainPanel = new JPanel(cardLayout); // container using the switcher

        mainMenu = new MainMenu(this); // create the menu
        startScreen = new StartScreen(this); // create the rules screen
        gamePreview = new GamePreview(this); // create the preview screen
        soundManager = new SoundManager(true); // load sounds and music in the background
        soundManager.startMusic(); // start the background music loop
        treasureMaze = new TreasureMaze(this, soundManager); // create the game panel
        resultScreen = new ResultScreen(this); // create the result screen

        mainPanel.add(mainMenu, "MENU"); // register the menu
        mainPanel.add(startScreen, "HOW"); // register the rules screen
        mainPanel.add(gamePreview, "PREVIEW"); // register the preview
        mainPanel.add(treasureMaze, "GAME"); // register the game
        mainPanel.add(resultScreen, "RESULT"); // register the results

        window.add(mainPanel); // put the screens in the window
        window.setMinimumSize(new Dimension(900, 700)); // smallest allowed size
        window.setExtendedState(JFrame.MAXIMIZED_BOTH); // start maximized
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // closing the window ends the program
        window.setVisible(true); // show it

        showScreen("MENU"); // start on the menu
    } // end of showWindow

    public void showScreen(String screenName) { // switch to a screen by name
        if ("MENU".equals(screenName) && mainMenu != null) { // going to the menu
            mainMenu.refreshHighScore(); // update the displayed best score
        } // end of menu check

        if ("GAME".equals(screenName) && treasureMaze != null) { // going to the game
            treasureMaze.resetGameToLevel1(); // start a fresh game
        } // end of game check

        if (cardLayout != null && mainPanel != null) { // window is built
            cardLayout.show(mainPanel, screenName); // show the requested screen
            mainPanel.revalidate(); // recompute layout
            mainPanel.repaint(); // redraw
        } // end of layout check

        if ("GAME".equals(screenName) && treasureMaze != null) { // going to the game
            SwingUtilities.invokeLater(() -> treasureMaze.requestFocusInWindow()); // give the game keyboard focus
        } // end of focus check
    } // end of showScreen

    public void showResults(int finalScore, boolean won) { // end the game and show the result
        SaveManager.saveHighScore(finalScore); // store the score if it is a record
        if (resultScreen != null) { // screen exists
            resultScreen.setResult(finalScore, won); // fill it with the outcome
        } // end of null check
        showScreen("RESULT"); // display it
    } // end of showResults
} // end of GameUI
