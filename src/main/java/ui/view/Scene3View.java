package ui.view;

import models.location.LocationNode;
import ui.component.LocationResultRow;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;
import weather.Period;

import java.util.List;
import java.util.function.Consumer;

/**
 * Scene 3 — City Search / Pinned List.
 */
public class Scene3View {

    // Callbacks set by Scene3Controller
    private Consumer<String> onSearchTextChanged;
    private Runnable         onUnitSwitch;
    private Runnable         onPinnedClick;

    // Live UI references for dynamic updates
    private VBox  resultsBox;
    private VBox  resultsPopupPanel;
    private Label pinnedTempLabel;
    private LocationNode pinnedLocation;

    // Scene 3 buildder
    public Scene build(LocationNode currentLocation) {
        this.pinnedLocation = currentLocation;

        AnchorPane root = new AnchorPane();
        root.getStyleClass().add("scene3-root");
        root.setPrefSize(405, 810);

        // ── Static content ──────────────────────────────────────
        VBox mainContent = new VBox(0);
        mainContent.setStyle("-fx-background-color: transparent;");
        mainContent.setPrefWidth(405);
        mainContent.getChildren().addAll(buildHeader(), buildBody(currentLocation));

        AnchorPane.setTopAnchor(mainContent,    0.0);
        AnchorPane.setLeftAnchor(mainContent,   0.0);
        AnchorPane.setRightAnchor(mainContent,  0.0);

        // setup popup pane for search results
        resultsPopupPanel = buildResultsPopup();
        resultsPopupPanel.setVisible(false);
        resultsPopupPanel.setManaged(false);
        resultsPopupPanel.setMaxHeight(380);

        AnchorPane.setTopAnchor(resultsPopupPanel,  205.5);
        AnchorPane.setLeftAnchor(resultsPopupPanel,  30.0);
        AnchorPane.setRightAnchor(resultsPopupPanel, 30.0);

        root.getChildren().addAll(mainContent, resultsPopupPanel);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    // Quick set functions
    public void setOnSearchTextChanged(Consumer<String> c) { this.onSearchTextChanged = c; }
    public void setOnUnitSwitch(Runnable r)                { this.onUnitSwitch = r; }
    public void setOnPinnedClick(Runnable r)               { this.onPinnedClick = r; }

    //update functions
    //update results
    public void setResults(List<LocationNode> locations,
                           Consumer<LocationNode> onRowClick) {
        if (resultsBox == null) return;
        resultsBox.getChildren().clear();

        if (locations == null || locations.isEmpty()) {
            Label lbl = new Label("No results found.");
            lbl.getStyleClass().add("no-results-label");
            resultsBox.getChildren().add(lbl);
        } else {
            for (LocationNode lw : locations) {
                LocationResultRow row = new LocationResultRow(lw, () -> {
                    if (onRowClick != null) onRowClick.accept(lw);
                });
                resultsBox.getChildren().add(row);
            }
        }
        showResultsPanel(true);
    }

    // loading spiral
    public void setLoading(boolean loading) {
        if (resultsBox == null || !loading) return;
        resultsBox.getChildren().clear();
        Label lbl = new Label("Searching\u2026");
        lbl.getStyleClass().add("no-results-label");
        resultsBox.getChildren().add(lbl);
        showResultsPanel(true);
    }

    //quickly reformat scene 3 to reflect unit change
    public void refreshPinnedTemp() {
        if (pinnedTempLabel == null || pinnedLocation == null) return;
        if (pinnedLocation.periods12hr == null || pinnedLocation.periods12hr.isEmpty()) return;
        pinnedTempLabel.setText(TempConverter.format(pinnedLocation.currentTemp));
    }

    private void showResultsPanel(boolean visible) {
        if (resultsPopupPanel == null) return;
        resultsPopupPanel.setVisible(visible);
        resultsPopupPanel.setManaged(visible);
    }

    //build each section
    private HBox buildHeader() {
        HBox bar = new HBox();
        bar.getStyleClass().addAll("menu-bar", "scene3-header");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Weather");
        title.getStyleClass().add("scene3-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label unitSwitch = new Label(unitSwitchText());
        unitSwitch.getStyleClass().add("unit-switch-label");
        unitSwitch.setOnMouseClicked(e -> {
            TempConverter.toggle();
            unitSwitch.setText(unitSwitchText());
            refreshPinnedTemp();
            if (onUnitSwitch != null) onUnitSwitch.run();
        });

        bar.getChildren().addAll(title, spacer, unitSwitch);
        return bar;
    }

    private VBox buildBody(LocationNode loc) {
        VBox body = new VBox(12);
        body.getStyleClass().add("scene3-content");
        body.getChildren().addAll(buildSearchBar(), buildPinnedCard(loc));
        return body;
    }

    private VBox buildPinnedCard(LocationNode loc) {
        VBox card = new VBox(4);
        card.getStyleClass().add("pinned-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        // Row 1: CURRENT LOCATION badge + weather icon
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("CURRENT LOCATION");
        badge.getStyleClass().add("pinned-card-label");

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        Region iconRegion = new Region();
        if (loc != null && loc.periods12hr != null && !loc.periods12hr.isEmpty()) {
            Period p = loc.periods12hr.get(0);
            iconRegion = SvgIcon.load(IconRouter.getLocalPath(p.icon, p.isDaytime));
        }
        iconRegion.getStyleClass().add("pinned-icon");
        topRow.getChildren().addAll(badge, sp1, iconRegion);

        // Row 2: city name + temperature
        HBox cityRow = new HBox();
        cityRow.setAlignment(Pos.CENTER_LEFT);

        Label cityLabel = new Label(loc != null && loc.displayName != null
                ? loc.displayName : "Chicago, IL");
        cityLabel.getStyleClass().add("pinned-card-city");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        String tempStr = (loc != null && loc.periods12hr != null && !loc.periods12hr.isEmpty())
                ? TempConverter.format(loc.currentTemp) : "--";
        pinnedTempLabel = new Label(tempStr);
        pinnedTempLabel.getStyleClass().add("pinned-card-temp");
        cityRow.getChildren().addAll(cityLabel, sp2, pinnedTempLabel);

        // Row 3: short description
        String desc = "--";
        if (loc != null && loc.periods12hr != null && !loc.periods12hr.isEmpty()) {
            Period p = loc.periods12hr.get(0);
            desc = p.shortForecast != null ? p.shortForecast : "--";
        }
        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("pinned-card-desc");

        card.getChildren().addAll(topRow, cityRow, descLabel);

        // Click → home navigation
        card.setOnMouseClicked(e -> {
            if (onPinnedClick != null) onPinnedClick.run();
        });

        return card;
    }

    // Search bar: magnifier icon + text field
    private HBox buildSearchBar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("search-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\uD83D\uDD0D");
        icon.getStyleClass().add("search-icon");

        TextField field = new TextField();
        field.setPromptText("Search city\u2026");
        field.getStyleClass().add("search-field");
        HBox.setHgrow(field, Priority.ALWAYS);

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String trimmed = newVal.trim();
            if (trimmed.isEmpty()) showResultsPanel(false);
            if (onSearchTextChanged != null) onSearchTextChanged.accept(trimmed);
        });

        bar.getChildren().addAll(icon, field);
        return bar;
    }

    // results popup
    private VBox buildResultsPopup() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("results-popup-panel");

        resultsBox = new VBox(0);
        resultsBox.getStyleClass().add("results-list");

        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(378);
        scroll.getStyleClass().add("results-scroll");

        panel.getChildren().add(scroll);
        return panel;
    }

    // Helper to get the unit
    private String unitSwitchText() {
        return TempConverter.getUnit() == utils.TempConverter.Unit.FAHRENHEIT ? "\u00B0C" : "\u00B0F";
    }
}