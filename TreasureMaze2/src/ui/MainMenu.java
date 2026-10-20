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

public final class MainMenu extends JPanel { // title screen

    private static final long serialVersionUID = 1L; // version id for serialization

    private final JLabel highScoreLabel; // shows the saved high score

    public MainMenu(GameUI ui) { // build the menu
        setBackground(UiStyle.BACKGROUND); // dark background
        setLayout(new GridBagLayout()); // center content in a grid

        GridBagConstraints gbc = new GridBagConstraints(); // layout settings
        gbc.insets = new Insets(15, 15, 15, 15); // spacing around components
        gbc.gridx = 0; // single column

        JLabel titleLabel = new JLabel("THE LAST SYMBOL"); // game title
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48)); // big monospaced font
        titleLabel.setForeground(Color.CYAN); // cyan text

        highScoreLabel = new JLabel("HIGH SCORE: 0"); // placeholder text
        highScoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20)); // medium font
        highScoreLabel.setForeground(Color.YELLOW); // yellow text

        Color base = new Color(40, 40, 60); // button color
        JButton startBtn = UiStyle.button("PLAY GAME", 20, 250, 50, base); // play button
        JButton howBtn = UiStyle.button("HOW TO PLAY", 20, 250, 50, base); // rules button
        JButton exitBtn = UiStyle.button("EXIT", 20, 250, 50, base); // quit button

        startBtn.addActionListener(e -> ui.showScreen("PREVIEW")); // play opens the preview
        howBtn.addActionListener(e -> ui.showScreen("HOW")); // rules opens the how-to screen
        exitBtn.addActionListener(e -> System.exit(0)); // quit ends the program

        gbc.gridy = 0; // row 0
        add(titleLabel, gbc); // add title
        gbc.gridy = 1; // row 1
        add(highScoreLabel, gbc); // add high score
        gbc.gridy = 2; // row 2
        add(startBtn, gbc); // add play button
        gbc.gridy = 3; // row 3
        add(howBtn, gbc); // add rules button
        gbc.gridy = 4; // row 4
        add(exitBtn, gbc); // add quit button

        refreshHighScore(); // show the saved score
    } // end of constructor

    public void refreshHighScore() { // update the high score text
        highScoreLabel.setText("HIGH SCORE: " + SaveManager.loadHighScore()); // read file and display
    } // end of refreshHighScore
} // end of MainMenu
