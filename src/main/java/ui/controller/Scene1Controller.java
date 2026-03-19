package ui.controller;

import models.location.LocationWeather;
import models.hourlyForecast.HourlyPeriod;
import models.hourlyForecast.MyWeatherAPI;
import ui.view.Scene1View;
import weather.Period;
import weather.WeatherAPI;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Fetches all data for Scene 1, populates Scene1View, wires navigation callbacks.
 *
 * Key fix: homeScene1 tracks the LAST CHICAGO scene separately from lastScene1
 * (which is always the most-recently-built scene, possibly for a non-home city).
 * The Home button always goes back to homeScene1, not lastScene1.
 */
public class Scene1Controller {

    private static final LocationWeather HOME = LocationWeather.chicagoDefault();

    private final Stage       primaryStage;
    private final Scene1View  view;
    private Scene2Controller  scene2Controller;
    private Scene3Controller  scene3Controller;

    /** Always the most recently built Scene 1 (any city). */
    private Scene lastScene1;

    /**
     * The last Scene 1 built for the Chicago home location.
     * Only updated when buildScene() or buildSceneForLocation(Chicago) runs.
     * The Home button navigates here.
     */
    private Scene homeScene1;

    private LocationWeather currentLocation;

    public Scene1Controller(Stage primaryStage) {
        this.primaryStage    = primaryStage;
        this.view            = new Scene1View();
        this.currentLocation = HOME;
    }

    public void setScene2Controller(Scene2Controller s2c) { this.scene2Controller = s2c; }
    public void setScene3Controller(Scene3Controller s3c) { this.scene3Controller = s3c; }

    // ---------------------------------------------------------------
    // Phase 1 — called by JavaFX.java 30-minute refresh cycle (Chicago)
    // ---------------------------------------------------------------

    public Scene buildScene(ArrayList<Period> periods12hr,
                            ArrayList<HourlyPeriod> hourlyPeriods) {
        LocationWeather lw = LocationWeather.chicagoDefault();
        lw.periods12hr   = periods12hr;
        lw.hourlyPeriods = hourlyPeriods;
        lw.refreshConvenience();

        Scene scene = buildSceneForLocation(lw);
        // This came from the auto-refresh for Chicago — store as the canonical home scene
        this.homeScene1 = scene;
        return scene;
    }

    // ---------------------------------------------------------------
    // Phase 3 — called by Scene3Controller after a city is picked
    // ---------------------------------------------------------------

    public Scene buildSceneForLocation(LocationWeather location) {
        this.currentLocation = location;

        wireCallbacks(location);

        Scene scene = view.build(
                location.periods12hr,
                location.hourlyPeriods != null ? location.hourlyPeriods : new ArrayList<>(),
                location.displayName
        );

        // Propagate weather theme to Scene 2
        if (scene2Controller != null) {
            scene2Controller.setWeatherClass(view.getDetectedWeatherClass());
        }

        this.lastScene1 = scene;

        // If this is the home/Chicago location, update the homeScene1 reference too
        if (isHomeLocation(location)) {
            this.homeScene1 = scene;
        }

        if (scene2Controller != null) {
            scene2Controller.setScene1Reference(scene);
        }

        return scene;
    }

    // ---------------------------------------------------------------
    // Unit toggle callback (from Scene 3)
    // ---------------------------------------------------------------

    public void onUnitChanged() {
        if (currentLocation != null && currentLocation.periods12hr != null) {
            Scene rebuilt = buildSceneForLocation(currentLocation);
            // Only push to the stage if Scene 1 is currently showing
            Scene showing = primaryStage.getScene();
            if (showing != null
                    && showing.getRoot().getStyleClass().contains("scene1-root")) {
                primaryStage.setScene(rebuilt);
            }
        }
    }

    // ---------------------------------------------------------------
    // Static data fetchers used by JavaFX.java
    // ---------------------------------------------------------------

    public static ArrayList<Period> fetch12Hr() {
        return WeatherAPI.getForecast(HOME.nwsOffice, HOME.gridX, HOME.gridY);
    }

    public static ArrayList<HourlyPeriod> fetchHourly() {
        return MyWeatherAPI.getHourlyForecast(HOME.nwsOffice, HOME.gridX, HOME.gridY);
    }

    // ---------------------------------------------------------------
    // Accessors used by Scene3Controller
    // ---------------------------------------------------------------

    public LocationWeather getCurrentLocation() { return currentLocation; }
    public Scene            getLastScene1()     { return lastScene1; }
    public Scene            getHomeScene1()     { return homeScene1; }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private boolean isHomeLocation(LocationWeather loc) {
        return loc != null
                && loc.nwsOffice.equals(HOME.nwsOffice)
                && loc.gridX == HOME.gridX
                && loc.gridY == HOME.gridY;
    }

    private void wireCallbacks(LocationWeather location) {

        // "More >" → Scene 2
        view.setOnMoreForecastClick(() -> {
            if (scene2Controller != null && location.periods12hr != null) {
                Scene s2 = scene2Controller.buildScene(location.periods12hr);
                primaryStage.setScene(s2);
            }
        });

        // Location button → Scene 3
        view.setOnLocationClick(() -> {
            if (scene3Controller != null) {
                // Always pass the HOME location data for the pinned card in Scene 3
                LocationWeather pinnedData = isHomeLocation(currentLocation)
                        ? currentLocation
                        : HOME; // fallback if home hasn't been fetched yet
                scene3Controller.show(currentLocation, lastScene1);
            }
        });

        // Home button → show the last Chicago scene
        view.setOnHomeClick(() -> {
            if (isHomeLocation(currentLocation)) return; // already home, no-op

            if (homeScene1 != null) {
                // We have a previously-built home scene — just show it
                primaryStage.setScene(homeScene1);
                // Update currentLocation to reflect we're back home
                this.currentLocation = HOME;
            } else {
                // No home scene built yet (shouldn't happen in normal flow, but safe fallback)
                System.out.println("[Scene1Controller] No homeScene1 available — fetching...");
                ArrayList<Period> p12 = fetch12Hr();
                ArrayList<HourlyPeriod> ph = fetchHourly();
                if (p12 != null && ph != null) {
                    Scene s = buildScene(p12, ph);
                    primaryStage.setScene(s);
                }
            }
        });
    }
}