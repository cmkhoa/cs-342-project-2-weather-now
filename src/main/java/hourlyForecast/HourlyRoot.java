package hourlyForecast;

import weather.Geometry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HourlyRoot {
    public String type;
    public Geometry geometry;
    public HourlyProperties properties;
}
