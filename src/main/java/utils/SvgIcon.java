package utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rasterizes SVG weather icons and UI icons to JavaFX Regions using Apache Batik.
 * JavaFX has no native SVG support, so Batik converts each SVG to a BufferedImage
 * which is then wrapped in a JavaFX Region as a background image.
 * reference: BufferedImageTranscoder: https://gist.github.com/ComFreek/b0684ac324c815232556
 */
public class SvgIcon {

    // Separate caches for normal and tinted icons so they don't collide on the same path key
    private static final Map<String, WritableImage> CACHE        = new ConcurrentHashMap<>();
    private static final Map<String, WritableImage> TINTED_CACHE = new ConcurrentHashMap<>();

    // default svg size, to be scaled by css
    private static final int RASTER_SIZE = 200;

    // load a weather svg from the given path, results are cached to avoid rerendering
    public static Region load(String resourcePath) {
        // Return cached image if already rasterized
        WritableImage cached = CACHE.get(resourcePath);
        if (cached != null) return wrapInRegion(cached);

        try (InputStream is = SvgIcon.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[SvgIcon] Not found: " + resourcePath);
                return placeholder();
            }
            WritableImage img = rasterize(is, RASTER_SIZE, false);
            if (img == null) return placeholder();
            CACHE.put(resourcePath, img);
            return wrapInRegion(img);
        } catch (Exception e) {
            System.err.println("[SvgIcon] Load failed " + resourcePath + ": " + e.getMessage());
            return placeholder();
        }
    }

    // load but with a white out color for ui buttons
    public static Region loadTinted(String resourcePath, int pixelSize) {
        // Key includes size so the same path can be cached at different sizes without collision
        String key = resourcePath + "@" + pixelSize;
        WritableImage cached = TINTED_CACHE.get(key);
        if (cached != null) return wrapInRegion(cached);

        try (InputStream is = SvgIcon.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[SvgIcon] Tinted not found: " + resourcePath);
                return placeholder();
            }
            WritableImage img = rasterize(is, pixelSize, true);
            if (img == null) return placeholder();
            TINTED_CACHE.put(key, img);
            return wrapInRegion(img);
        } catch (Exception e) {
            System.err.println("[SvgIcon] Tinted failed " + resourcePath + ": " + e.getMessage());
            return placeholder();
        }
    }

    // clear both cache, though i dont think the app is heavy enough
    public static void clearCache() {
        CACHE.clear();
        TINTED_CACHE.clear();
    }

    /**
     * Proper Batik subclass via ImageTranscoder (the abstract base).
     * createImage() allocates the ARGB buffer; writeImage() captures it.
     * from: https://gist.github.com/ComFreek/b0684ac324c815232556
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage img = null;

        @Override
        public BufferedImage createImage(int width, int height) {
            // Allocate a transparent ARGB buffer for Batik to render into
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput to) throws TranscoderException {
            // Capture the rendered image instead of writing it to any stream
            this.img = img;
        }

        public BufferedImage getBufferedImage() { return img; }
    }

    /**
     * Rasterizes an SVG stream to a JavaFX WritableImage.
     *
     * @param svgStream  open InputStream for the SVG (caller closes via try-with-resources)
     * @param pixelSize  output width and height in pixels
     * @param tintWhite  if true, all pixels are replaced with white (alpha preserved)
     * @return           a WritableImage, or null if transcoding fails
     */
    private static WritableImage rasterize(InputStream svgStream, int pixelSize, boolean tintWhite) {
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH,  (float) pixelSize);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) pixelSize);
        // Transparent background so the icon composites cleanly over any panel color
        transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, new java.awt.Color(0, 0, 0, 0));

        try {
            transcoder.transcode(new TranscoderInput(svgStream), null);
        } catch (Exception e) {
            System.err.println("[SvgIcon] Batik error: " + e.getMessage());
            return null;
        }

        BufferedImage img = transcoder.getBufferedImage();
        if (img == null) return null;
        if (tintWhite) img = tintToWhite(img);
        // Convert AWT BufferedImage to JavaFX WritableImage
        return SwingFXUtils.toFXImage(img, null);
    }

    // change every pixel to white for filling
    private static BufferedImage tintToWhite(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb  = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;               // extract original alpha
                out.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);  // white + original alpha
            }
        }
        return out;
    }

    //wrap writable image into a region as a background
    private static Region wrapInRegion(WritableImage image) {
        Region region = new Region();
        region.setBackground(new Background(new BackgroundImage(image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false)
        )));
        return region;
    }

    // default grey box fallback for when no icons are available
    private static Region placeholder() {
        Region r = new Region();
        r.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 4;");
        return r;
    }
}