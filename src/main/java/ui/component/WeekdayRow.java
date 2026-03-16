package ui.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import util.IconRouter;
import util.SvgIcon;
import util.TempConverter;
import weather.Period;

import java.text.SimpleDateFormat;

/**
 * A single row in the Scene 1 "Next Forecast" 3-day section.
 * Figma spec (node 62:66):
 *   Width: full (360px inside card)   Height: ~50px
 *   Layout: [ <weekday> ]  [ icon 30×30 ]  [ <hi/lo> ]
 *   Font: 14px Inter
 * NWS 12-hr period pairing convention:
 *   dayPeriod   (isDaytime=true)  → temperature is the HIGH
 *   nightPeriod (isDaytime=false) → temperature is the LOW
 * The icon and weekday name come from the day period.
 */
public class WeekdayRow extends HBox {


    public WeekdayRow(Period dayPeriod, Period nightPeriod) {
        super();

        getStyleClass().add("weekday-row");

        // -- Weekday label (left) --
        Label weekdayLabel = new Label(formatWeekday(dayPeriod));
        weekdayLabel.getStyleClass().addAll("weekday-label", "weekday-label-left");

        // -- Spacer --
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        // -- Weather icon (center) --
        Region icon = buildIcon(dayPeriod);

        // -- Spacer --
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // -- Hi / Lo label (right) --
        Label hiLoLabel = new Label(formatHiLo(dayPeriod, nightPeriod));
        hiLoLabel.getStyleClass().addAll("weekday-label", "weekday-label-right");

        getChildren().addAll(weekdayLabel, spacerLeft, icon, spacerRight, hiLoLabel);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Formats the period's startTime as the weekday name, e.g. "Monday". */
    private String formatWeekday(Period period) {
        if (period == null || period.startTime == null) return "--";
        try {
            return new SimpleDateFormat("EEEE").format(period.startTime);
        } catch (Exception e) {
            return period.name != null ? period.name : "--";
        }
    }

    /** Formats hi/lo as "72° / 55°" using the current unit. */
    private String formatHiLo(Period dayPeriod, Period nightPeriod) {
        String hi = dayPeriod   != null ? String.valueOf(TempConverter.convert(dayPeriod.temperature))   : "--";
        String lo = nightPeriod != null ? String.valueOf(TempConverter.convert(nightPeriod.temperature)) : "--";
        return hi + TempConverter.symbol() + " / " + lo + TempConverter.symbol();
    }

    /** Loads the day-period BOM static SVG icon; returns a scaled Region. */
    private Region buildIcon(Period period) {
        if (period == null) return new Region();
        String resourcePath = IconRouter.getLocalPath(period.icon, period.isDaytime);
        Region region = SvgIcon.load(resourcePath);
        region.getStyleClass().add("weekday-icon");
        return region;
    }
}
