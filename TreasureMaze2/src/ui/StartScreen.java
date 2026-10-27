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

/** "How to play" screen. */
public final class StartScreen extends JPanel { // rules screen

    private static final long serialVersionUID = 1L; // version id for serialization

    public StartScreen(GameUI ui) { // build the screen
        setBackground(UiStyle.BACKGROUND); // dark background
        setLayout(new GridBagLayout()); // center content in a grid

        GridBagConstraints gbc = new GridBagConstraints(); // layout settings
        gbc.insets = new Insets(10, 10, 10, 10); // spacing around components
        gbc.gridx = 0; // single column

        JLabel title = new JLabel("HOW TO PLAY"); // heading
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 36)); // big monospaced font
        title.setForeground(Color.CYAN); // cyan text

        // The rules text block follows; its lines cannot carry comments, so they are described here:
        // 8 numbered rules: keys, goal, coins, chest controls, hearts and countdown, mimic counts and damage, bombs, move limits.
        JTextArea rules = new JTextArea("""
            1. Move: WASD or Arrows.  M sound on/off.
               Esc opens the pause menu.
            2. GOAL: open the REAL treasure chest (+500).  That unlocks
               the Exit (E).  Reach E to finish the level.  (Collecting
               every coin also unlocks E.)
            3. Coins are optional: +100 points and +67 moves each.
            4. CHEST CONTROLS: chests open ONLY when you press E
               next to them (diagonals count).  E next to a
               chest that is really a MIMIC reveals the Mimic: no
               treasure, no unlock, and it freezes 0.5 s so you can run.
               Fake chests look exactly like real ones.  E with no
               chest around you does nothing.
            5. Hearts: 4.  At 0 a 5 second countdown starts: reach the
               open exit in time (1 heart back) or the game ends.
            6. MIMICS (hearts lost per catch):
               L1: 1 Mimic + 1 fake chest (1)   L2: 1 + 1 fake (2)
               L3: 2 Mimics + 1 fake chest (1)  Fake ones move 3x slower.
            7. BOMBS: 20 black (cost a heart, level 1: eliminate you)
               and 5 blue per level.  Walk over a blue bomb to pick it up,
               SPACE places it: it stuns Mimics within 4 cells for 3.5 s.
            8. MOVES: level 1 by route length, level 2 = 725, level 3 = 888.
               Out of moves: -1 heart.  Level 1 floor tiles crumble.
            """); // end of the rules text block
        rules.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14)); // readable font
        rules.setForeground(Color.WHITE); // white text
        rules.setBackground(UiStyle.PANEL); // panel color
        rules.setEditable(false); // read only
        rules.setFocusable(false); // never takes keyboard focus
        rules.setMargin(new Insets(15, 15, 15, 15)); // inner padding

        JButton backBtn = UiStyle.button("BACK TO MENU", 18, 220, 45, new Color(40, 40, 60)); // back button
        backBtn.addActionListener(e -> ui.showScreen("MENU")); // return to the menu

        gbc.gridy = 0; // row 0
        add(title, gbc); // add heading
        gbc.gridy = 1; // row 1
        add(rules, gbc); // add rules
        gbc.gridy = 2; // row 2
        add(backBtn, gbc); // add back button
    } // end of constructor
} // end of StartScreen
