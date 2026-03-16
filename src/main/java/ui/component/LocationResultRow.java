package ui.component;

import models.location.LocationWeather;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import utils.TempConverter;

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

//        TODO: Change this into the icon using LocationWeather's currentIcon
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tempLabel = new Label((location.periods12hr != null && !location.periods12hr.isEmpty()) ?
                TempConverter.format(location.currentTemp) : "--");
        tempLabel.getStyleClass().add("result-temp-label");

        getChildren().addAll(nameLabel, spacer, tempLabel);

        // Handle the click action
        if (onClick != null) {
            setOnMouseClicked(e -> onClick.run());
            getStyleClass().add("result-row-clickable");
        }
    }
}
