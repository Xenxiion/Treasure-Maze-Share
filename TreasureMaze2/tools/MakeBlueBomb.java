import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MakeBlueBomb {

    public static void main(String[] args) {
        try {
            File srcFile = new File("assets/custom/bomb.png");
            if (!srcFile.exists()) {
                System.out.println("Error: source file not found: " + srcFile.getPath());
                return;
            }

            BufferedImage src = ImageIO.read(srcFile);
            if (src == null) {
                System.out.println("Error: could not decode image: " + srcFile.getPath());
                return;
            }

            int width = src.getWidth();
            int height = src.getHeight();
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = src.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    int[] newColor = mapColor(r, g, b);

                    int newArgb = (alpha << 24) | (newColor[0] << 16) | (newColor[1] << 8) | newColor[2];
                    out.setRGB(x, y, newArgb);
                }
            }

            File outFile = new File("assets/custom/bomb_blue.png");
            ImageIO.write(out, "png", outFile);
            System.out.println("Wrote " + outFile.getPath() + " (" + width + "x" + height + " pixels)");

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static int[] mapColor(int r, int g, int b) {
        if (r == 38 && g == 40 && b == 56) {
            return new int[]{16, 36, 110};
        }
        if (r == 70 && g == 74 && b == 96) {
            return new int[]{40, 100, 215};
        }
        if (r == 150 && g == 160 && b == 190) {
            return new int[]{170, 215, 255};
        }
        return new int[]{r, g, b};
    }
}
