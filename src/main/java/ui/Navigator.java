package ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.location.LocationNode;
import services.ForecastService;
import services.GeocodingService;
import services.HourlyForecastService;
import ui.view.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for the entire app
 * Design patterns for hw4:
 *   Template Method: ForecastService / HourlyForecastService extend AbstractForecastService
 *   Abstract Factory: each scene is built via a SceneFactory (Scene1/2/3Factory)
 */
public class Navigator {
    // debounce for the search function
    private static final int DEBOUNCE_MS = 400;
    // instantiate main stage
    private final Stage stage;
    // instantiate services (to be called with template method)
    private final ForecastService       forecastService = new ForecastService();
    private final HourlyForecastService hourlyService   = new HourlyForecastService();
    // instantiate default scenes
    private LocationNode chicago;
    private Scene chicagoScene1;
    private Timeline debounce;

    public Navigator(Stage stage) { this.stage = stage; }

    // on init, set the default scene fetch starter data for chicago, set the values for the current card and build the scene
    public void init() {
        chicago = LocationNode.chicagoDefault();
        chicago.periods12hr   = forecastService.fetch(chicago.nwsOffice, chicago.gridX, chicago.gridY);
        chicago.hourlyPeriods = hourlyService  .fetch(chicago.nwsOffice, chicago.gridX, chicago.gridY);
        chicago.refreshConvenience();

        chicagoScene1 = buildScene1(chicago);
        stage.setScene(chicagoScene1);
        stage.show();
    }


    // Return to the always-cached Chicago Scene 1.
    private void goHome() { stage.setScene(chicagoScene1); }

    // action to go to scene 1 with the location as a parameter for the corresponding location
    private void goToScene1(LocationNode location) { stage.setScene(buildScene1(location)); }

    // action to go to scene 2 with the location, the scene to return to, and the weather class as parameters
    private void goToScene2(LocationNode location, Scene returnScene, String weatherClass) {
        Scene2View view = new Scene2View();
        view.setOnBackClick(() -> stage.setScene(returnScene));
        // Abstract Factory called, so the navigator function never actually have to adjust to what needs to build
        Scene scene = new Scene2Factory(view).create(location.periods12hr, null, null, weatherClass, null);
        stage.setScene(scene);
    }

    // action to go to scene 3
    private void goToScene3() { stage.setScene(buildScene3()); }


    // BUILDER FUNCTIONS

    // function that actually constructs scene 1
    private Scene buildScene1(LocationNode location) {
        Scene1View view = new Scene1View();
        // reset the scene stack
        Scene[] holder = {null};

        // set action controllers
        view.setOnMoreForecastClick(() -> goToScene2(location, holder[0], view.getDetectedWeatherClass()));
        view.setOnLocationClick(this::goToScene3);
        view.setOnHomeClick(() -> { if (!isChicago(location)) goHome(); }); // home button only when not in chicago

        // Abstract Factory called, so the navigator function never actually have to adjust to what needs to build
        Scene scene = new Scene1Factory(view).create(
                location.periods12hr,
                location.hourlyPeriods != null ? location.hourlyPeriods : new ArrayList<>(),
                location.displayName,
                null,   // weatherClass is detected internally by Scene1View.build()
                null
        );
        // add the current scene to the top of the stack
        holder[0] = scene;
        return scene;
    }

    private Scene buildScene3() {
        Scene3View view = new Scene3View();
        @SuppressWarnings("unchecked")
        // set an empty list of results
        List<LocationNode>[] lastResults = new List[]{null};
        //wire go home button
        view.setOnPinnedClick(this::goHome);
        // wire the unit switch
        view.setOnUnitSwitch(() -> {
            //rebuild the scenes when the unit switch is clicked
            if (lastResults[0] != null)
                view.setResults(lastResults[0], this::loadHourlyAndNavigate);
            chicagoScene1 = buildScene1(chicago);
        });
        // wire the search action when the user input changes (checked every debounce interval)
        view.setOnSearchTextChanged(query -> {
            if (debounce != null) debounce.stop();
            if (query == null || query.isEmpty()) return;
            debounce = new Timeline(new KeyFrame(Duration.millis(DEBOUNCE_MS), e -> {
                view.setLoading(true);
                Thread t = new Thread(() -> {
                    List<LocationNode> results = GeocodingService.searchByCity(query);
                    lastResults[0] = results;
                    Platform.runLater(() ->
                            view.setResults(results, this::loadHourlyAndNavigate));
                });
                t.setDaemon(true);
                t.start();
            }));
            debounce.setCycleCount(1);
            debounce.play();
        });

        // Abstract Factory — chicago is passed as the pinned card data
        return new Scene3Factory(view).create(null, null, null, null, chicago);
    }

    // use multiple threads to fetch data and then go to a new scene 1 reflecting that when clicked
    private void loadHourlyAndNavigate(LocationNode loc) {
        if (loc.isFullyLoaded()) { goToScene1(loc); return; }
        Thread t = new Thread(() -> {
            loc.hourlyPeriods = hourlyService.fetch(loc.nwsOffice, loc.gridX, loc.gridY);
            Platform.runLater(() -> goToScene1(loc));
        });
        t.setDaemon(true);
        t.start();
    }

    // Helper function that tells if a location node is reflecting chicago
    private boolean isChicago(LocationNode loc) {
        return loc != null && "LOT".equals(loc.nwsOffice)
                && loc.gridX == 77 && loc.gridY == 70;
    }
}