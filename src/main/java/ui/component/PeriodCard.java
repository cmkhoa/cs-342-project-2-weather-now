package ui.component;

import javafx.geometry.Pos;
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
 * A single period card in Scene 2.
 *
 * Row 1: [ Period name (left) ] [spacer] [ weather icon ] [spacer] [ temp (right) ]
 * Row 2: [ wind speed + direction (left, grouped) ] [spacer] [ precip % (right) ]
 *
 * All content is vertically centered within each row.
 * Text is white on the glass background.
 */
public class PeriodCard extends VBox {

    public PeriodCard(Period period) {
        super();
        getStyleClass().add("period-card");
        setSpacing(8);

        getChildren().addAll(
                buildInfoRow(period),
                buildSubRow(period)
        );
    }

    /** Row 1: name — icon — temperature */
    private HBox buildInfoRow(Period period) {
        HBox row = new HBox();
        row.getStyleClass().add("period-info1-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(period.name != null ? period.name : "--");
        nameLabel.getStyleClass().add("period-name-label");

        Region icon = buildIcon(period);

        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.getStyleClass().add("period-temp-label");

        Region spacerL = new Region(); HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region(); HBox.setHgrow(spacerR, Priority.ALWAYS);

        row.getChildren().addAll(nameLabel, spacerL, icon, spacerR, tempLabel);
        return row;
    }

    /**
     * Row 2: [ wind speed · wind direction ]  [spacer]  [ precip % ]
     *
     * Wind speed and direction are placed adjacent (no spacer between them)
     * on the left half. Precipitation is right-aligned.
     */
    private HBox buildSubRow(Period period) {
        HBox row = new HBox();
        row.getStyleClass().add("period-info2-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(0);

        // Wind group — speed + direction side by side, left-aligned
        String windSpeed = (period.windSpeed != null) ? period.windSpeed : "--";
        String windDir   = (period.windDirection != null) ? period.windDirection : "--";

        Label windLabel = new Label(windSpeed);
        windLabel.getStyleClass().add("period-sub-label");

        Label windDirLabel = new Label("  " + windDir); // small gap between speed and dir
        windDirLabel.getStyleClass().add("period-sub-label");

        HBox windGroup = new HBox(0, windLabel, windDirLabel);
        windGroup.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(windGroup, Priority.ALWAYS);

        // Precip — right-aligned
        String precipStr = (period.probabilityOfPrecipitation != null)
                ? period.probabilityOfPrecipitation.value + "% precip"
                : "0% precip";
        Label precipLabel = new Label(precipStr);
        precipLabel.getStyleClass().addAll("period-sub-label", "precip-label");
        precipLabel.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(windGroup, precipLabel);
        return row;
    }

    private Region buildIcon(Period period) {
        if (period == null) return new Region();
        Region region = SvgIcon.load(IconRouter.getLocalPath(period.icon, period.isDaytime));
        region.getStyleClass().add("period-icon");
        return region;
    }
}