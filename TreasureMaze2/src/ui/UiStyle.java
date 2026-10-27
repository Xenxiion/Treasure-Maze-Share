package ui; // package for screens

import java.awt.Color; // RGB color
import java.awt.Dimension; // width/height pair
import java.awt.Font; // text font
import javax.swing.JButton; // clickable button

final class UiStyle { // shared colors and button factory

    static final Color BACKGROUND = new Color(15, 15, 25); // screen background
    static final Color PANEL = new Color(25, 25, 35); // text box background

    private UiStyle() { // private: never instantiated
    } // end of constructor

    static JButton button(String text, int fontSize, int width, int height, Color background) { // build a styled button
        JButton btn = new JButton(text); // create the button
        btn.setFont(new Font(Font.MONOSPACED, Font.BOLD, fontSize)); // monospaced bold text
        btn.setPreferredSize(new Dimension(width, height)); // fixed size
        btn.setFocusPainted(false); // no focus outline
        btn.setOpaque(true); // draw the background color
        btn.setBackground(background); // set the color
        btn.setForeground(Color.WHITE); // white text
        return btn; // finished button
    } // end of button
} // end of UiStyle
