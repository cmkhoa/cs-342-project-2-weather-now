package ui.controller;

import models.location.LocationWeather;
import services.GeocodingService;
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
 * Navigation contract:
 *  - Pinned Chicago card click  → navigateToHome() → scene1Controller.getHomeScene1()
 *  - Search result row click    → navigateToScene1(selected city)
 *  - Unit switch toggle         → refreshes pinned card temp + last results display
 *                                 + notifies Scene1Controller to rebuild if Scene 1 showing
 *
 * Scene 3 is rebuilt on every show() call so the pinned card reflects
 * the current unit and the latest Chicago data.
 */
public class Scene3Controller {

    private static final int DEBOUNCE_MS = 400;

    private final Stage      primaryStage;
    private final Scene3View view;

    private Scene1Controller scene1Controller;

    // The active Scene 3 scene object
    private Scene scene3;

    // Debounce timer for the search field
    private Timeline debounce;

    // Cached last search results for unit-toggle refresh
    private List<LocationWeather> lastResults;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    public Scene3Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view         = new Scene3View();
    }

    public void setScene1Controller(Scene1Controller s1c) {
        this.scene1Controller = s1c;
    }

    // ---------------------------------------------------------------
    // Public API — called by Scene1Controller's location button
    // ---------------------------------------------------------------

    /**
     * Builds and shows Scene 3.
     *
     * @param currentLocation  The location currently shown in Scene 1 (used as pinned card data).
     *                         If non-Chicago, the pinned card still shows Chicago data if available.
     * @param returnScene      Unused directly — home navigation goes through scene1Controller.getHomeScene1().
     */
    public void show(LocationWeather currentLocation, Scene returnScene) {
        // Determine what to show on the pinned Chicago card.
        // Prefer the home location's data from Scene1Controller when available.
        LocationWeather pinnedData = getPinnedLocationData(currentLocation);

        buildScene(pinnedData);
        primaryStage.setScene(scene3);
    }

    // ---------------------------------------------------------------
    // Scene construction
    // ---------------------------------------------------------------

    private void buildScene(LocationWeather pinnedData) {
        // Clear any stale search results from a previous visit
        lastResults = null;

        // Wire all callbacks before building the view
        view.setOnSearchTextChanged(this::handleSearchInput);

        view.setOnUnitSwitch(() -> {
            // Refresh pinned card label (in-place, no rebuild needed)
            view.refreshPinnedTemp();

            // Refresh search results list with new units
            if (lastResults != null) {
                view.setResults(lastResults, this::onRowTapped);
            }

            // If Scene 1 is currently behind Scene 3, rebuild it with new units too
            if (scene1Controller != null) {
                scene1Controller.onUnitChanged();
            }
        });

        // Pinned card click → go to the Chicago home screen
        view.setOnPinnedClick(this::navigateToHome);

        scene3 = view.build(pinnedData);
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    /**
     * Navigates to the Chicago home Scene 1.
     * Uses scene1Controller.getHomeScene1() which is the canonical last-built
     * Chicago scene (updated every 30-minute refresh cycle).
     */
    private void navigateToHome() {
        if (scene1Controller == null) return;

        Scene home = scene1Controller.getHomeScene1();
        if (home != null) {
            primaryStage.setScene(home);
        } else {
            // Fallback: rebuild Scene 1 for Chicago from scratch
            LocationWeather chicago = LocationWeather.chicagoDefault();
            Scene s1 = scene1Controller.buildSceneForLocation(chicago);
            primaryStage.setScene(s1);
        }
    }

    /**
     * Navigates to Scene 1 for a selected search result city.
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

    // ---------------------------------------------------------------
    // Search debounce + background fetch
    // ---------------------------------------------------------------

    private void handleSearchInput(String query) {
        if (debounce != null) debounce.stop();
        if (query == null || query.isEmpty()) return;

        debounce = new Timeline(new KeyFrame(
                Duration.millis(DEBOUNCE_MS),
                e -> performSearch(query)
        ));
        debounce.setCycleCount(1);
        debounce.play();
    }

    private void performSearch(String query) {
        view.setLoading(true);

        Thread t = new Thread(() -> {
            System.out.println("[Scene3Controller] Searching: " + query);
            List<LocationWeather> results = GeocodingService.searchByCity(query);
            System.out.println("[Scene3Controller] Results: " + results.size());
            this.lastResults = results;

            Platform.runLater(() ->
                    view.setResults(results, this::onRowTapped)
            );
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Row tap — Stage-2 hourly load + navigate
    // ---------------------------------------------------------------

    private void onRowTapped(LocationWeather selected) {
        System.out.println("[Scene3Controller] Row tapped: " + selected.displayName);

        if (selected.isFullyLoaded()) {
            navigateToScene1(selected);
            return;
        }

        Thread t = new Thread(() -> {
            System.out.println("[Scene3Controller] Fetching hourly for: " + selected.displayName);
            selected.hourlyPeriods = MyWeatherAPI
                    .getHourlyForecast(selected.nwsOffice, selected.gridX, selected.gridY);

            if (selected.hourlyPeriods == null) {
                System.err.println("[Scene3Controller] Hourly fetch failed: " + selected.displayName);
            }
            Platform.runLater(() -> navigateToScene1(selected));
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Returns the best available LocationWeather for the pinned Chicago card.
     * Prefers data from scene1Controller's home location if it's Chicago,
     * otherwise falls back to the passed-in currentLocation or a stub.
     */
    private LocationWeather getPinnedLocationData(LocationWeather currentLocation) {
        if (scene1Controller != null) {
            LocationWeather sc1Loc = scene1Controller.getCurrentLocation();
            if (sc1Loc != null && isChicago(sc1Loc)) {
                return sc1Loc;
            }
        }
        // If the caller is showing Chicago, use that data
        if (currentLocation != null && isChicago(currentLocation)) {
            return currentLocation;
        }
        // Last resort: stub with no weather data (pinned card shows "--")
        LocationWeather stub = LocationWeather.chicagoDefault();
        return stub;
    }

    private boolean isChicago(LocationWeather loc) {
        return loc != null
                && "LOT".equals(loc.nwsOffice)
                && loc.gridX == 77
                && loc.gridY == 70;
    }
}