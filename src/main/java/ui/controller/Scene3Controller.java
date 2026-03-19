package ui.controller;

import models.location.LocationNode;
import services.GeocodingService;
import services.HourlyForecastService;
import ui.view.Scene3Factory;
import ui.view.Scene3View;
import ui.view.SceneFactory;

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
 * Design patterns in use:
 *
 *   Template Method — hourly data for tapped search results is now fetched
 *   via HourlyForecastService (an AbstractForecastService subclass) rather
 *   than calling MyWeatherAPI directly.
 *
 *   Abstract Factory — scene3 is built via sceneFactory.create() rather
 *   than view.build() directly.
 */
public class Scene3Controller {

    private static final int DEBOUNCE_MS = 400;

    // Template Method service — replaces MyWeatherAPI.getHourlyForecast()
    private static final HourlyForecastService hourlyForecastService = new HourlyForecastService();

    private final Stage      primaryStage;
    private final Scene3View view;

    private Scene1Controller scene1Controller;
    private SceneFactory     sceneFactory;    // Abstract Factory

    private Scene                 scene3;
    private Timeline              debounce;
    private List<LocationNode> lastResults;

    public Scene3Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view         = new Scene3View();
    }

    public void setScene1Controller(Scene1Controller s1c) {
        this.scene1Controller = s1c;
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public void show(LocationNode currentLocation, Scene returnScene) {
        LocationNode pinnedData = getPinnedLocationData(currentLocation);
        buildScene(pinnedData);
        primaryStage.setScene(scene3);
    }

    // ---------------------------------------------------------------
    // Scene construction
    // ---------------------------------------------------------------

    private void buildScene(LocationNode pinnedData) {
        lastResults  = null;
        sceneFactory = new Scene3Factory(view);  // Abstract Factory wired here

        view.setOnSearchTextChanged(this::handleSearchInput);

        view.setOnUnitSwitch(() -> {
            view.refreshPinnedTemp();
            if (lastResults != null) view.setResults(lastResults, this::onRowTapped);
            if (scene1Controller != null) scene1Controller.onUnitChanged();
        });

        view.setOnPinnedClick(this::navigateToHome);

        // Abstract Factory — interface call, not view.build() directly
        scene3 = sceneFactory.create(null, null, null, null, pinnedData);
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    private void navigateToHome() {
        if (scene1Controller == null) return;
        Scene home = scene1Controller.getHomeScene1();
        if (home != null) {
            primaryStage.setScene(home);
        } else {
            Scene s1 = scene1Controller.buildSceneForLocation(LocationNode.chicagoDefault());
            primaryStage.setScene(s1);
        }
    }

    private void navigateToScene1(LocationNode location) {
        if (scene1Controller == null) return;
        primaryStage.setScene(scene1Controller.buildSceneForLocation(location));
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    private void handleSearchInput(String query) {
        if (debounce != null) debounce.stop();
        if (query == null || query.isEmpty()) return;

        debounce = new Timeline(new KeyFrame(
                Duration.millis(DEBOUNCE_MS), e -> performSearch(query)));
        debounce.setCycleCount(1);
        debounce.play();
    }

    private void performSearch(String query) {
        view.setLoading(true);
        Thread t = new Thread(() -> {
            List<LocationNode> results = GeocodingService.searchByCity(query);
            this.lastResults = results;
            Platform.runLater(() -> view.setResults(results, this::onRowTapped));
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Row tap — Template Method fetches hourly data
    // ---------------------------------------------------------------

    private void onRowTapped(LocationNode selected) {
        if (selected.isFullyLoaded()) { navigateToScene1(selected); return; }

        Thread t = new Thread(() -> {
            // Template Method — HourlyForecastService.fetch() runs the skeleton:
            // buildUrl() → httpGet() → parseResponse()
            selected.hourlyPeriods = hourlyForecastService
                    .fetch(selected.nwsOffice, selected.gridX, selected.gridY);
            Platform.runLater(() -> navigateToScene1(selected));
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private LocationNode getPinnedLocationData(LocationNode currentLocation) {
        if (scene1Controller != null) {
            LocationNode loc = scene1Controller.getCurrentLocation();
            if (loc != null && isChicago(loc)) return loc;
        }
        if (currentLocation != null && isChicago(currentLocation)) return currentLocation;
        return LocationNode.chicagoDefault();
    }

    private boolean isChicago(LocationNode loc) {
        return loc != null && "LOT".equals(loc.nwsOffice)
                && loc.gridX == 77 && loc.gridY == 70;
    }
}