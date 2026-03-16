import controller.Scene1Controller;
import controller.Scene2Controller;
import hourlyForecast.HourlyPeriod;
import weather.Period;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;

/**
 * Application entry point and scene lifecycle manager.
 * Responsibilities:
 *   1. Launch the JavaFX application
 *   2. Perform initial data fetch on a background thread
 *   3. Build Scene 1 and Scene 2 on the FX thread
 *   4. Schedule a 30-minute refresh Timeline
 *   5. Own the Stage reference — all scene switches go through here
 * Thread model:
 *   - Network calls run on a daemon Thread (NOT the FX thread)
 *   - All Stage/Scene mutations are wrapped in Platform.runLater()
 *   - The Timeline fires on the FX thread and spawns a new background thread
 */
public class JavaFX extends Application {

	private static final String APP_TITLE    = "CS 342 Project 2: Weather App";
	private static final double SCENE_W      = 540;
	private static final double SCENE_H      = 1080;
	private static final double REFRESH_MINS = 30;

	private Stage primaryStage;
	private Scene currentScene1;
	private boolean isFirstLoad = true;
	// Controllers are created once; their views are rebuilt on each refresh
	private Scene1Controller scene1Controller;
	private Scene2Controller scene2Controller;

	// ---------------------------------------------------------------
	// Application lifecycle
	// ---------------------------------------------------------------

	public static void main(String[] args) {	launch(args);	}

	@Override
	public void start(Stage stage) {
		this.primaryStage = stage;
		stage.setTitle(APP_TITLE);
		stage.setResizable(false);

		// Wire controllers
		scene1Controller = new Scene1Controller(stage);
		scene2Controller = new Scene2Controller(stage);
		scene1Controller.setScene2Controller(scene2Controller);

		// Show a blank placeholder while data loads
		stage.setScene(new Scene(new javafx.scene.layout.StackPane(), SCENE_W, SCENE_H));
		stage.show();

		// Initial fetch + build on a background thread
		fetchAndRefresh();

		// 30-minute auto-refresh Timeline (fires on FX thread)
		Timeline refreshTimeline = new Timeline(
				new KeyFrame(Duration.minutes(REFRESH_MINS), event -> fetchAndRefresh())
		);
		refreshTimeline.setCycleCount(Timeline.INDEFINITE);
		refreshTimeline.play();
	}

	// ---------------------------------------------------------------
	// Fetch + rebuild cycle
	// ---------------------------------------------------------------

	/**
	 * Spawns a background thread to fetch both forecasts,
	 * then rebuilds Scene 1 and Scene 2 on the FX thread.
	 * If either fetch fails, the app retains the previous scenes
	 * (or the placeholder on first load) and prints an error.
	 */
	private void fetchAndRefresh() {
		Thread fetchThread = new Thread(() -> {
			System.out.println("[JavaFX] Fetching forecast data...");

			ArrayList<Period> periods12hr = Scene1Controller.fetch12Hr();
			ArrayList<HourlyPeriod> hourlyPeriods = Scene1Controller.fetchHourly();

			if (periods12hr == null) {
				System.err.println("[JavaFX] 12-hr forecast fetch failed — skipping refresh");
				return;
			}
			if (hourlyPeriods == null) {
				System.err.println("[JavaFX] Hourly forecast fetch failed — skipping refresh");
				return;
			}

			System.out.println("[JavaFX] Fetch complete — rebuilding scenes on FX thread");

			// All UI work must happen on the JavaFX Application Thread
			Platform.runLater(() -> rebuildScenes(periods12hr, hourlyPeriods));
		});

		fetchThread.setDaemon(true); // don't block JVM shutdown
		fetchThread.start();
	}

	/**
	 * Rebuilds both scenes from fresh data and updates the stage if currently
	 * showing Scene 1. Must be called on the FX thread.
	 */

	private void rebuildScenes(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods) {
		// 1. Build the scene and save the reference
		currentScene1 = scene1Controller.buildScene(periods12hr, hourlyPeriods);
		scene2Controller.setScene1Reference(currentScene1);

		// 2. Determine if we should actually update the screen
		Scene currentlyShowing = primaryStage.getScene();

		// We update if:
		// - It's the first time the app has ever loaded data
		// - OR the user is currently looking at Scene 1
		if (isFirstLoad || isScene1(currentlyShowing)) {
			System.out.println("[JavaFX] Updating Stage to Scene 1...");
			primaryStage.setScene(currentScene1);
			isFirstLoad = false;
		} else {
			// This happens during the 30-minute auto-refresh if the user is on Scene 2
			System.out.println("[JavaFX] Data refreshed in background; keeping current view.");
		}

		System.out.println("[JavaFX] Scenes rebuilt successfully");
	}

	/**
	 * Heuristic to determine if the currently showing scene is Scene 1.
	 * Checks style class on root node. Views tag their roots with scene-specific classes.
	 */
	private boolean isScene1(Scene scene) {
		if (scene.getRoot() == null) return true; // placeholder → treat as scene 1
		return scene.getRoot().getStyleClass().contains("scene1-root");
	}
}
