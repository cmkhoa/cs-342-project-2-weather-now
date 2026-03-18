package ui.component;

import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import models.hourlyForecast.HourlyPeriod;
import utils.IconRouter;
import utils.GifIcon;
import utils.TempConverter;

import java.text.SimpleDateFormat;

/**
 * A single card in the horizontal hourly forecast scroll strip (Scene 1).
 * The card is layed out vertically (vbox)
 */
public class HourlyCard extends VBox {
    public HourlyCard(HourlyPeriod period) {
        super();
        // Import properties from the CSS file
        getStyleClass().add("hourly-card");

        // Create and style the labels for time and temperature via the css
        Label hourLabel = new Label(formatHour(period));
        hourLabel.getStyleClass().addAll("hourly-text", "hourly-hour-label");
        // Format the time to match the unit system
        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.getStyleClass().addAll("hourly-text", "hourly-temp-label");

        // -- Weather icon --
        Region icon = buildIcon(period);

        getChildren().addAll(hourLabel, icon, tempLabel);
    }

    // Helpers functions

    // Function that formats the start time of each hourly entry
    private String formatHour(HourlyPeriod period) {
        if (period.startTime == null) return "--";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h a");
            return sdf.format(period.startTime);
        } catch (Exception e) {
            return "--";
        }
    }

    // Load each hour's icon and style it to match the cards size
    private Region buildIcon(HourlyPeriod period) {
        String resourcePath = IconRouter.getLocalPath(period.icon, period.isDaytime);
        Region region = GifIcon.load(resourcePath); // <-- CHANGED THIS LINE
        region.getStyleClass().add("hourly-icon");
        return region;
    }
}
