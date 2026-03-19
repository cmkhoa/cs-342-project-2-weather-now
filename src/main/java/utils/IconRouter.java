package utils;

/**
 * This class deconstructs the icon url from each period (both 12hr and hourly) to get a pre assigned icon
 * in /weather-icons-main/bom/weatherIcons/
 * Reference: https://clojurecivitas.org/scittle/weather/weather_nws_integration.html
 */
public class IconRouter {
    // Base URL and fallback icon
    private static final String BASE     = "/weather-icons-main/bom/weatherIcons/";
    private static final String FALLBACK = BASE + "04_cloudy.svg";

     // Function takes in icon url and the daytime status from the period and returns the resource path string
    public static String getLocalPath(String nwsIconUrl, boolean isDaytime) {
        if (nwsIconUrl == null || nwsIconUrl.isEmpty()) { return FALLBACK; }
        String token = extractIconToken(nwsIconUrl);
        return resolveToken(token.toLowerCase(), isDaytime);
    }

    // Function extracts the token from the icon url
    static String extractIconToken(String url) {
        int q = url.indexOf('?');
        String path = (q >= 0) ? url.substring(0, q) : url;
        int slash = path.lastIndexOf('/');
        String segment = (slash >= 0) ? path.substring(slash + 1) : path;
        int comma = segment.indexOf(',');
        return (comma >= 0) ? segment.substring(0, comma) : segment;
    }

    // Function checks the token available and returns the path to the icon
    private static String resolveToken(String token, boolean day) {
        // Thunderstorm
        if (token.contains("tsra") || token.contains("thunder")) {
            return day ? BASE + "16_storms.svg" : BASE + "16_storms_night.svg";
        }
        // Clear / sunny
        if (token.equals("skc")) {
            return day ? BASE + "01_sunny.svg" : BASE + "02_clear_night.svg";
        }
        // Few clouds
        if (token.contains("few")) {
            return day ? BASE + "05_mostly_sunny.svg" : BASE + "05_mostly_sunny_night.svg";
        }
        // Scattered clouds
        if (token.contains("sct")) {
            return day ? BASE + "03_partly_cloudy.svg" : BASE + "03_partly_cloudy_night.svg";
        }
        // Broken / overcast
        if (token.contains("bkn") || token.contains("ovc")) {
            return day ? BASE + "04_cloudy.svg" : BASE + "04_cloudy_night.svg";
        }
        // Snow
        if (token.contains("snow") || token.equals("sn") || token.contains("blizzard")) {
            return day ? BASE + "15_snow.svg" : BASE + "15_snow_night.svg";
        }
        // Rain / showers
        if (token.contains("rain") || token.contains("shra") || token.contains("shower")) {
            return day ? BASE + "12_rain.svg" : BASE + "12_rain_night.svg";
        }
        // Fog / haze
        if (token.contains("fog") || token.equals("fg") || token.contains("haze")) {
            return day ? BASE + "10_fog.svg" : BASE + "10_fog_night.svg";
        }
        // Wind
        if (token.contains("wind") || token.contains("breezy")) {
            return day ? BASE + "09_wind.svg" : BASE + "09_wind_night.svg";
        }
        // Hot
        if (token.contains("hot")) {
            return BASE + "01_sunny.svg";
        }
        // Cold / frost
        if (token.contains("cold") || token.contains("frost")) {
            return day ? BASE + "14_frost.svg" : BASE + "14_frost_night.svg";
        }

        return FALLBACK;
    }
}
