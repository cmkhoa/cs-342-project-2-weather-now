package ui.view;

import javafx.scene.Scene;
import models.hourlyForecast.HourlyPeriod;
import models.location.LocationNode;
import weather.Period;

import java.util.ArrayList;

/**
 * Abstract Factory pattern for homework 4
 * Abstract factory, implemented by factories that produces each scene.
 * The controllers only refer to this class with their information
 */
public interface SceneFactory {
    // each factory create and return a fully formed scene using some of the parameters
    Scene create(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods,
                 String locationName, String weatherClass,  LocationNode pinnedLocation);
}