package util;

/**
 * Maps an NWS forecast icon URL to a local static BOM-app SVG resource path.
 *
 * Source folder: /weather-icons-main/bom/app/
 * All icons are 96×96px static SVGs — rendered via {@link SvgIcon}.
 *
 * NWS token extraction:
 *   "https://api.weather.gov/icons/land/day/skc?size=medium"  → token "skc"
 *   "https://api.weather.gov/icons/land/night/tsra,40?size=medium" → token "tsra"
 *
 * Constraint keyword → BOM file mapping:
 *  ┌──────────────────┬─────────────────────────────────────────────┐
 *  │ NWS token(s)     │ BOM file (day / night)                      │
 *  ├──────────────────┼─────────────────────────────────────────────┤
 *  │ skc              │ 01_sunny  /  02_clear_night                 │
 *  │ few              │ 05_mostly_sunny  /  05_mostly_sunny_night    │
 *  │ sct              │ 03_partly_cloudy  /  03_partly_cloudy_night  │
 *  │ bkn              │ 04_cloudy  /  04_cloudy_night               │
 *  │ ovc              │ 04_cloudy  /  04_cloudy_night               │
 *  │ rain, ra, shra   │ 12_rain  /  12_rain_night                   │
 *  │ snow, sn         │ 15_snow  /  15_snow_night                   │
 *  │ tsra             │ 16_storms  /  16_storms_night               │
 *  │ fog, fg          │ 10_fog  /  10_fog_night                     │
 *  │ wind, breezy     │ 09_wind  /  09_wind_night                   │
 *  │ hot              │ 01_sunny  (hottest = clear day)             │
 *  │ cold, frost      │ 14_frost  /  14_frost_night                 │
 *  └──────────────────┴─────────────────────────────────────────────┘
 */
public class IconRouter {

    private static final String BASE     = "/weather-icons-main/bom/app/";
    private static final String FALLBACK = BASE + "04_cloudy.svg";

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Resolves an NWS icon URL to a local BOM-app SVG resource path.
     *
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

    // -----------------------------------------------------------------------
    // Token extraction
    // -----------------------------------------------------------------------

    /**
     * Strips the NWS icon URL down to just the condition token.
     *
     * Examples:
     *   ".../day/skc?size=medium"   → "skc"
     *   ".../night/tsra,40?size=m"  → "tsra"
     *   ".../day/rain_showers,20"   → "rain_showers"
     */
    static String extractIconToken(String url) {
        int q = url.indexOf('?');
        String path = (q >= 0) ? url.substring(0, q) : url;

        int slash = path.lastIndexOf('/');
        String segment = (slash >= 0) ? path.substring(slash + 1) : path;

        int comma = segment.indexOf(',');
        return (comma >= 0) ? segment.substring(0, comma) : segment;
    }

    // -----------------------------------------------------------------------
    // Keyword → BOM filename
    // Evaluation order matters: tsra before rain; sn/snow before ra.
    // -----------------------------------------------------------------------

    private static String resolveToken(String t, boolean day) {
        // Thunderstorm (check before rain)
        if (t.contains("tsra") || t.contains("thunder")) {
            return day ? BASE + "16_storms.svg" : BASE + "16_storms_night.svg";
        }
        // Clear / sunny
        if (t.equals("skc")) {
            return day ? BASE + "01_sunny.svg" : BASE + "02_clear_night.svg";
        }
        // Few clouds
        if (t.contains("few")) {
            return day ? BASE + "05_mostly_sunny.svg" : BASE + "05_mostly_sunny_night.svg";
        }
        // Scattered clouds
        if (t.contains("sct")) {
            return day ? BASE + "03_partly_cloudy.svg" : BASE + "03_partly_cloudy_night.svg";
        }
        // Broken / overcast
        if (t.contains("bkn") || t.contains("ovc")) {
            return day ? BASE + "04_cloudy.svg" : BASE + "04_cloudy_night.svg";
        }
        // Snow (check before "rain" to avoid rain_snow misroute)
        if (t.contains("snow") || t.equals("sn") || t.contains("blizzard")) {
            return day ? BASE + "15_snow.svg" : BASE + "15_snow_night.svg";
        }
        // Rain / showers
        if (t.contains("rain") || t.contains("shra") || t.contains("shower")) {
            return day ? BASE + "12_rain.svg" : BASE + "12_rain_night.svg";
        }
        // Fog / haze
        if (t.contains("fog") || t.equals("fg") || t.contains("haze")) {
            return day ? BASE + "10_fog.svg" : BASE + "10_fog_night.svg";
        }
        // Wind
        if (t.contains("wind") || t.contains("breezy")) {
            return day ? BASE + "09_wind.svg" : BASE + "09_wind_night.svg";
        }
        // Hot
        if (t.contains("hot")) {
            return BASE + "01_sunny.svg";
        }
        // Cold / frost
        if (t.contains("cold") || t.contains("frost")) {
            return day ? BASE + "14_frost.svg" : BASE + "14_frost_night.svg";
        }

        return FALLBACK;
    }
}
