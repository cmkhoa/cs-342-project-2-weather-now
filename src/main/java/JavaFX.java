import ui.controller.Scene1Controller;
import ui.controller.Scene2Controller;
import ui.controller.Scene3Controller;
import models.hourlyForecast.HourlyPeriod;
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
 *
 * Responsibilities:
 *   1. Launch the JavaFX application
 *   2. Perform initial data fetch on a background thread
 *   3. Build Scene 1 and Scene 2 on the FX thread
 *   4. Schedule a 30-minute refresh Timeline
 *   5. Own the Stage reference — all scene switches go through here
 *
 * Phase 3 additions:
 *   - Instantiates Scene3Controller
 *   - Wires Scene3Controller ↔ Scene1Controller bidirectionally
 *
 * Thread model:
 *   - Network calls run on a daemon Thread (NOT the FX thread)
 *   - All Stage/Scene mutations are wrapped in Platform.runLater()
 *   - The Timeline fires on the FX thread and spawns a new background thread
 */
public class JavaFX extends Application {

	private static final String APP_TITLE    = "CS 342 Project 2: Weather App";
	private static final double REFRESH_MINS = 30;

	private Stage primaryStage;
	private Scene currentScene1;
	private boolean isFirstLoad = true;

	// Controllers are created once; their views are rebuilt on each refresh
	private Scene1Controller scene1Controller;
	private Scene2Controller scene2Controller;
	private Scene3Controller scene3Controller;  // Phase 3

	// ---------------------------------------------------------------
	// Application lifecycle
	// ---------------------------------------------------------------

	public static void main(String[] args) { launch(args); }

	@Override
	public void start(Stage stage) {
		this.primaryStage = stage;
		stage.setTitle(APP_TITLE);
		stage.setResizable(false);

		// ── Wire controllers ─────────────────────────────────────────
		scene1Controller = new Scene1Controller(stage);
		scene2Controller = new Scene2Controller(stage);
		scene3Controller = new Scene3Controller(stage);   // Phase 3

		// Bidirectional wiring: each controller knows about its neighbours
		scene1Controller.setScene2Controller(scene2Controller);
		scene1Controller.setScene3Controller(scene3Controller);  // Phase 3
		scene3Controller.setScene1Controller(scene1Controller);  // Phase 3

		// ── Placeholder while data loads ─────────────────────────────
		javafx.scene.layout.StackPane placeholderRoot = new javafx.scene.layout.StackPane();
		placeholderRoot.getStyleClass().add("scene1-root");
		Scene placeholderScene = new Scene(placeholderRoot);
		placeholderScene.getStylesheets().add(
				getClass().getResource("/styles.css").toExternalForm());
		stage.setScene(placeholderScene);
		stage.show();

		// ── Initial fetch ────────────────────────────────────────────
		fetchAndRefresh();

		// ── 30-minute auto-refresh ───────────────────────────────────
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
	 * Spawns a background thread to fetch both forecasts for the home
	 * (Chicago) location, then rebuilds Scene 1 and Scene 2 on the FX thread.
	 *
	 * If either fetch fails, the app retains the previous scenes
	 * (or the placeholder on first load) and prints an error.
	 */
	private void fetchAndRefresh() {
		Thread fetchThread = new Thread(() -> {
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

			System.out.println("[JavaFX] Fetch complete — rebuilding scenes on FX thread");
			Platform.runLater(() -> rebuildScenes(periods12hr, hourlyPeriods));
		});

		fetchThread.setDaemon(true);
		fetchThread.start();
	}

	/**
	 * Rebuilds both scenes from fresh data and updates the stage if currently
	 * showing Scene 1. Must be called on the FX thread.
	 */
	private void rebuildScenes(ArrayList<Period> periods12hr,
							   ArrayList<HourlyPeriod> hourlyPeriods) {
		// Build Scene 1 (also updates scene2Controller's back-reference internally)
		currentScene1 = scene1Controller.buildScene(periods12hr, hourlyPeriods);
		scene2Controller.setScene1Reference(currentScene1);

		Scene currentlyShowing = primaryStage.getScene();

		if (isFirstLoad || isScene1(currentlyShowing)) {
			System.out.println("[JavaFX] Updating Stage to Scene 1...");
			primaryStage.setScene(currentScene1);
			isFirstLoad = false;
		} else {
			System.out.println("[JavaFX] Data refreshed in background; keeping current view.");
		}

		System.out.println("[JavaFX] Scenes rebuilt successfully");
	}

	/**
	 * Returns true if the currently showing scene is Scene 1 (or the placeholder).
	 * Checks the root node's style class — each view tags its root distinctly.
	 */
	private boolean isScene1(Scene scene) {
		if (scene == null || scene.getRoot() == null) return true;
		return scene.getRoot().getStyleClass().contains("scene1-root");
	}
}
