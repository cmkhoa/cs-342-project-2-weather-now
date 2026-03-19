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
 * Updated: now accepts a weatherClass string from Scene1View so Scene 2
 * renders the same background gradient as Scene 1's current weather state.
 */
public class Scene2Controller {

    private final Stage primaryStage;
    private final Scene2View view;
    private Scene scene1Reference;

    // The last weather class applied (stored so it can be reused on rebuild)
    private String lastWeatherClass = "";

    public Scene2Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view = new Scene2View();
    }

    public void setScene1Reference(Scene scene1) {
        this.scene1Reference = scene1;
    }

    /** Set by Scene1Controller after each build so Scene 2 matches. */
    public void setWeatherClass(String weatherClass) {
        this.lastWeatherClass = weatherClass != null ? weatherClass : "";
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public Scene buildScene(ArrayList<Period> periods12hr) {
        view.setOnBackClick(() -> {
            if (scene1Reference != null) {
                primaryStage.setScene(scene1Reference);
            }
        });

        return view.build(periods12hr, lastWeatherClass);
    }
}