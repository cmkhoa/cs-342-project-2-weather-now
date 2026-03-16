package view;

import component.PeriodCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import weather.Period;

import java.util.ArrayList;

/**
 * Builds the full Scene 2 layout from 12-hour period data.
 * Figma: node 1:277  "Scene 2 - Three Day Forecast"
 * Canvas size: 540 × 1080
 * Structure:
 *   VBox (root)
 *   ├── MenuBar    HBox: [ Back button ]  [ "Forecast" title ]
 *   └── content    ScrollPane > VBox of PeriodCards (all 14 periods)
 * Navigation: "Back" button callback wired by Scene2Controller.
 */
public class Scene2View {

    private static final double SCENE_W   = 540;
    private static final double SCENE_H   = 1080;
    private static final double PADDING   = 50;
    private static final double CONTENT_H = 910;

    // Callback wired by Scene2Controller
    private Runnable onBackClick;

    // ---------------------------------------------------------------
    // Public builder
    // ---------------------------------------------------------------

    /**
     * Constructs and returns a fully populated Scene 2.
     *
     * @param periods12hr All 14 12-hour periods from WeatherAPI (must not be null)
     */
    public Scene build(ArrayList<Period> periods12hr) {
        VBox root = new VBox(0);
        root.setPrefSize(SCENE_W, SCENE_H);
        root.setPadding(new Insets(PADDING));
        root.getStyleClass().add("scene2-root");

        root.getChildren().addAll(
                buildMenuBar(),
                buildContent(periods12hr)
        );

        Scene scene = new Scene(root, SCENE_W, SCENE_H);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    // ---------------------------------------------------------------
    // Callback setter
    // ---------------------------------------------------------------

    public void setOnBackClick(Runnable r) { this.onBackClick = r; }

    // ---------------------------------------------------------------
    // Section builders
    // ---------------------------------------------------------------

    /**
     * Menu bar — Figma node 62:84
     * [ Back button ]  [ "Forecast" label ]
     */
    private HBox buildMenuBar() {
        HBox bar = new HBox(60); // 60px gap between back btn and title
        bar.setPrefWidth(440);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(20));
        bar.getStyleClass().add("menu-bar");

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> { if (onBackClick != null) onBackClick.run(); });

        Label titleLabel = new Label("Forecast");
        titleLabel.getStyleClass().add("scene2-title");

        bar.getChildren().addAll(backBtn, titleLabel);
        return bar;
    }

    /**
     * Scrollable period list — Figma node 62:90 / 62:149
     * VBox of PeriodCard components, one per period.
     */
    private ScrollPane buildContent(ArrayList<Period> periods12hr) {
        VBox periodsBox = new VBox(10);
        periodsBox.setPadding(new Insets(0, 20, 0, 20));
        periodsBox.setMaxWidth(Double.MAX_VALUE);
        periodsBox.getStyleClass().add("periods-list");

        for (Period p : periods12hr) {
            PeriodCard card = new PeriodCard(p);
            card.setMaxWidth(Double.MAX_VALUE);
            periodsBox.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(periodsBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(CONTENT_H);
        scrollPane.getStyleClass().add("scene2-scroll");

        return scrollPane;
    }
}
