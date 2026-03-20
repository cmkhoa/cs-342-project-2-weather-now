package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

/**
 * Template Method pattern for homework 4.
 *  Defines the skeleton algorithm for fetching a weather forecast:
 *  fetch calls buildUrl, httpGet and parseResponse(), with subclasses changing buildurl and parseresponse
 * @param <T>  The period type produced: Period or HourlyPeriod.
 */
public abstract class AbstractForecastService<T> {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    // Fetches and parses a weather forecast for the given NWS grid point.
    public final ArrayList<T> fetch(String office, int gridX, int gridY) {
        String url  = buildUrl(office, gridX, gridY);
        String json = httpGet(url);
        if (json == null) return null;
        return parseResponse(json);
    }
    // abstract functions to be clarified by sub classes
    // function returns and build url for the corresponding type of api
    protected abstract String buildUrl(String office, int gridX, int gridY);
    // function parses the json response from the http api call
    protected abstract ArrayList<T> parseResponse(String json);

    // Non changing part of the template
    // function uses the url constructed to call and return a json string
    private String httpGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return null;
        }
    }
}