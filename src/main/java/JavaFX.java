import ui.controller.Scene1Controller;
import ui.controller.Scene2Controller;
import ui.controller.Scene3Controller;
import models.hourlyForecast.HourlyPeriod;
import weather.Period;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;

/**
 * Application entry point
 * Responsibilities:
 *   1. Launch the JavaFX application
 *   2. Wire the three scene controllers to each other
 *   3. Perform the initial data fetch
 *   4. Show the stage once data is ready
 *   5. Schedule a 30-minute auto-refresh Timeline
 */
public class JavaFX extends Application {
	private static final String APP_TITLE    = "CS 342 Project 2: Weather App";
	private static final double REFRESH_MINS = 30;   // auto-refresh interval

	private Stage primaryStage;
    private boolean isFirstLoad = true;  // used to push Scene 1 to stage on startup

	// Controllers are created once at startup; their views are rebuilt on each refresh
	private Scene1Controller scene1Controller;
	private Scene2Controller scene2Controller;
	private Scene3Controller scene3Controller;

	public static void main(String[] args) { launch(args); }

	@Override
	public void start(Stage stage) {
		this.primaryStage = stage;
		stage.setTitle(APP_TITLE);
		stage.setResizable(false);

		// Each controller owns one view and is reused across refreshes.
		scene1Controller = new Scene1Controller(stage);
		scene2Controller = new Scene2Controller(stage);
		scene3Controller = new Scene3Controller(stage);

		// Scene1 needs Scene2 for the "More >" button and Scene3 for the location button.
		// Scene3 needs Scene1 so it can navigate back after a city is selected.
		scene1Controller.setScene2Controller(scene2Controller);
		scene1Controller.setScene3Controller(scene3Controller);
		scene3Controller.setScene1Controller(scene1Controller);

		// fetch new data and rebuild the scenes
		fetchAndRefresh();

		// Show the stage only after data is ready so the user never sees a blank window
		stage.show();

		// Fires fetchAndRefresh() repeatedly every 30 minutes; INDEFINITE means it never stops.
		Timeline refreshTimeline = new Timeline(
				new KeyFrame(Duration.minutes(REFRESH_MINS), event -> fetchAndRefresh())
		);
		refreshTimeline.setCycleCount(Timeline.INDEFINITE);
		refreshTimeline.play();
	}


	// Fetches the 12-hr and hourly forecasts for the home (Chicago) location,
	private void fetchAndRefresh() {
		System.out.println("[JavaFX] Fetching forecast data...");

		ArrayList<Period>       periods12hr   = Scene1Controller.fetch12Hr();
		ArrayList<HourlyPeriod> hourlyPeriods = Scene1Controller.fetchHourly();

		if (periods12hr == null) {
			System.err.println("[JavaFX] 12-hr forecast fetch failed — skipping refresh");
			return;
		}
		if (hourlyPeriods == null) {
			System.err.println("[JavaFX] Hourly forecast fetch failed — skipping refresh");
			return;
		}

		System.out.println("[JavaFX] Fetch complete — rebuilding scenes");
		rebuildScenes(periods12hr, hourlyPeriods);
	}

	// Rebuild scene 1 when it is being shown and update scene 2's back reference
	private void rebuildScenes(ArrayList<Period> periods12hr,
							   ArrayList<HourlyPeriod> hourlyPeriods) {
        Scene currentScene1 = scene1Controller.buildScene(periods12hr, hourlyPeriods);
		scene2Controller.setScene1Reference(currentScene1);

		Scene currentlyShowing = primaryStage.getScene();

		if (isFirstLoad || isScene1(currentlyShowing)) {
			// Push the new Scene 1 to the stage on first load or during a background refresh while Scene 1 is visible
			System.out.println("[JavaFX] Updating Stage to Scene 1...");
			primaryStage.setScene(currentScene1);
			isFirstLoad = false;
		} else {
			// User is on Scene 2 or 3 — update data silently without interrupting navigation
			System.out.println("[JavaFX] Data refreshed; keeping current view.");
		}

		System.out.println("[JavaFX] Scenes rebuilt successfully");
	}

	//Returns true if the currently displayed scene is Scene 1 (or nothing yet).
	private boolean isScene1(Scene scene) {
		if (scene == null || scene.getRoot() == null) return true;
		return scene.getRoot().getStyleClass().contains("scene1-root");
	}
}