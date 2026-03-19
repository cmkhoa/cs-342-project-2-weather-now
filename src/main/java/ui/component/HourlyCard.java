package ui.component;

import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import models.hourlyForecast.HourlyPeriod;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;

import java.text.SimpleDateFormat;

/**
 * A single card in the hourly forecast scroll strip (Scene 1).
 * Layout: hour + icon + temp
 */
public class HourlyCard extends VBox {
    public HourlyCard(HourlyPeriod period) {
        super(4);
        getStyleClass().add("hourly-card");
        setAlignment(Pos.CENTER);

        Label hourLabel = new Label(formatHour(period));
        hourLabel.getStyleClass().add("hourly-hour-label");
        hourLabel.setAlignment(Pos.CENTER);
        hourLabel.setMaxWidth(Double.MAX_VALUE);

        Region icon = getIcon(period);

        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.getStyleClass().add("hourly-temp-label");
        tempLabel.setAlignment(Pos.CENTER);
        tempLabel.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(hourLabel, icon, tempLabel);
    }

    private String formatHour(HourlyPeriod period) {
        if (period.startTime == null) return "--";
        try { return new java.text.SimpleDateFormat("h a").format(period.startTime); }
        catch (Exception e) { return "--"; }
    }

    private Region getIcon(HourlyPeriod period) {
        Region region = SvgIcon.load(IconRouter.getLocalPath(period.icon, period.isDaytime));
        region.getStyleClass().add("hourly-icon");
        return region;
    }
}