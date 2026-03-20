package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.hourlyForecast.HourlyPeriod;
import models.hourlyForecast.HourlyRoot;

import java.util.ArrayList;

/** Using the abstract class to implement the template method pattern.
 * defining the 2 abstract functions for the hourly periods
 */
public class HourlyForecastService extends AbstractForecastService<HourlyPeriod> {
    private static final ObjectMapper OM = new ObjectMapper();

    // actually define the function buildURL for the hourly periods
    @Override
    protected String buildUrl(String office, int gridX, int gridY) {
        return "https://api.weather.gov/gridpoints/" + office + "/" + gridX + "," + gridY + "/forecast/hourly";
    }

    // actually define the function to parse the json into an array of hourly periods
    @Override
    protected ArrayList<HourlyPeriod> parseResponse(String json) {
        try {
            HourlyRoot root = OM.readValue(json, HourlyRoot.class);
            return root.properties.periods;
        } catch (Exception e) {
            return null;
        }
    }
}