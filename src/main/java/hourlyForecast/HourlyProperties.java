package hourlyForecast;

import weather.Elevation;

import java.util.ArrayList;
import java.util.Date;

public class HourlyProperties {
    public String units;
    public String forecastGenerator;
    public Date generatedAt;
    public Date updateTime;
    public String validTimes;
    public Elevation elevation;
    public ArrayList<HourlyPeriod> periods;
}
