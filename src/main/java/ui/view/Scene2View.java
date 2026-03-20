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
 */
public class Scene2View {

    private Runnable onBackClick;

    // Public builder
    public Scene build(ArrayList<Period> periods12hr, String weatherClass) {
        VBox root = new VBox();
        root.getStyleClass().add("scene2-root");
        if (weatherClass != null && !weatherClass.isEmpty()) {
            root.getStyleClass().add(weatherClass);
        }

        root.getChildren().addAll(buildMenuBar(), buildContent(periods12hr));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }
    // default with no theme
    public Scene build(ArrayList<Period> periods12hr) { return build(periods12hr, ""); }

    public void setOnBackClick(Runnable r) { this.onBackClick = r; }

    // Section builders
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

    //scrollable list of period cards
    private ScrollPane buildContent(ArrayList<Period> periods12hr) {
        VBox periodsBox = new VBox();
        periodsBox.getStyleClass().add("periods-list");

        if (periods12hr != null) {
            // Show up to 8 periods starting from the current one
            int start = (periods12hr.get(0).isDaytime ? 2 : 1);
            for (int i = start; i < start + 6; i++) {
                periodsBox.getChildren().add(new PeriodCard(periods12hr.get(i)));
            }
        }

        ScrollPane scrollPane = new ScrollPane(periodsBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scene2-scroll");

        return scrollPane;
    }
}