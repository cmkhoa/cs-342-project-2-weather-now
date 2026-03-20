package ui.view;

import javafx.scene.Scene;
import models.hourlyForecast.HourlyPeriod;
import models.location.LocationNode;
import weather.Period;

import java.util.ArrayList;

// concrete factory for scene 2
public class Scene2Factory implements SceneFactory {
    private final Scene2View view;
    public Scene2Factory(Scene2View view) {this.view = view;}

    // scene 2 only uses the 12 hour periods and the weather condition presented in scene 1 for the background, the rest are ignored
    @Override
    public Scene create(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods,
                        String locationName, String weatherClass, LocationNode pinnedLocation) {
        return view.build(periods12hr, weatherClass);
    }
}