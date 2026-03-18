package utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GifIcon {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    public static Region load(String resourcePath) {
        Image image = CACHE.get(resourcePath);

        if (image == null) {
            try (InputStream is = GifIcon.class.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("[GifIcon] Resource not found: " + resourcePath);
                    return placeholder();
                }
                image = new Image(is);
                CACHE.put(resourcePath, image);
            } catch (Exception e) {
                System.err.println("[GifIcon] Failed to load " + resourcePath + ": " + e.getMessage());
                return placeholder();
            }
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        javafx.scene.layout.StackPane region = new javafx.scene.layout.StackPane();
        region.getChildren().add(imageView);

        imageView.fitWidthProperty().bind(region.widthProperty());
        imageView.fitHeightProperty().bind(region.heightProperty());

        return region;
    }

    private static Region placeholder() {
        Region r = new Region();
        r.setStyle("-fx-background-color: light gray; -fx-background-radius: 4;");
        return r;
    }
}
