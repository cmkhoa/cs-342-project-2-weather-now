package models.hourlyForecast;

import weather.Period;

// Extension of the period class that stores data from the forecast/hourly endpoint
public class HourlyPeriod extends Period {
    public DewPoint dewpoint;
    public RelativeHumidity relativeHumidity;
}