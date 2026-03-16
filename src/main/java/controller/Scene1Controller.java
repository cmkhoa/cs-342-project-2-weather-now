package controller;

import hourlyForecast.HourlyPeriod;
import view.Scene1View;
import weather.Period;
import weather.WeatherAPI;
import hourlyForecast.MyWeatherAPI;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Fetches all data needed for Scene 1, populates Scene1View,
 * and wires navigation callbacks.
 * Hardcoded location for Phase 1: Chicago (LOT / 77,70)
 * Called by:
 *   JavaFX.java  — on startup and every 30 minutes via Timeline
 */
public class Scene1Controller {

    // Hardcoded Chicago grid for Phase 1
    private static final String REGION = "LOT";
    private static final int    GRID_X = 77;
    private static final int    GRID_Y = 70;

    private final Stage primaryStage;
    private final Scene1View view;
    private Scene2Controller scene2Controller; // set after construction

    public Scene1Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view = new Scene1View();
    }

    public void setScene2Controller(Scene2Controller s2c) {
        this.scene2Controller = s2c;
    }

    // ---------------------------------------------------------------
    // Public API — called by JavaFX.java on startup and refresh
    // ---------------------------------------------------------------

    /**
     * Fetches fresh data from both endpoints and builds + shows Scene 1.
     * This method performs blocking network calls — call from a background
     * thread and use Platform.runLater() for the stage update if needed.
     * (JavaFX.java's Timeline handler manages threading.)
     */
    public Scene buildScene(ArrayList<Period> periods12hr,
                            ArrayList<HourlyPeriod> hourlyPeriods) {

        // Wire navigation callbacks before building
        view.setOnMoreForecastClick(() -> {
            if (scene2Controller != null) {
                Scene s2 = scene2Controller.buildScene(periods12hr);
                primaryStage.setScene(s2);
            }
        });

        view.setOnLocationClick(() -> {
            // TODO Phase 3: navigate to Scene 3 (city search)
            System.out.println("[Scene1Controller] Location button clicked — Scene 3 not yet implemented");
        });

        view.setOnHomeClick(() -> {
            // Home reloads Scene 1 with Chicago hardcode — no-op in Phase 1
            System.out.println("[Scene1Controller] Home button clicked");
        });

        return view.build(periods12hr, hourlyPeriods);
    }

    // ---------------------------------------------------------------
    // Static data fetchers — used by JavaFX.java's refresh cycle
    // ---------------------------------------------------------------

    /**
     * Fetches the 12-hour forecast for Chicago.
     * Returns null on failure; caller must handle gracefully.
     */
    public static ArrayList<Period> fetch12Hr() {
        return WeatherAPI.getForecast(REGION, GRID_X, GRID_Y);
    }

    /**
     * Fetches the hourly forecast for Chicago.
     * Returns null on failure; caller must handle gracefully.
     */
    public static ArrayList<HourlyPeriod> fetchHourly() {
        return MyWeatherAPI.getHourlyForecast(REGION, GRID_X, GRID_Y);
    }
}
