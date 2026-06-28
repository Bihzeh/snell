import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Crops NMSR full-body frames to the launch card's head→knee framing and scales them to a fixed
 * size. Usage: java Bake.java <rawDir> <outDir> <W> <H> <K>
 *   rawDir  directory of NN.png NMSR renders (one per yaw)
 *   W,H     output size per frame (the launch card uses a 720x928 / ~0.776 aspect)
 *   K       fraction of the model height (hat-top → feet) to keep; ~0.84 lands at the knees
 *
 * The model is centred on the canvas by NMSR, so we crop around the canvas axis (the cape swings
 * but the body stays put). An alpha threshold of 140 ignores NMSR's faint drop shadow so the
 * bounding box is the body, not the shadow.
 */
public class Bake {
    public static void main(String[] a) throws Exception {
        File rawDir = new File(a[0]), outDir = new File(a[1]);
        int W = Integer.parseInt(a[2]), H = Integer.parseInt(a[3]);
        double K = Double.parseDouble(a[4]);
        outDir.mkdirs();
        for (File f : rawDir.listFiles((d, n) -> n.endsWith(".png"))) {
            BufferedImage src = ImageIO.read(f);
            int iw = src.getWidth(), ih = src.getHeight(), minY = ih, maxY = 0;
            for (int y = 0; y < ih; y++)
                for (int x = 0; x < iw; x++)
                    if (((src.getRGB(x, y) >>> 24) & 0xff) > 140) { if (y < minY) minY = y; if (y > maxY) maxY = y; }
            int cropH = (int) Math.round((maxY - minY) * K);
            int cropW = (int) Math.round(cropH * (double) W / H);
            int x0 = Math.max(0, iw / 2 - cropW / 2);
            cropW = Math.min(cropW, iw - x0);
            cropH = Math.min(cropH, ih - minY);
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src.getSubimage(x0, minY, cropW, cropH), 0, 0, W, H, null);
            g.dispose();
            ImageIO.write(out, "png", new File(outDir, f.getName()));
        }
        System.out.println("baked " + outDir);
    }
}
