package ui.view;

import models.location.LocationWeather;
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
 *
 * Layout (AnchorPane root):
 *   ┌─── mainContent (VBox, full-width) ───────────────────┐
 *   │  header strip (64px)                                  │
 *   │  scene3-content VBox (16px padding each side)        │
 *   │    pinned card  (~120px)                              │
 *   │    search bar   ( 46px)                               │
 *   │    [gap 12px]                                         │
 *   └───────────────────────────────────────────────────────┘
 *   ┌─── resultsPopupPanel (overlay, AnchorPane anchors) ──┐
 *   │  positioned at top=280, left=18, right=18            │
 *   │  (below: header + top-pad + pinned + spacing + bar)   │
 *   └───────────────────────────────────────────────────────┘
 *
 * Key fixes:
 *  - AnchorPane root (not StackPane) prevents the popup from overflowing
 *    or being clipped; left/right anchors of 18px match scene3-content padding.
 *  - pinnedTempLabel held as a field; refreshPinnedTemp() re-formats in-place
 *    when the unit switch is toggled — no scene rebuild needed.
 *  - Pinned card fires onPinnedClick callback → controller navigates to Chicago.
 *  - Search field clears the popup when emptied.
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
    private LocationWeather pinnedLocation;

    // ---------------------------------------------------------------
    // Public builder
    // ---------------------------------------------------------------

    public Scene build(LocationWeather currentLocation) {
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

        // ── Results popup overlay ────────────────────────────────
        // Offset breakdown (px):
        //   header:       64
        //   content-top:  16  (scene3-content padding)
        //   pinned card: ~120 (badge row + city+temp row + desc + spacing)
        //   gap:          12
        //   search bar:   46
        //   gap:          12
        //   ──────────────
        //   total:       270  (+10 breathing room = 280)
        resultsPopupPanel = buildResultsPopup();
        resultsPopupPanel.setVisible(false);
        resultsPopupPanel.setManaged(false);
        resultsPopupPanel.setMaxHeight(380);

        AnchorPane.setTopAnchor(resultsPopupPanel,  280.0);
        AnchorPane.setLeftAnchor(resultsPopupPanel,  18.0);
        AnchorPane.setRightAnchor(resultsPopupPanel, 18.0);

        root.getChildren().addAll(mainContent, resultsPopupPanel);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    // ---------------------------------------------------------------
    // Callback setters
    // ---------------------------------------------------------------

    public void setOnSearchTextChanged(Consumer<String> c) { this.onSearchTextChanged = c; }
    public void setOnUnitSwitch(Runnable r)                { this.onUnitSwitch = r; }
    public void setOnPinnedClick(Runnable r)               { this.onPinnedClick = r; }

    // ---------------------------------------------------------------
    // Public update methods (called by Scene3Controller)
    // ---------------------------------------------------------------

    /** Replaces result rows with the given list. Shows the popup. */
    public void setResults(List<LocationWeather> locations,
                           Consumer<LocationWeather> onRowClick) {
        if (resultsBox == null) return;
        resultsBox.getChildren().clear();

        if (locations == null || locations.isEmpty()) {
            Label lbl = new Label("No results found.");
            lbl.getStyleClass().add("no-results-label");
            resultsBox.getChildren().add(lbl);
        } else {
            for (LocationWeather lw : locations) {
                LocationResultRow row = new LocationResultRow(lw, () -> {
                    if (onRowClick != null) onRowClick.accept(lw);
                });
                resultsBox.getChildren().add(row);
            }
        }
        showResultsPanel(true);
    }

    /** Shows a "Searching…" placeholder while the network call is in-flight. */
    public void setLoading(boolean loading) {
        if (resultsBox == null || !loading) return;
        resultsBox.getChildren().clear();
        Label lbl = new Label("Searching\u2026");
        lbl.getStyleClass().add("no-results-label");
        resultsBox.getChildren().add(lbl);
        showResultsPanel(true);
    }

    /**
     * Re-formats the pinned card's temperature label in the current unit.
     * Called by Scene3Controller when the unit switch is toggled — no rebuild needed.
     */
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

    // ---------------------------------------------------------------
    // Section builders
    // ---------------------------------------------------------------

    /** Header strip — "Weather" title (left) + unit switch pill (right) */
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

    /** Body — pinned card + search bar */
    private VBox buildBody(LocationWeather loc) {
        VBox body = new VBox(12);
        body.getStyleClass().add("scene3-content");
        body.getChildren().addAll(buildPinnedCard(loc), buildSearchBar());
        return body;
    }

    /**
     * Pinned Chicago card — always visible.
     *
     * Clicking anywhere on the card fires onPinnedClick → Scene3Controller
     * navigates back to the Chicago home Scene 1.
     */
    private VBox buildPinnedCard(LocationWeather loc) {
        VBox card = new VBox(4);
        card.getStyleClass().add("pinned-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        // Row 1: "📍 CURRENT LOCATION" badge + weather icon
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("\uD83D\uDCCD CURRENT LOCATION");
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

    /** Search bar — magnifier icon + text field */
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

    /**
     * Results popup — dark glass container, hidden scrollbar.
     * Width is controlled by AnchorPane left/right anchors set in build().
     */
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

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private String unitSwitchText() {
        return TempConverter.getUnit() == utils.TempConverter.Unit.FAHRENHEIT ? "\u00B0C" : "\u00B0F";
    }
}