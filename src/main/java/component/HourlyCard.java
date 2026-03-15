package component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.HourlyPeriod;
import util.IconRouter;
import util.SvgIcon;
import util.TempConverter;

import java.text.SimpleDateFormat;

/**
 * A single card in the horizontal hourly forecast scroll strip (Scene 1).
 *
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

    private static final double CARD_WIDTH  = 74;
    private static final double CARD_HEIGHT = 110;
    private static final double ICON_SIZE   = 30;
    private static final double FONT_SIZE   = 14;

    public HourlyCard(HourlyPeriod period) {
        super(6); // spacing between children

        setPrefWidth(CARD_WIDTH);
        setPrefHeight(CARD_HEIGHT);
        setMinWidth(CARD_WIDTH);
        setMaxWidth(CARD_WIDTH);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(8));
        getStyleClass().add("hourly-card");

        // -- Hour label --
        Label hourLabel = new Label(formatHour(period));
        hourLabel.setStyle("-fx-font-size: " + FONT_SIZE + "px; -fx-font-family: 'Inter', sans-serif;");
        hourLabel.setAlignment(Pos.CENTER);

        // -- Weather icon --
        Group icon = buildIcon(period);

        // -- Temperature label --
        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.setStyle("-fx-font-size: " + FONT_SIZE + "px; -fx-font-family: 'Inter', sans-serif;");
        tempLabel.setAlignment(Pos.CENTER);

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

    /** Loads the BOM static SVG icon via SvgIcon parser; returns a scaled Group. */
    private Group buildIcon(HourlyPeriod period) {
        String resourcePath = IconRouter.getLocalPath(period.icon, period.isDaytime);
        return SvgIcon.load(resourcePath, ICON_SIZE);
    }
}
