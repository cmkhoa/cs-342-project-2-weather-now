package ui.view;

import javafx.scene.Scene;
import models.hourlyForecast.HourlyPeriod;
import models.location.LocationNode;
import weather.Period;

import java.util.ArrayList;

// Concrete Factory for Scene 1
public class Scene1Factory implements SceneFactory {
    private final Scene1View view;
    public Scene1Factory(Scene1View view) {this.view = view;}

    @Override
    public Scene create(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods,
                        String locationName, String weatherClass, LocationNode pinnedLocation) {
        return view.build(periods12hr, hourlyPeriods, locationName);
    }
}