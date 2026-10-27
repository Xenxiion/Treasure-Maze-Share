package ui; // package for screens

import data.SaveManager; // high score storage
import java.awt.Color; // RGB color
import java.awt.Font; // text font
import java.awt.GridBagConstraints; // layout settings for one cell
import java.awt.GridBagLayout; // grid layout manager
import java.awt.Insets; // padding values
import javax.swing.JButton; // clickable button
import javax.swing.JLabel; // text label
import javax.swing.JPanel; // container

public final class ResultScreen extends JPanel { // end-of-game screen

    private static final long serialVersionUID = 1L; // version id for serialization

    private final JLabel titleLabel; // win or game over heading
    private final JLabel scoreLabel; // final score text
    private final JLabel highScoreLabel; // best score text

    public ResultScreen(GameUI ui) { // build the screen
        setBackground(UiStyle.BACKGROUND); // dark background
        setLayout(new GridBagLayout()); // center content in a grid

        GridBagConstraints gbc = new GridBagConstraints(); // layout settings
        gbc.insets = new Insets(15, 15, 15, 15); // spacing around components
        gbc.gridx = 0; // single column

        titleLabel = new JLabel("THE END - RESULTS"); // placeholder heading
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40)); // big monospaced font
        titleLabel.setForeground(Color.CYAN); // cyan text

        scoreLabel = new JLabel("FINAL SCORE: 0"); // placeholder score
        scoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24)); // medium font
        scoreLabel.setForeground(Color.WHITE); // white text

        highScoreLabel = new JLabel("BEST HIGH SCORE: 0"); // placeholder best score
        highScoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20)); // smaller font
        highScoreLabel.setForeground(Color.YELLOW); // yellow text

        JButton playAgainBtn = UiStyle.button("PLAY AGAIN", 20, 240, 50, new Color(30, 120, 60)); // replay button
        playAgainBtn.addActionListener(e -> ui.showScreen("PREVIEW")); // back to the preview

        JButton menuBtn = UiStyle.button("MAIN MENU", 18, 240, 45, new Color(50, 50, 70)); // menu button
        menuBtn.addActionListener(e -> ui.showScreen("MENU")); // back to the menu

        gbc.gridy = 0; // row 0
        add(titleLabel, gbc); // add heading
        gbc.gridy = 1; // row 1
        add(scoreLabel, gbc); // add score
        gbc.gridy = 2; // row 2
        add(highScoreLabel, gbc); // add best score
        gbc.gridy = 3; // row 3
        add(playAgainBtn, gbc); // add replay button
        gbc.gridy = 4; // row 4
        add(menuBtn, gbc); // add menu button
    } // end of constructor

    public void setResult(int score, boolean won) { // fill the screen with the outcome
        titleLabel.setText(won ? "YOU ESCAPED - ALL LEVELS CLEARED" : "GAME OVER"); // heading by outcome
        titleLabel.setForeground(won ? Color.GREEN : Color.RED); // green win, red loss
        scoreLabel.setText("FINAL SCORE: " + score); // show the score
        highScoreLabel.setText("BEST HIGH SCORE: " + SaveManager.loadHighScore()); // show the saved best
    } // end of setResult
} // end of ResultScreen
