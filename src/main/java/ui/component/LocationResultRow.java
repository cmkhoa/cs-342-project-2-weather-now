package ui.component;

import models.location.LocationNode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;
import weather.Period;

/**
 * A single row in Scene 3's search results popup.
 * Layout: city + icon + temp
 */
public class LocationResultRow extends HBox {
    public LocationResultRow(LocationNode location, Runnable onClick) {
        super(8);
        getStyleClass().add("location-result-row");
        setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(location.displayName != null ? location.displayName : "--");
        nameLabel.getStyleClass().add("result-location-label");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Weather icon
        Region iconRegion = new Region();
        if (location.periods12hr != null && !location.periods12hr.isEmpty()) {
            Period current = location.periods12hr.get(0);
            iconRegion = SvgIcon.load(IconRouter.getLocalPath(current.icon, current.isDaytime));
        }
        iconRegion.getStyleClass().add("result-icon");

        // Temperature
        Label tempLabel = new Label((location.periods12hr != null && !location.periods12hr.isEmpty())
                        ? TempConverter.format(location.currentTemp) : "--");
        tempLabel.getStyleClass().add("result-temp-label");
        tempLabel.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(nameLabel, iconRegion, tempLabel); // , pinBtn

        // Row click triggers navigation
        if (onClick != null) {
            getStyleClass().add("result-row-clickable");
            setOnMouseClicked(e -> {
                if (!e.getPickResult().getIntersectedNode().getStyleClass().contains("pin-btn")) {
                    //e.getPickResult().getIntersectedNode() != pinBtn &&
                    onClick.run();
                }
            });
        }
    }
}