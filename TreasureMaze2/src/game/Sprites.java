package game; // package for core game objects

import data.AssetPaths; // finds the assets folder
import java.awt.Graphics2D; // drawing surface
import java.awt.RenderingHints; // drawing quality options
import java.awt.image.BufferedImage; // in-memory picture
import java.io.File; // file path
import java.io.IOException; // thrown when an image cannot be read
import java.util.HashMap; // name -> image lookup table
import java.util.Map; // map interface
import javax.imageio.ImageIO; // reads PNG files

/**
 * Loads the pixel-art pictures once and draws them crisp (no smoothing).
 * Tiles come from Kenney "Tiny Dungeon" (CC0); the explosion from Kenney "Explosion Pack" (CC0);
 * bomb, coin and heart are drawn by tools/make_custom_sprites.py.
 * If a file is missing the game still runs: callers fall back to letters.
 */
// Loads each small picture once into a table (name -> image) and draws it scaled with nearest-neighbour,
// which keeps pixel art sharp. If a picture is missing, the game draws a letter instead, so it still runs.
public final class Sprites { // picture library

    private static final int EXPLOSION_FRAMES = 9; // pixelExplosion00 .. pixelExplosion08

    private final Map<String, BufferedImage> images = new HashMap<>(); // sprite name -> picture

    public Sprites() { // load every sprite
        String td = "kenney-tiny-dungeon/tile_"; // folder + file prefix of the dungeon tiles
        load("floor0", td + "0000.png"); // plain dungeon floor
        load("floor1", td + "0001.png"); // second floor variant
        load("cracked", td + "0012.png"); // cracked floor (level 1 tile about to vanish)
        load("wall0", td + "0040.png"); // stone brick wall
        load("wall1", td + "0014.png"); // second brick wall variant
        load("wall2", td + "0028.png"); // wall with a barred window
        load("doorClosed", td + "0045.png"); // locked exit door
        load("doorOpen", td + "0010.png"); // open exit doorway
        load("chestClosed", td + "0090.png"); // treasure chest, closed
        load("chestOpen", td + "0091.png"); // treasure chest, open
        load("mimic", td + "0092.png"); // mimic (chest with teeth and tongue)
        load("player", td + "0097.png"); // knight with visor
        load("bomb", "custom/bomb.png"); // bomb (custom sprite)
        load("bombBlue", "custom/bomb_blue.png"); // blue Mimic-trap bomb (recolored bomb)
        load("coin", "custom/coin.png"); // coin (custom sprite)
        load("heart", "custom/heart.png"); // heart (custom sprite)
        for (int i = 0; i < EXPLOSION_FRAMES; i++) { // each explosion frame
            load("explosion" + i, String.format("kenney-explosion/pixelExplosion%02d.png", i)); // frame i
        } // end of frame loop
    } // end of constructor

    private void load(String name, String relativePath) { // read one image into the table
        try { // reading can fail
            File file = AssetPaths.file(relativePath); // locate the file
            if (file.isFile()) { // it exists
                BufferedImage image = ImageIO.read(file); // decode the PNG
                if (image != null) { // decoded successfully
                    images.put(name, image); // remember it
                } // end of decode check
            } // end of exists check
        } catch (IOException e) { // unreadable file
            // leave the sprite out: the game falls back to a letter for this object
        } // end of try/catch
    } // end of load

    public boolean has(String name) { // is this sprite available
        return images.containsKey(name); // true if it loaded
    } // end of has

    /** Draws a sprite scaled to a square of {@code size} pixels, optionally mirrored left-right. */
    public void draw(Graphics2D g, String name, int x, int y, int size, boolean flipX) { // draw one sprite
        BufferedImage image = images.get(name); // look it up
        if (image == null) return; // missing sprite: draw nothing
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // crisp pixels
        if (flipX) { // mirrored
            g.drawImage(image, x + size, y, -size, size, null); // negative width flips the picture
        } else { // normal
            g.drawImage(image, x, y, size, size, null); // draw scaled
        } // end of flip check
    } // end of draw

    /** Draws a sprite scaled to an exact width and height (used for the explosion, which is not square). */
    public void drawScaled(Graphics2D g, String name, int x, int y, int width, int height) { // draw with custom size
        BufferedImage image = images.get(name); // look it up
        if (image == null) return; // missing sprite: draw nothing
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // crisp pixels
        g.drawImage(image, x, y, width, height, null); // draw scaled
    } // end of drawScaled

    public int explosionFrames() { // number of explosion frames
        return EXPLOSION_FRAMES; // constant
    } // end of explosionFrames
} // end of Sprites
