package ui.controller;

import ui.view.Scene2View;
import weather.Period;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Receives the already-fetched 12-hour period data (from Scene1Controller)
 * and builds + returns Scene 2.
 *
 * Scene 2 does NOT need its own fetch — all data comes from the same
 * WeatherAPI.getForecast() call that powers Scene 1's 3DayInfo section.
 * This avoids a redundant network request on navigation.
 */
public class Scene2Controller {

    private final Stage primaryStage;
    private final Scene2View view;
    private Scene scene1Reference; // set by JavaFX.java after Scene 1 is built

    public Scene2Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view = new Scene2View();
    }

    /** Called by JavaFX.java once Scene 1 is ready, so Back can navigate home. */
    public void setScene1Reference(Scene scene1) {
        this.scene1Reference = scene1;
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Builds and returns Scene 2 populated with the provided 12-hr periods.
     *
     * @param periods12hr The same 14-period list used by Scene 1 — must not be null
     */
    public Scene buildScene(ArrayList<Period> periods12hr) {

        view.setOnBackClick(() -> {
            if (scene1Reference != null) {
                primaryStage.setScene(scene1Reference);
            }
        });

        return view.build(periods12hr);
    }
}
