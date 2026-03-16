package component;



import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import util.IconRouter;
import util.SvgIcon;
import util.TempConverter;
import weather.Period;

/**
 * A single period card in the Scene 2 scrollable forecast list.
 * Figma spec (node 62:162):
 *   Width: full (400px in content area)   Height: 114px
 *   Border: 1px solid black
 *   Padding: 25px
 *   Gap between rows: 10px
 *   Row 1 (info1) — HBox, justify-between:
 *     <Period>  (20px, left)
 *     icon      (30×30, center)
 *     <temp>    (20px, right)
 *   Row 2 (info2) — HBox, 3 equal columns:
 *     <wind>    |  <windir>  |  <precip>
 */
public class PeriodCard extends VBox {



    public PeriodCard(Period period) {
        super();

        getStyleClass().add("period-card");

        getChildren().addAll(
                buildInfo1Row(period),
                buildInfo2Row(period)
        );
    }

    // ---------------------------------------------------------------
    // Row builders
    // ---------------------------------------------------------------

    /**
     * Row 1: [ <Period name>  |  icon  |  <temp> ]
     */
    private HBox buildInfo1Row(Period period) {
        HBox row = new HBox();
        row.getStyleClass().add("period-info1-row");

        // Period name
        Label nameLabel = new Label(period.name != null ? period.name : "--");
        nameLabel.getStyleClass().add("period-name-label");

        // Spacer
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        // Icon
        Region icon = buildIcon(period);

        // Spacer
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // Temperature
        Label tempLabel = new Label(TempConverter.format(period.temperature));
        tempLabel.getStyleClass().add("period-temp-label");

        row.getChildren().addAll(nameLabel, spacerLeft, icon, spacerRight, tempLabel);
        return row;
    }

    /**
     * Row 2: [ <wind speed>  |  <wind direction>  |  <precip %> ]
     * Three equal-width columns matching Figma node 62:167.
     */
    private HBox buildInfo2Row(Period period) {
        HBox row = new HBox();
        row.getStyleClass().add("period-info2-row");

        Label windLabel  = makeSubLabel(formatWind(period));
        Label windirLabel = makeSubLabel(period.windDirection != null ? period.windDirection : "--");
        Label precipLabel = makeSubLabel(formatPrecip(period));

        // Equal width: each takes 1/3 of available space
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
