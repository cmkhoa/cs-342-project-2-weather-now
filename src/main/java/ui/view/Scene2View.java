package ui.view;

import ui.component.PeriodCard;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import utils.SvgIcon;
import weather.Period;

import java.util.ArrayList;

/**
 * Scene 2 — Detailed Forecast.
 *
 * Fixes:
 *  - Background gradient matches Scene 1's current weather theme.
 *    The Scene2Controller passes the detected weather class string via build(periods, weatherClass).
 *  - Scrollbar completely hidden (same CSS override technique as the hourly strip).
 *  - Period sub-row: wind+dir grouped left, precip right, evenly distributed.
 */
public class Scene2View {

    private Runnable onBackClick;

    // ---------------------------------------------------------------
    // Public builder
    // ---------------------------------------------------------------

    /**
     * Builds Scene 2.
     *
     * @param periods12hr   The 12-hr period list.
     * @param weatherClass  The CSS class string detected by Scene1View (e.g. "weather-sunny").
     *                      Pass empty string for the default gradient.
     */
    public Scene build(ArrayList<Period> periods12hr, String weatherClass) {
        VBox root = new VBox();
        root.getStyleClass().add("scene2-root");
        if (weatherClass != null && !weatherClass.isEmpty()) {
            root.getStyleClass().add(weatherClass);
        }

        root.getChildren().addAll(
                buildMenuBar(),
                buildContent(periods12hr)
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    /** Backwards-compatible overload — no theme. */
    public Scene build(ArrayList<Period> periods12hr) {
        return build(periods12hr, "");
    }

    public void setOnBackClick(Runnable r) { this.onBackClick = r; }

    // ---------------------------------------------------------------
    // Section builders
    // ---------------------------------------------------------------

    private HBox buildMenuBar() {
        HBox bar = new HBox();
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setSpacing(12);
        bar.getStyleClass().add("menu-bar");

        Button backBtn = buildBackButton();
        backBtn.setOnAction(e -> { if (onBackClick != null) onBackClick.run(); });

        Label titleLabel = new Label("Forecast");
        titleLabel.getStyleClass().add("scene2-title");

        bar.getChildren().addAll(backBtn, titleLabel);
        return bar;
    }

    private Button buildBackButton() {
        Region backIcon = SvgIcon.loadTinted("/ui-icons/backButton.svg", 20);
        backIcon.getStyleClass().add("back-icon");
        Button btn = new Button();
        btn.setGraphic(backIcon);
        btn.getStyleClass().add("back-btn");
        return btn;
    }

    /**
     * Scrollable list of period cards.
     * Scrollbar is hidden via CSS — user scrolls with mouse wheel / trackpad.
     */
    private ScrollPane buildContent(ArrayList<Period> periods12hr) {
        VBox periodsBox = new VBox();
        periodsBox.getStyleClass().add("periods-list");

        if (periods12hr != null) {
            // Show up to 8 periods starting from the current one
            int end = Math.min(8, periods12hr.size());
            for (int i = 0; i < end; i++) {
                periodsBox.getChildren().add(new PeriodCard(periods12hr.get(i)));
            }
        }

        ScrollPane scrollPane = new ScrollPane(periodsBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // hidden by CSS too
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scene2-scroll");

        return scrollPane;
    }
}