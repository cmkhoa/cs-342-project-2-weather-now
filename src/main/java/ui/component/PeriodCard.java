package ui.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;
import weather.Period;

/**
 * A single period card in the Scene 2 scrollable forecast list.
 * basically has 2 rows, the first with the period name, an icon, the temperature
 * the second row has the wind speed, the wind dir and the chance of precipitation
 */
public class PeriodCard extends VBox {
    public PeriodCard(Period period) {
        super();
        // Import properties from the CSS file
        getStyleClass().add("period-card");

        // call builder functions for the inner 2 rows
        getChildren().addAll(
                buildInfo1Row(period),
                buildInfo2Row(period)
        );
    }

    private HBox buildInfo1Row(Period period) {
        HBox row = new HBox();
        // Import properties from the CSS file
        row.getStyleClass().add("period-info1-row");

        // create the label for the Period name and the temperature
        Label nameLabel = new Label(period.name != null ? period.name : "--");
        nameLabel.getStyleClass().add("period-name-label");
        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.getStyleClass().add("period-temp-label");

        // build the icon
        Region icon = buildIcon(period);

        // generate the spacers
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        // return the row
        row.getChildren().addAll(nameLabel, spacerLeft, icon, spacerRight, tempLabel);
        return row;
    }

    private HBox buildInfo2Row(Period period) {
        HBox row = new HBox();
        // Import properties from the CSS file
        row.getStyleClass().add("period-info2-row");

        // create the labels for each entry
        Label windLabel  = makeSubLabel(formatWind(period));
        Label windirLabel = makeSubLabel(period.windDirection != null ? period.windDirection : "--");
        Label precipLabel = makeSubLabel(formatPrecip(period));

        HBox.setHgrow(windLabel,   Priority.ALWAYS);
        HBox.setHgrow(windirLabel, Priority.ALWAYS);
        HBox.setHgrow(precipLabel, Priority.ALWAYS);

        row.getChildren().addAll(windLabel, windirLabel, precipLabel);
        return row;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Label makeSubLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("period-sub-label");
        return l;
    }

    private String formatWind(Period period) {
        return period.windSpeed != null ? period.windSpeed : "-- mph";
    }

    private String formatPrecip(Period period) {
        if (period.probabilityOfPrecipitation == null) {
            return "0% precip";
        }
        return period.probabilityOfPrecipitation.value + "% precip";
    }

    private Region buildIcon(Period period) {
        if (period == null) return new Region();
        String resourcePath = IconRouter.getLocalPath(period.icon, period.isDaytime);
        Region region = SvgIcon.load(resourcePath);
        region.getStyleClass().add("period-icon");
        return region;
    }
}
