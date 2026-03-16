package ui.controller;

import models.location.LocationWeather;
import service.GeocodingService;
import models.hourlyForecast.MyWeatherAPI;
import ui.view.Scene3View;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Manages Scene 3 — City Search / Pinned List.
 *
 * Responsibilities:
 *   1. Build and show Scene 3 (navigated to from Scene 1's location button)
 *   2. Debounce search bar input and run geocoding on a background thread
 *   3. On row tap: load Stage-2 hourly data (background thread), then
 *      hand the fully-loaded LocationWeather back to Scene1Controller
 *      and navigate to Scene 1
 *
 * Threading model (same as JavaFX.java):
 *   - Geocoding and hourly fetch run on daemon background threads
 *   - All Stage/Scene mutations wrapped in Platform.runLater()
 *
 * Wired in JavaFX.java:
 *   scene3Controller = new Scene3Controller(stage);
 *   scene1Controller.setScene3Controller(scene3Controller);
 *   scene3Controller.setScene1Controller(scene1Controller);
 */
public class Scene3Controller {

    private static final int DEBOUNCE_MS = 400;

    private final Stage       primaryStage;
    private final Scene3View  view;

    // Back-reference to Scene 1 — set by JavaFX.java after wiring
    private Scene1Controller scene1Controller;

    // The Scene 3 scene object — built once, shown on each navigation
    private Scene scene3;

    // The Scene 1 scene to return to if the user does NOT pick a city
    // (set by Scene1Controller before calling show())
    private Scene callerScene1;

    // Debounce timeline — reset on every keystroke
    private Timeline debounce;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    public Scene3Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view         = new Scene3View();
    }

    // ---------------------------------------------------------------
    // Wiring
    // ---------------------------------------------------------------

    public void setScene1Controller(Scene1Controller s1c) {
        this.scene1Controller = s1c;
    }

    // ---------------------------------------------------------------
    // Public API — called by Scene1Controller's location button
    // ---------------------------------------------------------------

    /**
     * Builds (first call) or reuses Scene 3, then switches the stage to it.
     *
     * @param defaultLocation  The current home location — shown as the
     *                         "Current Location" row before any search.
     * @param returnScene      The Scene 1 to navigate back to if the user
     *                         presses back / picks nothing.
     */
    public void show(LocationWeather defaultLocation, Scene returnScene) {
        this.callerScene1 = returnScene;

        // Lazily build the scene once
        if (scene3 == null) {
            buildScene(defaultLocation);
        }

        primaryStage.setScene(scene3);
    }

    // ---------------------------------------------------------------
    // Scene construction
    // ---------------------------------------------------------------

    private void buildScene(LocationWeather defaultLocation) {
        // Wire callbacks before building the view
        view.setOnSearchTextChanged(this::handleSearchInput);

        view.setOnUnitSwitch(() -> {
            // Unit changed globally via TempConverter.toggle() inside the view.
            // Notify Scene1Controller so it can rebuild Scene 1 with new units
            // if the user navigates back without picking a new city.
            if (scene1Controller != null) {
                scene1Controller.onUnitChanged();
            }
        });

        scene3 = view.build(defaultLocation);
    }

    // ---------------------------------------------------------------
    // Search debounce
    // ---------------------------------------------------------------

    /**
     * Called on every keystroke from the search TextField.
     * Resets the debounce timer — fires the actual search only after
     * DEBOUNCE_MS ms of inactivity.
     */
    private void handleSearchInput(String query) {
        if (debounce != null) debounce.stop();

        if (query == null || query.isEmpty()) {
            // If search is cleared, reset to the default "Current Location" view
            // by doing nothing — the results stay as-is from the last state.
            return;
        }

        debounce = new Timeline(new KeyFrame(
                Duration.millis(DEBOUNCE_MS),
                e -> performSearch(query)
        ));
        debounce.setCycleCount(1);
        debounce.play();
    }

    // ---------------------------------------------------------------
    // Background search
    // ---------------------------------------------------------------

    /**
     * Fires on the FX thread after the debounce delay.
     * Shows a loading state immediately, then spawns a background thread
     * to call GeocodingService and update the results list.
     */
    private void performSearch(String query) {
        view.setLoading(true);

        Thread t = new Thread(() -> {
            System.out.println("[Scene3Controller] Searching: " + query);
            List<LocationWeather> results = GeocodingService.searchByCity(query);
            System.out.println("[Scene3Controller] Results: " + results.size());

            Platform.runLater(() ->
                    view.setResults(results, this::onRowTapped)
            );
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Row tap — Stage-2 load + navigate to Scene 1
    // ---------------------------------------------------------------

    /**
     * Called when the user taps a result row.
     * Loads hourly data on a background thread (Stage 2), then hands
     * the complete LocationWeather to Scene1Controller and navigates.
     */
    private void onRowTapped(LocationWeather selected) {
        System.out.println("[Scene3Controller] Row tapped: " + selected.displayName);

        // If Stage 2 is already loaded (cached), skip the fetch
        if (selected.isFullyLoaded()) {
            navigateToScene1(selected);
            return;
        }

        // Show a brief loading indicator on the stage
        // (Scene 1 will appear as soon as data is ready)
        Thread t = new Thread(() -> {
            System.out.println("[Scene3Controller] Fetching hourly for: " + selected.displayName);

            selected.hourlyPeriods = MyWeatherAPI
                    .getHourlyForecast(selected.nwsOffice, selected.gridX, selected.gridY);

            if (selected.hourlyPeriods == null) {
                System.err.println("[Scene3Controller] Hourly fetch failed for: " + selected.displayName);
                // Proceed with empty hourly — Scene 1 handles null gracefully
            }

            Platform.runLater(() -> navigateToScene1(selected));
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    /**
     * Hands the fully-loaded LocationWeather to Scene1Controller and
     * switches the stage to Scene 1.
     * Must be called on the FX thread.
     */
    private void navigateToScene1(LocationWeather location) {
        if (scene1Controller == null) {
            System.err.println("[Scene3Controller] scene1Controller not wired!");
            return;
        }
        Scene s1 = scene1Controller.buildSceneForLocation(location);
        primaryStage.setScene(s1);
    }
}
