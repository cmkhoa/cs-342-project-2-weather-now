package ui.controller;

import models.location.LocationNode;
import models.hourlyForecast.HourlyPeriod;
import services.ForecastService;
import services.HourlyForecastService;
import ui.view.Scene1Factory;
import ui.view.Scene1View;
import ui.view.SceneFactory;
import weather.Period;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Wires Scene 1 data, navigation callbacks, and scene construction.
 *
 * For hw4:
 *      template method: by using forecastService and hourlyforecastservice
 *      abstract factory: the build function calls sceneFactory.create()
 */
public class Scene1Controller {
    private static final LocationNode HOME = LocationNode.chicagoDefault();

    // Template Method services
    private static final ForecastService forecastService = new ForecastService();
    private static final HourlyForecastService hourlyForecastService = new HourlyForecastService();

    private final Stage primaryStage;
    private final Scene1View view;
    private final SceneFactory sceneFactory;  // Abstract Factory

    private Scene2Controller scene2Controller;
    private Scene3Controller scene3Controller;

    // store the most recent version of scene 1, being the previous for back, or home for chicago
    private Scene lastScene1;
    private Scene homeScene1;
    private LocationNode currentLocation;

    public Scene1Controller(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.view = new Scene1View();
        this.sceneFactory = new Scene1Factory(view);
        this.currentLocation = HOME;
    }

    public void setScene2Controller(Scene2Controller s2c) { this.scene2Controller = s2c; }
    public void setScene3Controller(Scene3Controller s3c) { this.scene3Controller = s3c; }

    public Scene buildScene(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods) {
        LocationNode lw = LocationNode.chicagoDefault();
        lw.periods12hr   = periods12hr;
        lw.hourlyPeriods = hourlyPeriods;
        lw.refreshConvenience();

        Scene scene = buildSceneForLocation(lw);
        this.homeScene1 = scene;  // auto-refresh always produces a Chicago scene
        return scene;
    }

    // Called by Scene3Controller after a city is picked
    public Scene buildSceneForLocation(LocationNode location) {
        this.currentLocation = location;
        wireCallbacks(location);

        // Abstract Factory — controller calls the interface, not the view directly
        Scene scene = sceneFactory.create(location.periods12hr,
                location.hourlyPeriods != null ? location.hourlyPeriods : new ArrayList<>(),
                location.displayName,
                view.getDetectedWeatherClass(),
                null
        );

        if (scene2Controller != null) {
            scene2Controller.setWeatherClass(view.getDetectedWeatherClass());
            scene2Controller.setScene1Reference(scene);
        }

        this.lastScene1 = scene;

        if (isHomeLocation(location)) {
            this.homeScene1 = scene;
        }

        return scene;
    }

    // Called by Scene3Controller on unit toggle
    public void onUnitChanged() {
        if (currentLocation == null || currentLocation.periods12hr == null) return;
        Scene rebuilt = buildSceneForLocation(currentLocation);
        Scene showing = primaryStage.getScene();
        if (showing != null && showing.getRoot().getStyleClass().contains("scene1-root")) {
            primaryStage.setScene(rebuilt);
        }
    }

    // Static data fetchers — Template Method pattern
    // Both delegate to AbstractForecastService subclasses.
    public static ArrayList<Period> fetch12Hr() {
        return forecastService.fetch(HOME.nwsOffice, HOME.gridX, HOME.gridY);
    }
    public static ArrayList<HourlyPeriod> fetchHourly() {
        return hourlyForecastService.fetch(HOME.nwsOffice, HOME.gridX, HOME.gridY);
    }

    // Accessors
    public LocationNode getCurrentLocation() { return currentLocation; }
    public Scene getLastScene1()     { return lastScene1; }
    public Scene getHomeScene1()     { return homeScene1; }

    private boolean isHomeLocation(LocationNode loc) {
        return loc != null && loc.nwsOffice.equals(HOME.nwsOffice) && loc.gridX == HOME.gridX && loc.gridY == HOME.gridY;
    }

    private void wireCallbacks(LocationNode location) {

        view.setOnMoreForecastClick(() -> {
            if (scene2Controller != null && location.periods12hr != null) {
                primaryStage.setScene(scene2Controller.buildScene(location.periods12hr));
            }
        });

        view.setOnLocationClick(() -> {
            if (scene3Controller != null) {
                scene3Controller.show(location, lastScene1);
            }
        });

        view.setOnHomeClick(() -> {
            if (isHomeLocation(currentLocation)) return;

            if (homeScene1 != null) {
                primaryStage.setScene(homeScene1);
                this.currentLocation = HOME;
                if (scene2Controller != null) scene2Controller.setScene1Reference(homeScene1);
            } else {
                ArrayList<Period> p12 = fetch12Hr();
                ArrayList<HourlyPeriod> ph = fetchHourly();
                if (p12 != null && ph != null) {
                    primaryStage.setScene(buildScene(p12, ph));
                }
            }
        });
    }
}