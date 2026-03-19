package utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.image.WritableImage;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rasterizes SVG weather icons and UI icons to JavaFX Regions using Apache Batik.
 *
 * Two entry points:
 *  {@link #load(String)}              — standard weather icon at 200px (cached).
 *  {@link #loadTinted(String, int)}   — UI icon rasterized at a given size, pixels
 *                                       converted to white (preserving alpha) for use
 *                                       on dark/glass backgrounds.
 */
public class SvgIcon {

    private static final Map<String, WritableImage> CACHE        = new ConcurrentHashMap<>();
    private static final Map<String, WritableImage> TINTED_CACHE = new ConcurrentHashMap<>();
    private static final int RASTER_SIZE = 200;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Loads a weather SVG at 200px, cached. */
    public static Region load(String resourcePath) {
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

    /**
     * Loads a UI icon SVG, tinted to solid white (alpha preserved).
     * Used for home, back, more, location icons over glass/dark backgrounds.
     *
     * @param resourcePath  e.g. "/ui-icons/homeButton.svg"
     * @param pixelSize     raster output size in pixels
     */
    public static Region loadTinted(String resourcePath, int pixelSize) {
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

    public static void clearCache() {
        CACHE.clear();
        TINTED_CACHE.clear();
    }

    // -----------------------------------------------------------------------
    // Batik rasterization
    // -----------------------------------------------------------------------

    private static WritableImage rasterize(InputStream svgStream, int pixelSize, boolean tintWhite) {
        final BufferedImage[] result = { null };

        PNGTranscoder transcoder = new PNGTranscoder() {
            @Override
            public void writeImage(BufferedImage img, TranscoderOutput output) {
                result[0] = img;
            }
        };
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH,  (float) pixelSize);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) pixelSize);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, new java.awt.Color(0, 0, 0, 0));

        try {
            transcoder.transcode(new TranscoderInput(svgStream), new TranscoderOutput());
        } catch (Exception e) {
            System.err.println("[SvgIcon] Batik error: " + e.getMessage());
            return null;
        }

        if (result[0] == null) return null;

        BufferedImage img = result[0];
        if (tintWhite) img = tintToWhite(img);
        return SwingFXUtils.toFXImage(img, null);
    }

    /** Converts all pixels to white preserving original alpha channel. */
    private static BufferedImage tintToWhite(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb  = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                out.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Region wrapInRegion(WritableImage image) {
        Region region = new Region();
        region.setBackground(new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false)
        )));
        return region;
    }

    private static Region placeholder() {
        Region r = new Region();
        r.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 4;");
        return r;
    }
}