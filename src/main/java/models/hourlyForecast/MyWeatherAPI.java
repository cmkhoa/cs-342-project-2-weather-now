package models.hourlyForecast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import weather.WeatherAPI;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

// Extends WeatherAPI to add methods for fetching and parsing hourly forecasts.
// no longer used because of hw 4
public class MyWeatherAPI extends WeatherAPI {
    public static ArrayList<HourlyPeriod> getHourlyForecast(String region, int gridx, int gridy) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.weather.gov/gridpoints/"+region+"/"+String.valueOf(gridx)+","+String.valueOf(gridy)+"/forecast/hourly"))
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) { e.printStackTrace(); }
        HourlyRoot r = getHourlyObject(response.body());
        if(r == null){
            System.err.println("Failed to parse JSon");
            return null;
        }
        return r.properties.periods;
    }

    public static HourlyRoot getHourlyObject(String json){
        ObjectMapper om = new ObjectMapper();
        HourlyRoot toRet = null;
        try {
            toRet = om.readValue(json, HourlyRoot.class);
            ArrayList<HourlyPeriod> p = toRet.properties.periods;
        } catch (JsonProcessingException e) { e.printStackTrace(); }
        return toRet;

    }
}