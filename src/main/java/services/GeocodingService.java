package services;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.location.LocationWeather;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Geocoding services with two distinct entry points, mirroring the
 * GeocodeByCity / GeocodeByPoint split in the reference Go implementation.
 * Methods:
 *   GeocodingService.searchByCity("Chicago") → List<LocationWeather>  (Scene 3 search bar)
 *   GeocodingService.searchByPoint(41.85, -87.65) → LocationWeather (map tap / coordinate lookup)
 * Provider: https://nominatim.openstreetmap.org
 * Threading: all methods are blocking — call from a background thread.
 */
public class GeocodingService {

    private static final String NOMINATIM_SEARCH_URL =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=";

    // reverse endpoint takes lat/lon directly and returns a single best match
    private static final String NOMINATIM_REVERSE_URL =
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%s&lon=%s";

    private static final String NWS_POINTS_URL =
            "https://api.weather.gov/points/";

    private static final HttpClient   CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OM     = new ObjectMapper();

    // ---------------------------------------------------------------
    // Abbreviation / nickname expansion table
    // ---------------------------------------------------------------

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        ALIASES.put("nyc",          "New York City");
        ALIASES.put("new york",     "New York City");
        ALIASES.put("ny",           "New York City");
        ALIASES.put("la",           "Los Angeles");
        ALIASES.put("l.a.",         "Los Angeles");
        ALIASES.put("san fran",     "San Francisco");
        ALIASES.put("sf",           "San Francisco");
        ALIASES.put("s.f.",         "San Francisco");
        ALIASES.put("chi",          "Chicago");
        ALIASES.put("chitown",      "Chicago");
        ALIASES.put("chi-town",     "Chicago");
        ALIASES.put("dc",           "Washington DC");
        ALIASES.put("d.c.",         "Washington DC");
        ALIASES.put("washington dc","Washington DC");
        ALIASES.put("philly",       "Philadelphia");
        ALIASES.put("h-town",       "Houston");
        ALIASES.put("htown",        "Houston");
        ALIASES.put("htx",          "Houston");
        ALIASES.put("atl",          "Atlanta");
        ALIASES.put("vegas",        "Las Vegas");
        ALIASES.put("lv",           "Las Vegas");
        ALIASES.put("nola",         "New Orleans");
        ALIASES.put("pdx",          "Portland Oregon");
        ALIASES.put("sea",          "Seattle");
        ALIASES.put("stl",          "Saint Louis");
        ALIASES.put("kc",           "Kansas City");
        ALIASES.put("cle",          "Cleveland");
        ALIASES.put("pitt",         "Pittsburgh");
        ALIASES.put("indy",         "Indianapolis");
        ALIASES.put("mpls",         "Minneapolis");
        ALIASES.put("twin cities",  "Minneapolis");
        ALIASES.put("sd",           "San Diego");
        ALIASES.put("oak",          "Oakland");
        ALIASES.put("bmore",        "Baltimore");
        ALIASES.put("okc",          "Oklahoma City");
        ALIASES.put("slc",          "Salt Lake City");
        ALIASES.put("abq",          "Albuquerque");
        ALIASES.put("dfw",          "Dallas");
    }

    // ================================================================
    // PUBLIC API
    // ================================================================

    /**
     * Scene 3 search bar entry point.
     *
     * Searches Nominatim by city name and returns up to 5 Stage-1
     * LocationWeather results. Abbreviations are expanded first.
     *
     * @param cityName  Raw user input, e.g. "nyc", "Denver", "Austin TX"
     * @return          List of matching LocationWeather objects (may be empty)
     */
    public static List<LocationWeather> searchByCity(String cityName) {
        List<LocationWeather> results = new ArrayList<>();

        String expanded = expand(cityName);
        System.out.println("[GeocodingService] searchByCity: \""
                + cityName + "\" → \"" + expanded + "\"");

        List<NominatimResult> candidates = nominatimSearch(expanded);
        if (candidates == null || candidates.isEmpty()) {
            System.err.println("[GeocodingService] No Nominatim results for: " + expanded);
            return results;
        }

        for (NominatimResult candidate : candidates) {
            try {
                double lat = Double.parseDouble(candidate.lat);
                double lon = Double.parseDouble(candidate.lon);

                LocationWeather lw = buildFromCoords(lat, lon);
                if (lw == null) continue;

                results.add(lw);
                if (results.size() >= 5) break;

            } catch (Exception e) {
                System.err.println("[GeocodingService] Skipping candidate: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Map tap / open range lookup entry point.
     *
     * Given raw coordinates (e.g. from a map click), resolves a single
     * best-match LocationWeather using Nominatim /reverse.
     * Returns null if coordinates are outside NWS coverage (non-US).
     *
     * @param lat  Latitude
     * @param lon  Longitude
     * @return     Stage-1 populated LocationWeather, or null on failure
     */
    public static LocationWeather searchByPoint(double lat, double lon) {
        System.out.printf("[GeocodingService] searchByPoint: %.4f, %.4f%n", lat, lon);
        return buildFromCoords(lat, lon);
    }

    // ================================================================
    // SHARED INTERNAL PIPELINE
    // ================================================================

    /**
     * Core pipeline shared by both public methods.
     * Given a lat/lon, calls NWS /points/ for the grid, then fetches
     * the 12-hr forecast. Returns a Stage-1 LocationWeather or null.
     */
    private static LocationWeather buildFromCoords(double lat, double lon) {
        NwsPointsResult nws = resolveNwsGrid(lat, lon);
        if (nws == null) return null;

        LocationWeather lw = new LocationWeather(
                nws.displayName, lat, lon,
                nws.office, nws.gridX, nws.gridY
        );

        lw.periods12hr = weather.WeatherAPI.getForecast(nws.office, nws.gridX, nws.gridY);
        if (lw.periods12hr == null || lw.periods12hr.isEmpty()) return null;

        lw.refreshConvenience();
        return lw;
    }

    // ================================================================
    // NOMINATIM
    // ================================================================

    /**
     * Calls Nominatim /search with the given query string.
     * Returns the raw result list sorted by Nominatim's importance score.
     */
    private static List<NominatimResult> nominatimSearch(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(NOMINATIM_SEARCH_URL + encoded))
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            NominatimResult[] arr = OM.readValue(resp.body(), NominatimResult[].class);
            return List.of(arr);

        } catch (Exception e) {
            System.err.println("[GeocodingService] Nominatim /search error: " + e.getMessage());
            return null;
        }
    }

    // ================================================================
    // NWS /points/
    // ================================================================

    /**
     * Calls NWS /points/{lat},{lon} to resolve the NWS grid and display name.
     * Returns null if the coordinates are outside NWS coverage or on error.
     */
    private static NwsPointsResult resolveNwsGrid(double lat, double lon) {
        try {
            String coord = String.format("%.4f,%.4f", lat, lon);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NWS_POINTS_URL + coord))
                    .build();

            HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            NwsPointsRoot root = OM.readValue(resp.body(), NwsPointsRoot.class);
            if (root == null || root.properties == null) return null;
            if (root.properties.gridId == null || root.properties.gridId.isEmpty()) return null;

            NwsPointsResult result = new NwsPointsResult();
            result.office = root.properties.gridId;
            result.gridX  = root.properties.gridX;
            result.gridY  = root.properties.gridY;

            String city  = (root.properties.relativeLocation != null)
                    ? root.properties.relativeLocation.properties.city  : "Unknown";
            String state = (root.properties.relativeLocation != null)
                    ? root.properties.relativeLocation.properties.state : "";
            result.displayName = city + ", " + state;

            return result;

        } catch (Exception e) {
            System.err.println("[GeocodingService] NWS /points/ error for "
                    + lat + "," + lon + ": " + e.getMessage());
            return null;
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Expands common city abbreviations/nicknames to full names.
     * Matching is case-insensitive on the trimmed input.
     */
    static String expand(String query) {
        if (query == null) return "";
        String key = query.trim().toLowerCase();
        return ALIASES.getOrDefault(key, query.trim());
    }

    // ================================================================
    // INTERNAL POJOs
    // ================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NominatimResult {
        public String lat;
        public String lon;
        public String display_name;
        public String type;        // "city", "borough", "town", etc.
        public double importance;  // Nominatim's ranking score
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NwsPointsRoot {
        public NwsPointsProperties properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NwsPointsProperties {
        public String          gridId;
        public int             gridX;
        public int             gridY;
        public RelativeLocation relativeLocation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RelativeLocation {
        public RelativeLocationProperties properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RelativeLocationProperties {
        public String city;
        public String state;
    }

    private static class NwsPointsResult {
        String office;
        int    gridX;
        int    gridY;
        String displayName;
    }
}