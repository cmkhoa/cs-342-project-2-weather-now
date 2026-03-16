package component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
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

    private static final double ICON_SIZE  = 30;
    private static final double ROW_HEIGHT = 50;

    public WeekdayRow(Period dayPeriod, Period nightPeriod) {
        super();

        setPrefHeight(ROW_HEIGHT);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(10));
        getStyleClass().add("weekday-row");

        // -- Weekday label (left) --
        Label weekdayLabel = new Label(formatWeekday(dayPeriod));
        weekdayLabel.getStyleClass().add("weekday-label");
        weekdayLabel.setPrefWidth(78);

        // -- Spacer --
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        // -- Weather icon (center) --
        Group icon = buildIcon(dayPeriod);

        // -- Spacer --
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // -- Hi / Lo label (right) --
        Label hiLoLabel = new Label(formatHiLo(dayPeriod, nightPeriod));
        hiLoLabel.getStyleClass().add("weekday-label");
        hiLoLabel.setAlignment(Pos.CENTER_RIGHT);
        hiLoLabel.setPrefWidth(47);

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

    /** Loads the day-period BOM static SVG icon; returns a scaled Group. */
    private Group buildIcon(Period period) {
        if (period == null) return new Group();
        String resourcePath = IconRouter.getLocalPath(period.icon, period.isDaytime);
        return SvgIcon.load(resourcePath, ICON_SIZE);
    }
}
