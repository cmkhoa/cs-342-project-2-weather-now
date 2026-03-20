package ui.view;

import javafx.scene.Scene;
import models.hourlyForecast.HourlyPeriod;
import models.location.LocationNode;
import weather.Period;

import java.util.ArrayList;

// concrete factory for scene 3
public class Scene3Factory implements SceneFactory {
    private final Scene3View view;
    public Scene3Factory(Scene3View view) {this.view = view;}

    // scene 3 only uses the node of the pinned location, the rest are ignored
    @Override
    public Scene create(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods,
                        String locationName, String weatherClass, LocationNode pinnedLocation) {
        return view.build(pinnedLocation);
    }
}