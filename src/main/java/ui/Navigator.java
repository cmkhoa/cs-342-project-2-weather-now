package ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.location.LocationNode;
import services.ForecastService;
import services.GeocodingService;
import services.HourlyForecastService;
import ui.view.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Single navigator — replaces Scene1Controller / Scene2Controller / Scene3Controller.
 *
 * Flow:
 *   Scene 1  ──[More]──────►  Scene 2  (same location)
 *   Scene 2  ──[Back]──────►  Scene 1  (same location)
 *   Scene 1  ──[Location]──►  Scene 3
 *   Scene 3  ──[Chicago card]► Chicago Scene 1
 *   Scene 3  ──[Search row]──► that location's Scene 1
 *   Scene 1  ──[Home btn]───►  Chicago Scene 1  (only visible for non-Chicago)
 *
 * Design patterns preserved (required by hw4):
 *   Template Method — ForecastService / HourlyForecastService extend AbstractForecastService
 *   Abstract Factory — each scene is built via a SceneFactory (Scene1/2/3Factory)
 */
public class Navigator {

    private static final int DEBOUNCE_MS = 400;

    private final Stage stage;
    private final ForecastService       forecastService = new ForecastService();
    private final HourlyForecastService hourlyService   = new HourlyForecastService();

    private LocationNode chicago;
    /** Cached so Home / pinned-card navigation is instant. Rebuilt after unit toggle. */
    private Scene chicagoScene1;

    private Timeline debounce;

    public Navigator(Stage stage) {
        this.stage = stage;
    }

    // ------------------------------------------------------------------ startup

    public void init() {
        chicago = LocationNode.chicagoDefault();
        chicago.periods12hr   = forecastService.fetch(chicago.nwsOffice, chicago.gridX, chicago.gridY);
        chicago.hourlyPeriods = hourlyService  .fetch(chicago.nwsOffice, chicago.gridX, chicago.gridY);
        chicago.refreshConvenience();

        chicagoScene1 = buildScene1(chicago);
        stage.setScene(chicagoScene1);
        stage.show();
    }

    // ------------------------------------------------------------------ navigation

    /** Return to the always-cached Chicago Scene 1. */
    private void goHome() {
        stage.setScene(chicagoScene1);
    }

    private void goToScene1(LocationNode location) {
        stage.setScene(buildScene1(location));
    }

    private void goToScene2(LocationNode location, Scene returnScene, String weatherClass) {
        Scene2View view = new Scene2View();
        view.setOnBackClick(() -> stage.setScene(returnScene));
        // Abstract Factory
        Scene scene = new Scene2Factory(view).create(location.periods12hr, null, null, weatherClass, null);
        stage.setScene(scene);
    }

    private void goToScene3() {
        stage.setScene(buildScene3());
    }

    // ------------------------------------------------------------------ builders

    private Scene buildScene1(LocationNode location) {
        Scene1View view = new Scene1View();

        // Array wrapper so the More-lambda can reference the scene after it's built
        Scene[] holder = {null};

        view.setOnMoreForecastClick(() ->
                goToScene2(location, holder[0], view.getDetectedWeatherClass()));

        view.setOnLocationClick(this::goToScene3);

        // Home button only does something when we're not already on Chicago
        view.setOnHomeClick(() -> { if (!isChicago(location)) goHome(); });

        // Abstract Factory
        Scene scene = new Scene1Factory(view).create(
                location.periods12hr,
                location.hourlyPeriods != null ? location.hourlyPeriods : new ArrayList<>(),
                location.displayName,
                null,   // weatherClass is detected internally by Scene1View.build()
                null
        );
        holder[0] = scene;
        return scene;
    }

    private Scene buildScene3() {
        Scene3View view = new Scene3View();

        // Kept so the unit-switch can refresh temp labels in the visible result list
        @SuppressWarnings("unchecked")
        List<LocationNode>[] lastResults = new List[]{null};

        view.setOnPinnedClick(this::goHome);

        view.setOnUnitSwitch(() -> {
            // TempConverter.toggle() + refreshPinnedTemp() are already called inside
            // Scene3View before this callback fires — just update the result rows and
            // rebuild the cached Chicago Scene 1 so Home reflects the new unit.
            if (lastResults[0] != null)
                view.setResults(lastResults[0], this::loadHourlyAndNavigate);
            chicagoScene1 = buildScene1(chicago);
        });

        view.setOnSearchTextChanged(query -> {
            if (debounce != null) debounce.stop();
            if (query == null || query.isEmpty()) return;
            debounce = new Timeline(new KeyFrame(Duration.millis(DEBOUNCE_MS), e -> {
                view.setLoading(true);
                Thread t = new Thread(() -> {
                    List<LocationNode> results = GeocodingService.searchByCity(query);
                    lastResults[0] = results;
                    Platform.runLater(() ->
                            view.setResults(results, this::loadHourlyAndNavigate));
                });
                t.setDaemon(true);
                t.start();
            }));
            debounce.setCycleCount(1);
            debounce.play();
        });

        // Abstract Factory — chicago is passed as the pinned card data
        return new Scene3Factory(view).create(null, null, null, null, chicago);
    }

    // ------------------------------------------------------------------ async fetch + navigate

    /** Fetches hourly data for a search result on a background thread, then navigates. */
    private void loadHourlyAndNavigate(LocationNode loc) {
        if (loc.isFullyLoaded()) { goToScene1(loc); return; }
        Thread t = new Thread(() -> {
            loc.hourlyPeriods = hourlyService.fetch(loc.nwsOffice, loc.gridX, loc.gridY);
            Platform.runLater(() -> goToScene1(loc));
        });
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ helper

    private boolean isChicago(LocationNode loc) {
        return loc != null && "LOT".equals(loc.nwsOffice)
                && loc.gridX == 77 && loc.gridY == 70;
    }
}