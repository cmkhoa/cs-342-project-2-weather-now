package models.location;

import models.hourlyForecast.HourlyPeriod;
import weather.Period;

import java.util.ArrayList;

/**
 * Data model for a single location's weather state, does not call the API.
 * Populated in two stages to avoid unnecessary API calls:
 *   Stage 1 (happens on search): uses the weatherAPI
 *   Stage 2 (when the user clicks on the search result): uses the MyWeatherAPI
 * With the default Chicago location set with a function
 */
public class LocationNode {
    public String displayName;
    public double lat, lon;

    // NWS grid
    public String nwsOffice;
    public int gridX, gridY;

    // Stage 1 data
    public ArrayList<Period> periods12hr;    // 14 12-hr periods from WeatherAPI
    public int currentTemp;    // periods12hr.get(0).temperature (convenience)
    public String currentIcon;  // periods12hr.get(0).icon

    // Stage 2 data (null until loaded on demand)
    public ArrayList<HourlyPeriod> hourlyPeriods;

    // Constructors
    public LocationNode() {}
    public LocationNode(String displayName, double lat, double lon, String nwsOffice, int gridX, int gridY){
        this.displayName = displayName;
        this.lat = lat; this.lon = lon;
        this.nwsOffice = nwsOffice;
        this.gridX = gridX; this.gridY = gridY;
    }

    // Function to check when stage 2 data is loaded, checked by Scene1Controller before re-fetch
    public boolean isFullyLoaded() {
        return hourlyPeriods != null && !hourlyPeriods.isEmpty();
    }
    // Function populates the quick access fields for the search result row
    public void refreshConvenience() {
        if (periods12hr != null && !periods12hr.isEmpty()) {
            currentTemp = periods12hr.get(0).temperature;
            currentIcon = periods12hr.get(0).icon;
        }
    }
    // Default home location: set the location for the Chicago line at boot
    public static LocationNode chicagoDefault() {
        return new LocationNode("Chicago, IL", 41.85, -87.65, "LOT", 77, 70);
    }
}
