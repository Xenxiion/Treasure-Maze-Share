package ui; // package for screens

import java.awt.Color; // RGB color
import java.awt.Font; // text font
import java.awt.GridBagConstraints; // layout settings for one cell
import java.awt.GridBagLayout; // grid layout manager
import java.awt.Insets; // padding values
import javax.swing.JButton; // clickable button
import javax.swing.JLabel; // text label
import javax.swing.JPanel; // container
import javax.swing.JTextArea; // multi-line text box

public final class GamePreview extends JPanel { // level overview screen

    private static final long serialVersionUID = 1L; // version id for serialization

    public GamePreview(GameUI ui) { // build the screen
        setBackground(UiStyle.BACKGROUND); // dark background
        setLayout(new GridBagLayout()); // center content in a grid

        GridBagConstraints gbc = new GridBagConstraints(); // layout settings
        gbc.insets = new Insets(12, 12, 12, 12); // spacing around components
        gbc.gridx = 0; // single column

        JLabel title = new JLabel("GAME PREVIEW & MAP INFO"); // heading
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32)); // big monospaced font
        title.setForeground(Color.YELLOW); // yellow text

        // The info text block follows; its lines cannot carry comments, so they are described here:
        // one line per level (size, coins, mimics, damage), then bombs, moves, chest and blue bomb rules.
        JTextArea info = new JTextArea("""
            LEVEL 1 (EASY): 15x15 | 3 coins | 1 Mimic + 1 fake chest | 1 heart
            LEVEL 2 (MID):  31x25 | 4 coins | 1 Mimic + 1 fake chest | 2 hearts
            LEVEL 3 (HARD): 35x31 | 5 coins | 2 Mimics + 1 fake chest | 1 heart
            Level 1: 10 black + 5 blue (15 pickups).  Levels 2-3: 20 black
            + 5 blue (25 pickups).
            Moves: L1 by route length, L2 725, L3 888.  Coin = +67 moves.

            Goal: open the real chest (press E next to it) -> Exit unlocks
            -> reach E.  E next to a fake chest reveals a Mimic instead.
            Blue bomb: pick up, SPACE to place, stuns Mimics 3.5 s.
            Maps are random every game.
            """); // end of the info text block
        info.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16)); // monospaced bold
        info.setForeground(Color.GREEN); // green text
        info.setBackground(UiStyle.PANEL); // panel color
        info.setEditable(false); // read only
        info.setFocusable(false); // never takes keyboard focus
        info.setMargin(new Insets(15, 15, 15, 15)); // inner padding

        JButton startBtn = UiStyle.button("ENTER MAZE", 22, 240, 50, new Color(30, 120, 60)); // start button
        startBtn.addActionListener(e -> ui.showScreen("GAME")); // open the game

        JButton backBtn = UiStyle.button("BACK", 16, 140, 40, new Color(60, 60, 70)); // back button
        backBtn.addActionListener(e -> ui.showScreen("MENU")); // return to the menu

        gbc.gridy = 0; // row 0
        add(title, gbc); // add heading
        gbc.gridy = 1; // row 1
        add(info, gbc); // add level info
        gbc.gridy = 2; // row 2
        add(startBtn, gbc); // add start button
        gbc.gridy = 3; // row 3
        add(backBtn, gbc); // add back button
    } // end of constructor
} // end of GamePreview