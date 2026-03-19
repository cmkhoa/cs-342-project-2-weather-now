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
    @Override
    protected String buildUrl(String office, int gridX, int gridY) {
        return "https://api.weather.gov/gridpoints/" + office + "/" + gridX + "," + gridY + "/forecast";
    }

    @Override
    protected ArrayList<Period> parseResponse(String json) {
        try {
            Root root = OM.readValue(json, Root.class);
            return root.properties.periods;
        } catch (Exception e) {
            System.err.println("[ForecastService] Parse error: " + e.getMessage());
            return null;
        }
    }
}