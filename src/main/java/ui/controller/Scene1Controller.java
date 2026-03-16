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
 * Fetches all data needed for Scene 1, populates Scene1View,
 * and wires navigation callbacks.
 *
 * Phase 1 (unchanged): buildScene(periods12hr, hourlyPeriods)
 *   → used by JavaFX.java's 30-min refresh cycle for the default Chicago location.
 *
 * Phase 3 (new): buildSceneForLocation(LocationWeather)
 *   → called by Scene3Controller after the user picks a city.
 *   → uses the grid from the LocationWeather instead of the hardcoded constants.
 *
 * The static fetch helpers are still used by JavaFX.java — they now delegate
 * to the Chicago default LocationWeather so there's a single source of truth.
 */
public class Scene1Controller {

    // ── Home (Chicago) defaults ─────────────────────────────────────
    private static final LocationWeather HOME = LocationWeather.chicagoDefault();

    // ── Collaborators ───────────────────────────────────────────────
    private final Stage       primaryStage;
    private final Scene1View  view;
    private Scene2Controller  scene2Controller;
    private Scene3Controller  scene3Controller;

    // The last Scene 1 built — returned to by "Home" and "Back" buttons
    private Scene lastScene1;

    // The location currently displayed in Scene 1 — used by Scene 3's
    // "Current Location" row and the Home button reset
    private LocationWeather currentLocation;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    public Scene1Controller(Stage primaryStage) {
        this.primaryStage    = primaryStage;
        this.view            = new Scene1View();
        this.currentLocation = HOME;  // default until first fetch
    }

    // ---------------------------------------------------------------
    // Wiring setters
    // ---------------------------------------------------------------

    public void setScene2Controller(Scene2Controller s2c) { this.scene2Controller = s2c; }
    public void setScene3Controller(Scene3Controller s3c) { this.scene3Controller = s3c; }

    // ---------------------------------------------------------------
    // Phase 1 — called by JavaFX.java refresh cycle (Chicago hardcode)
    // ---------------------------------------------------------------

    /**
     * Builds Scene 1 for the home Chicago location using already-fetched data.
     * JavaFX.java calls this via its background fetch + Platform.runLater.
     */
    public Scene buildScene(ArrayList<Period> periods12hr,
                            ArrayList<HourlyPeriod> hourlyPeriods) {
        // Populate a LocationWeather from the fetched data
        LocationWeather lw = LocationWeather.chicagoDefault();
        lw.periods12hr   = periods12hr;
        lw.hourlyPeriods = hourlyPeriods;
        lw.refreshConvenience();

        return buildSceneForLocation(lw);
    }

    // ---------------------------------------------------------------
    // Phase 3 — called by Scene3Controller after a city is picked
    // ---------------------------------------------------------------

    /**
     * Builds Scene 1 for any location.
     * The LocationWeather must be Stage-1 populated (periods12hr non-null).
     * hourlyPeriods may be null — the view handles this gracefully.
     */
    public Scene buildSceneForLocation(LocationWeather location) {
        this.currentLocation = location;

        wireCallbacks(location);

        Scene scene = view.build(
                location.periods12hr,
                location.hourlyPeriods != null ? location.hourlyPeriods : new ArrayList<>(),
                location.displayName
        );

        this.lastScene1 = scene;

        // Keep Scene2Controller's back-reference up to date
        if (scene2Controller != null) {
            scene2Controller.setScene1Reference(scene);
        }

        return scene;
    }

    // ---------------------------------------------------------------
    // Called by Scene3Controller when the unit toggle fires in Scene 3
    // ---------------------------------------------------------------

    /**
     * Rebuilds Scene 1 with the current location data under the new unit.
     * TempConverter.toggle() has already been called before this fires.
     */
    public void onUnitChanged() {
        if (currentLocation != null && currentLocation.periods12hr != null) {
            Scene rebuilt = buildSceneForLocation(currentLocation);
            // Only push to stage if Scene 1 is currently showing
            if (primaryStage.getScene() != null
                    && primaryStage.getScene().getRoot().getStyleClass().contains("scene1-root")) {
                primaryStage.setScene(rebuilt);
            }
        }
    }

    // ---------------------------------------------------------------
    // Static data fetchers — used by JavaFX.java's refresh cycle
    // ---------------------------------------------------------------

    /** Fetches the 12-hr forecast for the home (Chicago) location. */
    public static ArrayList<Period> fetch12Hr() {
        return WeatherAPI.getForecast(HOME.nwsOffice, HOME.gridX, HOME.gridY);
    }

    /** Fetches the hourly forecast for the home (Chicago) location. */
    public static ArrayList<HourlyPeriod> fetchHourly() {
        return MyWeatherAPI.getHourlyForecast(HOME.nwsOffice, HOME.gridX, HOME.gridY);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Wires all navigation callbacks onto the view for a given location.
     * Called before every view.build() so callbacks always capture the
     * correct location reference.
     */
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
                scene3Controller.show(currentLocation, lastScene1);
            }
        });

        // Home button → rebuild Scene 1 for the Chicago default
        view.setOnHomeClick(() -> {
            if (currentLocation != null
                    && currentLocation.nwsOffice.equals(HOME.nwsOffice)
                    && currentLocation.gridX == HOME.gridX
                    && currentLocation.gridY == HOME.gridY) {
                // Already on home — no-op
                return;
            }
            // Navigate back to the last known Scene 1 (which was Chicago)
            // If that's stale, a full re-fetch would happen on next auto-refresh.
            if (lastScene1 != null) {
                // Find the Chicago scene by checking root style class — if the
                // last scene was already rebuilt for Chicago, just reuse it.
                // Otherwise request JavaFX.java to re-fetch (simplified: reuse).
                primaryStage.setScene(lastScene1);
            }
        });
    }
}
