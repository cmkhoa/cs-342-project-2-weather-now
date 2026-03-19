package ui.controller;

import ui.view.Scene2Factory;
import ui.view.Scene2View;
import ui.view.SceneFactory;
import weather.Period;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Builds Scene 2 from already-fetched 12-hour period data.
 *
 * Abstract Factory — holds a SceneFactory reference, calls sceneFactory.create()
 * rather than view.build() directly. The controller is decoupled from the
 * concrete view class.
 */
public class Scene2Controller {

    private final Stage        primaryStage;
    private final Scene2View   view;
    private final SceneFactory sceneFactory;  // Abstract Factory

    private Scene  scene1Reference;
    private String lastWeatherClass = "";

    public Scene2Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view         = new Scene2View();
        this.sceneFactory = new Scene2Factory(view);  // Abstract Factory wired here
    }

    public void setScene1Reference(Scene scene1)   { this.scene1Reference  = scene1; }
    public void setWeatherClass(String weatherClass) {
        this.lastWeatherClass = weatherClass != null ? weatherClass : "";
    }

    public Scene buildScene(ArrayList<Period> periods12hr) {
        view.setOnBackClick(() -> {
            if (scene1Reference != null) primaryStage.setScene(scene1Reference);
        });

        // Abstract Factory — interface call, not view.build() directly
        return sceneFactory.create(periods12hr, null, null, lastWeatherClass, null);
    }
}