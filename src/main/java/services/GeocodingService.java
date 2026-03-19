package services;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.location.LocationNode;
import models.nwsPoints.nwsPoint;

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
 * Geocoding services (location name -> OSM data)
 *  searchByCity("Chicago") → List<LocationNode>
 *  Provider: https://nominatim.openstreetmap.org
 */
public class GeocodingService {
    private static final String NOMINATIM_SEARCH_URL =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=";
    private static final String NWS_POINTS_URL =
            "https://api.weather.gov/points/";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OM     = new ObjectMapper();

    // Searches Nominatim by city name and returns up to 5 Stage-1 LocationNode results
    public static List<LocationNode> searchByCity(String cityName) {
        // perform the call
        List<LocationNode> results = new ArrayList<>();
        String expanded = expand(cityName);
        System.out.println("[GeocodingService] searchByCity: " + expanded);
        List<NominatimResult> candidates = nominatimSearch(expanded);

        // if no results
        if (candidates == null || candidates.isEmpty()) {
            System.err.println("[GeocodingService] No Nominatim results for: " + expanded);
            return results;
        }

        // parse results into nodes
        for (NominatimResult candidate : candidates) {
            try {
                double lat = Double.parseDouble(candidate.lat);
                double lon = Double.parseDouble(candidate.lon);

                LocationNode lw = buildFromCoords(lat, lon);
                if (lw == null) continue;
                // Nominatim returns a full string like: "City of New York, New York, United States"
                if (candidate.display_name != null) {
                    String[] parts = candidate.display_name.split(",");
                    String city = parts[0].trim();
                    // Extract state from Nominatim (usually the 2nd to last item)
                    if (parts.length >= 3) {
                        String state = parts[parts.length - 2].trim();
                        lw.displayName = city + ", " + state;
                    } else { lw.displayName = city; }
                }
                results.add(lw);
                if (results.size() >= 5) break;

            } catch (Exception e) { System.err.println("[GeocodingService] Skipping candidate: " + e.getMessage()); }
        }
        return results;
    }


    // Internal method that constructs a location node from the coordinates and populates the other fields with api calls

    private static LocationNode buildFromCoords(double lat, double lon) {
        nwsPoint nws = resolveNwsGrid(lat, lon);
        if (nws == null) return null;
        LocationNode lw = new LocationNode(nws.displayName, lat, lon, nws.office, nws.gridX, nws.gridY);
        lw.periods12hr = weather.WeatherAPI.getForecast(nws.office, nws.gridX, nws.gridY);
        if (lw.periods12hr == null || lw.periods12hr.isEmpty()) return null;
        lw.refreshConvenience();
        return lw;
    }


    //Calls Nominatim geocoding api and returns the raw result list sorted by Nominatim.
    private static List<NominatimResult> nominatimSearch(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(NOMINATIM_SEARCH_URL + encoded)).build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            NominatimResult[] arr = OM.readValue(resp.body(), NominatimResult[].class);
            return List.of(arr);
        } catch (Exception e) {
            System.err.println("[GeocodingService] Nominatim /search error: " + e.getMessage());
            return null;
        }
    }

    // Calls nws points endpoint to get station data on a coordinates
    private static nwsPoint resolveNwsGrid(double lat, double lon) {
        try {
            String coord = String.format("%.4f,%.4f", lat, lon);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(NWS_POINTS_URL + coord)).build();
            HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            NwsPointsRoot root = OM.readValue(resp.body(), NwsPointsRoot.class);
            if (root == null || root.properties == null) return null;
            if (root.properties.gridId == null || root.properties.gridId.isEmpty()) return null;

            nwsPoint result = new nwsPoint();
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

    // expand nicknames of cities for search query
    static String expand(String query) {
        if (query == null) return "";
        String key = query.trim().toLowerCase();
        return ALIASES.getOrDefault(key, query.trim());
    }

    // classes for parsing the nominatim and nws points api
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
        public String gridId;
        public int gridX;
        public int gridY;
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

    // Nicknames for cities, used in searching
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
}