package models.hourlyForecast;

import weather.Geometry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Modified copy of the weather.root class to hold hourly properties instead.
@JsonIgnoreProperties(ignoreUnknown = true)
public class HourlyRoot {
    public String type;
    public Geometry geometry;
    public HourlyProperties properties;
}
