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
 * Rasterizes SVG weather icons to JavaFX ImageViews using Apache Batik.
 *
 * Why Batik?
 *   JavaFX's built-in Image loader does not support SVG. The animated
 *   icons in weather-icons-main/animated/ use CSS keyframes, {@code <line>},
 *   {@code <polygon>}, and complex {@code <g>} transform chains that a
 *   hand-rolled DOM parser cannot reliably reproduce. Batik's
 *   SVGAbstractTranscoder handles the full SVG 1.1 spec and produces a
 *   pixel-perfect BufferedImage that SwingFXUtils.toFXImage() converts to
 *   a JavaFX WritableImage.
 *
 * Note on animation:
 *   Batik rasterizes one static frame of each SVG (the initial state).
 *   CSS keyframe / SMIL animation is not played — icons appear as their
 *   first frame. For the weather app context this is entirely acceptable.
 *
 * Caching:
 *   rasterization at 200px and cached by resourcePath.
 *
 * Public API:
 * <pre>
 *   Region icon = SvgIcon.load("/weather-icons-main/animated/clear-day.svg");
 * </pre>
 * The returned Region resizes the background image to fit its CSS boundaries.
 */
public class SvgIcon {

    /** Cache key format: "resourcePath" */
    private static final Map<String, WritableImage> CACHE = new ConcurrentHashMap<>();
    private static final int RASTER_SIZE = 200;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Loads, rasterizes, and returns an SVG resource as a JavaFX Group.
     *
     * <p>Must be called on the JavaFX Application Thread (or in a
     * {@code Platform.runLater()} block) because {@code SwingFXUtils.toFXImage()}
     * writes to a {@link WritableImage} that must be created on the FX thread.
     * All call sites in this project already satisfy this requirement via the
     * {@code Platform.runLater()} wrapping in {@code JavaFX.java}.
     *
     * @param resourcePath  classpath-relative path starting with '/',
     *                      e.g. "/weather-icons-main/animated/clear-day.svg"
     * @return              a {@link Region} containing the image as a background
     */
    public static Region load(String resourcePath) {
        String cacheKey = resourcePath;

        WritableImage cached = CACHE.get(cacheKey);
        if (cached != null) {
            return wrapInRegion(cached);
        }

        try (InputStream is = SvgIcon.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[SvgIcon] Resource not found: " + resourcePath);
                return placeholder();
            }

            WritableImage fxImage = rasterize(is, RASTER_SIZE);
            if (fxImage == null) {
                System.err.println("[SvgIcon] Rasterization returned null for: " + resourcePath);
                return placeholder();
            }

            CACHE.put(cacheKey, fxImage);
            return wrapInRegion(fxImage);

        } catch (Exception e) {
            System.err.println("[SvgIcon] Failed to load " + resourcePath + ": " + e.getMessage());
            return placeholder();
        }
    }

    /**
     * Clears the rasterization cache.
     * Call this in {@code JavaFX.java}'s refresh cycle if memory pressure
     * is a concern (the cache will be repopulated on next render pass).
     */
    public static void clearCache() {
        CACHE.clear();
    }

    // -----------------------------------------------------------------------
    // Batik rasterization
    // -----------------------------------------------------------------------

    /**
     * Uses Batik's {@link PNGTranscoder} to rasterize an SVG InputStream
     * into a {@link BufferedImage}, then converts it to a JavaFX
     * {@link WritableImage} via {@code SwingFXUtils.toFXImage()}.
     *
     * <p>{@link PNGTranscoder} is used (rather than the abstract
     * {@link org.apache.batik.transcoder.image.ImageTranscoder}) because
     * it is the stable, concrete subclass that correctly initialises Batik's
     * image pipeline. We override {@code writeImage()} to intercept the
     * rendered {@link BufferedImage} before it is written to any stream,
     * so no temporary files or byte buffers are created.
     *
     * @param svgStream  open InputStream of the SVG file (caller is responsible
     *                   for closing via try-with-resources)
     * @param pixelSize  output image width and height in pixels
     * @return           a JavaFX WritableImage, or {@code null} on failure
     */
    private static WritableImage rasterize(InputStream svgStream, int pixelSize) {
        // Single-element array lets the anonymous class write back the result
        final BufferedImage[] result = { null };

        PNGTranscoder transcoder = new PNGTranscoder() {
            @Override
            public void writeImage(BufferedImage img, TranscoderOutput output) {
                // Intercept — store the image, skip writing to any stream
                result[0] = img;
            }
        };

        // Output dimensions
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH,  (float) pixelSize);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) pixelSize);

        // Transparent background so icons composite cleanly over any panel colour
        transcoder.addTranscodingHint(
                PNGTranscoder.KEY_BACKGROUND_COLOR,
                new java.awt.Color(0, 0, 0, 0));

        try {
            transcoder.transcode(
                    new TranscoderInput(svgStream),
                    new TranscoderOutput()   // unused — writeImage() fires before output is written
            );
        } catch (Exception e) {
            System.err.println("[SvgIcon] Batik transcoding error: " + e.getMessage());
            return null;
        }

        if (result[0] == null) return null;

        // Convert AWT BufferedImage → JavaFX WritableImage
        return SwingFXUtils.toFXImage(result[0], null);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Wraps a {@link WritableImage} in a fixed-size {@link Region} that scales it. */
    private static Region wrapInRegion(WritableImage image) {
        Region region = new Region();
        BackgroundImage bg = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false)
        );
        region.setBackground(new Background(bg));
        return region;
    }

    /**
     * Returns a small rounded-corner grey rectangle as a fallback.
     */
    private static Region placeholder() {
        Region r = new Region();
        r.setStyle("-fx-background-color: lightgray; -fx-background-radius: 4;");
        return r;
    }
}
