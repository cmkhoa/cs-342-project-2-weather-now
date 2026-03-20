package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import weather.Period;
import weather.Root;

import java.util.ArrayList;

/** Using the abstract class to implement the template method pattern.
 * defining the 2 abstract functions for the 12 hour periods
 */
public class ForecastService extends AbstractForecastService<Period> {
    private static final ObjectMapper OM = new ObjectMapper();

    // actually define the function buildURL for the 12hr periods
    @Override
    protected String buildUrl(String office, int gridX, int gridY) {
        return "https://api.weather.gov/gridpoints/" + office + "/" + gridX + "," + gridY + "/forecast";
    }

    // actually define the function to parse the json into an array of 12hr periods
    @Override
    protected ArrayList<Period> parseResponse(String json) {
        try {
            Root root = OM.readValue(json, Root.class);
            return root.properties.periods;
        } catch (Exception e) {
            return null;
        }
    }
}