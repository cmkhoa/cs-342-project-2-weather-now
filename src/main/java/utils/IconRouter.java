package utils;

/**
 * This class deconstructs the icon url from each period (both 12hr and hourly) to get a pre assigned icon
 * in /weather-icons-main/bom/app/
 * Reference: https://clojurecivitas.org/scittle/weather/weather_nws_integration.html
 *  NWS token(s)    --> BOM file (day / night)
 *  skc             --> 01_sunny / 02_clear_night
 *  few             --> 05_mostly_sunny / 05_mostly_sunny_night
 *  sct             --> 03_partly_cloudy / 03_partly_cloudy_night
 *  bkn             --> 04_cloudy / 04_cloudy_night
 *  ovc             --> 04_cloudy / 04_cloudy_night
 *  rain, ra, shra  --> 12_rain / 12_rain_night
 *  snow, sn        --> 15_snow / 15_snow_night
 *  tsra            --> 16_storms / 16_storms_night
 *  fog, fg         --> 10_fog / 10_fog_night
 *  wind, breezy    --> 09_wind / 09_wind_night
 *  hot             --> 01_sunny
 *  cold, frost     --> 14_frost / 14_frost_night
 */

public class IconRouter {
    // Base URL and fallback icon
    private static final String BASE     = "/weather-icons-main/bom/app/";
    private static final String FALLBACK = BASE + "04_cloudy.gif";
    /**
     * Function takes in icon url and the daytime status from the period
     * @param nwsIconUrl  full URL returned by the NWS API (Period.icon), may be null
     * @param isDaytime   true for day-variant icons
     * @return            classpath-relative resource path string
     */
    public static String getLocalPath(String nwsIconUrl, boolean isDaytime) {
        if (nwsIconUrl == null || nwsIconUrl.isEmpty()) {
            return FALLBACK;
        }
        String token = extractIconToken(nwsIconUrl);
        return resolveToken(token.toLowerCase(), isDaytime);
    }

    /**
     * Function extracts the token from the icon url
     * @param url: url of the icon
     * @return the first token seen
     */
    static String extractIconToken(String url) {
        int q = url.indexOf('?');
        String path = (q >= 0) ? url.substring(0, q) : url;

        int slash = path.lastIndexOf('/');
        String segment = (slash >= 0) ? path.substring(slash + 1) : path;

        int comma = segment.indexOf(',');
        return (comma >= 0) ? segment.substring(0, comma) : segment;
    }


    /**
     * Function checks the token available and returns the path to the icon
     * @param token: the keyword for matching
     * @param day: day or night
     * @return
     */
    private static String resolveToken(String token, boolean day) {
        // Thunderstorm
        if (token.contains("tsra") || token.contains("thunder")) {
            return day ? BASE + "16_storms.gif" : BASE + "16_storms_night.gif";
        }
        // Clear / sunny
        if (token.equals("skc")) {
            return day ? BASE + "01_sunny.gif" : BASE + "02_clear_night.gif";
        }
        // Few clouds
        if (token.contains("few")) {
            return day ? BASE + "05_mostly_sunny.gif" : BASE + "05_mostly_sunny_night.gif";
        }
        // Scattered clouds
        if (token.contains("sct")) {
            return day ? BASE + "03_partly_cloudy.gif" : BASE + "03_partly_cloudy_night.gif";
        }
        // Broken / overcast
        if (token.contains("bkn") || token.contains("ovc")) {
            return day ? BASE + "04_cloudy.gif" : BASE + "04_cloudy_night.gif";
        }
        // Snow
        if (token.contains("snow") || token.equals("sn") || token.contains("blizzard")) {
            return day ? BASE + "15_snow.gif" : BASE + "15_snow_night.gif";
        }
        // Rain / showers
        if (token.contains("rain") || token.contains("shra") || token.contains("shower")) {
            return day ? BASE + "12_rain.gif" : BASE + "12_rain_night.gif";
        }
        // Fog / haze
        if (token.contains("fog") || token.equals("fg") || token.contains("haze")) {
            return day ? BASE + "10_fog.gif" : BASE + "10_fog_night.gif";
        }
        // Wind
        if (token.contains("wind") || token.contains("breezy")) {
            return day ? BASE + "09_wind.gif" : BASE + "09_wind_night.gif";
        }
        // Hot
        if (token.contains("hot")) {
            return BASE + "01_sunny.gif";
        }
        // Cold / frost
        if (token.contains("cold") || token.contains("frost")) {
            return day ? BASE + "14_frost.gif" : BASE + "14_frost_night.gif";
        }

        return FALLBACK;
    }
}
