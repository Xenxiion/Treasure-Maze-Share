import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class MakeCustomSprites {

    private static final String[] BOMB = {
        "....oo..........",
        "...o..o.........",
        "....oo..........",
        "......k.........",
        "....kkkkk.......",
        "...kkkkkkk......",
        "..kkkkkkkkk.....",
        "..kkkwwkkkk.....",
        "..kkwwwkkkk.....",
        "..kkkkkkkkk.....",
        "..kkkkkkkkk.....",
        "...kkkkkkk......",
        "...ggkkkgg......",
        "....ooooo.......",
        "................",
        "................"
    };

    private static final String[] COIN = {
        "................",
        "....oooooo......",
        "...oyyyyyyo.....",
        "..oyyyyyyyyo....",
        ".oyylyyyyyyo....",
        ".oyllyyyyyyo....",
        "oyyyyyyyyyyyo...",
        "oyyyyyyyyyyyo...",
        "oyyyyyyyyyyyo...",
        "oyyyyyyyyyyyo...",
        ".oyyyyyyyyyo....",
        ".oyyyyddyyyo....",
        "..oyyyddyyo.....",
        "...oyyyyyo......",
        "....oooooo......",
        "................"
    };

    private static final String[] HEART = {
        "................",
        "...rrr....rrr...",
        "..rrrrr..rrrrr..",
        ".rrrrrrrrrrrrrr.",
        ".rrrpprrrrpprrr.",
        "rrrpppprrpppprrr",
        "rrrrrrrrrrrrrrrr",
        "rrrrrrrrrrrrrrrr",
        ".rrrrrrrrrrrrrr.",
        ".rrrrrrrrrrrrrr.",
        "..rrrrrrrrrrrr..",
        "..rrrrrrrrrrrr..",
        "...rrrrrrrrrr...",
        "....rrrrrrrr....",
        ".....rrrrrr.....",
        "......rrrr......"
    };

    public static void main(String[] args) {
        try {
            File outDir = new File("assets/custom");
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            Color outline = sampleOutlineColor("assets/kenney-tiny-dungeon/tile_0090.png");

            Map<Character, Color> palette = new HashMap<>();
            palette.put('o', outline);
            palette.put('k', new Color(38, 40, 56));
            palette.put('g', new Color(70, 74, 96));
            palette.put('w', new Color(150, 160, 190));
            palette.put('y', new Color(240, 190, 60));
            palette.put('d', new Color(190, 120, 40));
            palette.put('l', new Color(255, 235, 150));
            palette.put('f', new Color(170, 120, 70));
            palette.put('s', new Color(255, 120, 40));
            palette.put('t', new Color(255, 225, 90));
            palette.put('r', new Color(210, 50, 60));
            palette.put('p', new Color(255, 130, 140));

            int written = 0;
            written += writeSpriteSafe(BOMB, palette, "assets/custom/bomb.png");
            written += writeSpriteSafe(COIN, palette, "assets/custom/coin.png");
            written += writeSpriteSafe(HEART, palette, "assets/custom/heart.png");

            System.out.println(written + " sprite(s) written.");
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
        }
    }

    private static Color sampleOutlineColor(String path) {
        Color fallback = new Color(30, 30, 40);
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Warning: " + path + " not found. Using fallback outline color.");
            return fallback;
        }
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                System.out.println("Warning: could not decode " + path + ". Using fallback outline color.");
                return fallback;
            }
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha != 0) {
                        return new Color(argb, true);
                    }
                }
            }
            int argb00 = img.getRGB(0, 0);
            return new Color(argb00, true);
        } catch (IOException e) {
            System.out.println("Warning: error reading " + path + ": " + e.getMessage());
            return fallback;
        }
    }

    private static int writeSpriteSafe(String[] rows, Map<Character, Color> palette, String outPath) {
        try {
            int size = rows.length;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < size; y++) {
                String row = rows[y];
                for (int x = 0; x < size; x++) {
                    char c = row.charAt(x);
                    if (c == '.') {
                        img.setRGB(x, y, 0x00000000);
                    } else {
                        Color col = palette.get(c);
                        if (col == null) {
                            img.setRGB(x, y, 0x00000000);
                        } else {
                            img.setRGB(x, y, col.getRGB() | 0xFF000000);
                        }
                    }
                }
            }
            File outFile = new File(outPath);
            ImageIO.write(img, "png", outFile);
            System.out.println("Wrote " + outPath);
            return 1;
        } catch (IOException e) {
            System.out.println("Error writing " + outPath + ": " + e.getMessage());
            return 0;
        }
    }
}
