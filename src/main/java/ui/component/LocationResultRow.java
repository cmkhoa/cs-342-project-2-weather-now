package ui.component;

import models.location.LocationWeather;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;
import weather.Period;

/**
 * A single result row in the Scene 3 city search / pinned list.
 * Clicking anywhere on the row fires the provided onClick callback,
 * which Scene3Controller uses to trigger the Stage-2 load + Scene 1 navigation.
 */
public class LocationResultRow extends HBox {
    public LocationResultRow(LocationWeather location, Runnable onClick) {
        super();
        // Import properties from the css file
        getStyleClass().add("location-result-row");

        // Create the label for the location name and temperature of the current 12hr period
        //  style them via the css file
        Label nameLabel = new Label(location.displayName != null ?
                location.displayName : "--");
        nameLabel.getStyleClass().add("result-location-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- NEW: Load the animated GIF icon for the current period ---
        Region iconRegion = new Region(); // Default empty region
        if (location.periods12hr != null && !location.periods12hr.isEmpty()) {
            Period current = location.periods12hr.get(0);
            iconRegion = SvgIcon.load(IconRouter.getLocalPath(current.icon, current.isDaytime));
        }
        iconRegion.getStyleClass().add("result-icon");
        // --------------------------------------------------------------

        Label tempLabel = new Label((location.periods12hr != null && !location.periods12hr.isEmpty()) ?
                TempConverter.format(location.currentTemp) : "--");
        tempLabel.getStyleClass().add("result-temp-label");

        // Add the iconRegion right between the spacer and the tempLabel
        getChildren().addAll(nameLabel, spacer, iconRegion, tempLabel);

        // Handle the click action
        if (onClick != null) {
            setOnMouseClicked(e -> onClick.run());
            getStyleClass().add("result-row-clickable");
        }
    }
}
