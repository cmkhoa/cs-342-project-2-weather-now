package ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;
import weather.Period;

import java.text.SimpleDateFormat;

/**
 * A single row in the Scene 1 "Next Forecast" 3-day section.
 * Layout: Weekday + icon + hi/lo
 */
public class WeekdayRow extends HBox {
    public WeekdayRow(Period dayPeriod, Period nightPeriod) {
        super();
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("weekday-row");

        // Weekday name label (left column, fixed width)
        Label weekdayLabel = new Label(formatWeekday(dayPeriod));
        weekdayLabel.getStyleClass().addAll("weekday-label", "weekday-label-left");
        weekdayLabel.setAlignment(Pos.CENTER_LEFT);

        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        // Weather icon centered
        Region icon = getIcon(dayPeriod);

        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // Hi / Lo label (right column, fixed width, right-aligned text)
        Label hiLoLabel = new Label(formatHiLo(dayPeriod, nightPeriod));
        hiLoLabel.getStyleClass().addAll("weekday-label", "weekday-label-right");
        hiLoLabel.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(weekdayLabel, spacerLeft, icon, spacerRight, hiLoLabel);
    }

    private String formatWeekday(Period period) {
        if (period == null || period.startTime == null) return "--";
        try {
            return new SimpleDateFormat("EEEE").format(period.startTime);
        } catch (Exception e) { return period.name != null ? period.name : "--"; }
    }

    private String formatHiLo(Period dayPeriod, Period nightPeriod) {
        String hi = (dayPeriod   != null) ? String.valueOf(TempConverter.convert(dayPeriod.temperature))   : "--";
        String lo = (nightPeriod != null) ? String.valueOf(TempConverter.convert(nightPeriod.temperature)) : "--";
        return hi + TempConverter.symbol() + " / " + lo + TempConverter.symbol();
    }

    private Region getIcon(Period period) {
        Region region = SvgIcon.load(IconRouter.getLocalPath(period.icon, period.isDaytime));
        region.getStyleClass().add("weekday-icon");
        return region;
    }
}