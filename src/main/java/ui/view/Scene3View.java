package ui.view;

import models.location.LocationWeather;
import ui.component.LocationResultRow;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import util.TempConverter;

import java.util.List;
import java.util.function.Consumer;

/**
 * Builds Scene 3 — City Search / Pinned List.
 *
 * Figma: node 1:279  "Scene 3 - City search/Pinned List"
 * Canvas: 540 × 1080
 *
 * Structure:
 *   VBox (root)
 *   ├── MenuBar     HBox: [ "Weather" title ]  [ Unit Switch button ]
 *   └── content     VBox:
 *       ├── searchbar   HBox: [ 🔍 icon ]  [ TextField ]
 *       └── resultsList ScrollPane > VBox of LocationResultRows
 *
 * Callbacks wired by Scene3Controller:
 *   onSearchTextChanged  — fired (debounced) on every keystroke
 *   onUnitSwitch         — fired when the unit toggle is clicked
 *   onResultClick        — fired with the chosen LocationWeather
 */
public class Scene3View {

    // Callbacks set by Scene3Controller before build()
    private Consumer<String>       onSearchTextChanged;
    private Runnable               onUnitSwitch;

    // Live reference to the results VBox so Scene3Controller can update it
    private VBox resultsBox;

    // ---------------------------------------------------------------
    // Public builder
    // ---------------------------------------------------------------

    /**
     * Builds and returns a fully wired Scene 3.
     * Pass the initial "Current Location" entry so it shows before any search.
     *
     * @param currentLocation  The device/default location — shown as the first row.
     *                         May be null if location is unavailable.
     */
    public Scene build(LocationWeather currentLocation) {
        VBox root = new VBox();
        root.getStyleClass().add("scene3-root");

        root.getChildren().addAll(
                buildMenuBar(),
                buildContent(currentLocation)
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    // ---------------------------------------------------------------
    // Callback setters
    // ---------------------------------------------------------------

    public void setOnSearchTextChanged(Consumer<String> c) { this.onSearchTextChanged = c; }
    public void setOnUnitSwitch(Runnable r)                { this.onUnitSwitch = r; }

    // ---------------------------------------------------------------
    // Called by Scene3Controller to refresh the results list
    // ---------------------------------------------------------------

    /**
     * Replaces the results list content with the provided rows.
     * Always called on the FX thread (via Platform.runLater in the controller).
     *
     * @param locations  Search results — each paired with a click handler
     * @param onRowClick Consumer that receives the chosen LocationWeather
     */
    public void setResults(List<LocationWeather> locations,
                           Consumer<LocationWeather> onRowClick) {
        if (resultsBox == null) return;
        resultsBox.getChildren().clear();

        if (locations == null || locations.isEmpty()) {
            Label noResults = new Label("No results found.");
            noResults.getStyleClass().add("no-results-label");
            resultsBox.getChildren().add(noResults);
            return;
        }

        for (models.location.LocationWeather lw : locations) {
            LocationResultRow row = new LocationResultRow(lw, () -> {
                if (onRowClick != null) onRowClick.accept(lw);
            });
            resultsBox.getChildren().add(row);
        }
    }

    /**
     * Shows a loading indicator while the search is running.
     */
    public void setLoading(boolean loading) {
        if (resultsBox == null) return;
        if (loading) {
            resultsBox.getChildren().clear();
            Label lbl = new Label("Searching…");
            lbl.getStyleClass().add("no-results-label");
            resultsBox.getChildren().add(lbl);
        }
    }

    // ---------------------------------------------------------------
    // Section builders
    // ---------------------------------------------------------------

    /**
     * Menu bar — Figma node 62:150
     * [ "Weather" title (left) ]  [ Unit switch (right) ]
     */
    private HBox buildMenuBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("menu-bar");

        Label titleLabel = new Label("Weather");
        titleLabel.getStyleClass().add("scene3-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Unit switch — shows current unit, toggles on click
        Label unitSwitch = new Label(unitSwitchText());
        unitSwitch.getStyleClass().add("unit-switch-label");
        unitSwitch.setOnMouseClicked(e -> {
            TempConverter.toggle();
            unitSwitch.setText(unitSwitchText());
            if (onUnitSwitch != null) onUnitSwitch.run();
        });

        bar.getChildren().addAll(titleLabel, spacer, unitSwitch);
        return bar;
    }

    /**
     * Content area — Figma node 62:154
     * Search bar + scrollable results list
     */
    private VBox buildContent(LocationWeather currentLocation) {
        VBox content = new VBox();
        content.getStyleClass().add("scene3-content");

        content.getChildren().addAll(
                buildSearchBar(),
                buildResultsList(currentLocation)
        );

        return content;
    }

    /**
     * Search bar — Figma node 68:92
     * Bordered HBox: [ 🔍 placeholder icon ]  [ TextField ]
     */
    private HBox buildSearchBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("search-bar");

        // Search icon placeholder (matches the 30×29 rounded-rect in Figma)
        Label searchIcon = new Label("🔍");
        searchIcon.getStyleClass().add("search-icon");

        TextField searchField = new TextField();
        searchField.setPromptText("Search city…");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Forward text changes to the controller's debounce handler
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (onSearchTextChanged != null) onSearchTextChanged.accept(newVal.trim());
        });

        bar.getChildren().addAll(searchIcon, searchField);
        return bar;
    }

    /**
     * Results list — Figma node 68:99 "Display Results"
     * ScrollPane wrapping a VBox of LocationResultRows.
     * The first row is always "Current Location" when no search is active.
     */
    private ScrollPane buildResultsList(LocationWeather currentLocation) {
        resultsBox = new VBox();
        resultsBox.getStyleClass().add("results-list");

        // Pre-populate with the current/default location row
        if (currentLocation != null) {
            LocationWeather displayLoc = new LocationWeather();
            displayLoc.displayName  = "Current Location";
            displayLoc.periods12hr  = currentLocation.periods12hr;
            displayLoc.currentTemp  = currentLocation.currentTemp;
            displayLoc.hourlyPeriods = currentLocation.hourlyPeriods;
            displayLoc.nwsOffice    = currentLocation.nwsOffice;
            displayLoc.gridX        = currentLocation.gridX;
            displayLoc.gridY        = currentLocation.gridY;

            // Click on "Current Location" row uses the real LocationWeather
            LocationResultRow currentRow = new LocationResultRow(displayLoc, null);
            resultsBox.getChildren().add(currentRow);
        }

        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("results-scroll");

        return scroll;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String unitSwitchText() {
        return TempConverter.getUnit() == util.TempConverter.Unit.FAHRENHEIT
                ? "Switch to °C"
                : "Switch to °F";
    }
}
