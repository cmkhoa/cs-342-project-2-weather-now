package component;



import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import hourlyForecast.HourlyPeriod;
import util.IconRouter;
import util.SvgIcon;
import util.TempConverter;

import java.text.SimpleDateFormat;

/**
 * A single card in the horizontal hourly forecast scroll strip (Scene 1).
 * Figma spec (node 60:23):
 *   Width:  74px   Height: 110px
 *   Border: 1px solid black
 *   Padding: 8px
 *   Children (top → bottom, centered):
 *     <Hour>   — 14px, e.g. "3 PM"
 *     icon     — 30×30px SVG/image
 *     <Temp>   — 14px, e.g. "68°F"
 */
public class HourlyCard extends VBox {



    public HourlyCard(HourlyPeriod period) {
        super();

        getStyleClass().add("hourly-card");

        // -- Hour label --
        Label hourLabel = new Label(formatHour(period));
        hourLabel.getStyleClass().addAll("hourly-text", "hourly-hour-label");

        // -- Weather icon --
        Region icon = buildIcon(period);

        // -- Temperature label --
        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.getStyleClass().addAll("hourly-text", "hourly-temp-label");

        getChildren().addAll(hourLabel, icon, tempLabel);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Formats startTime as "3 PM" or "12 AM". */
    private String formatHour(HourlyPeriod period) {
        if (period.startTime == null) return "--";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h a");
            return sdf.format(period.startTime);
        } catch (Exception e) {
            return "--";
        }
    }

    /** Loads the BOM static SVG icon via SvgIcon parser; returns a scaled Region. */
    private Region buildIcon(HourlyPeriod period) {
        String resourcePath = IconRouter.getLocalPath(period.icon, period.isDaytime);
        Region region = SvgIcon.load(resourcePath);
        region.getStyleClass().add("hourly-icon");
        return region;
    }
}
