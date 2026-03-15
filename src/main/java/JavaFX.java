import javafx.application.Application;

import javafx.scene.Scene;

import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.HourlyPeriod;
import weather.Period;
import weather.WeatherAPI;

import java.util.ArrayList;

public class JavaFX extends Application {
	TextField temperature,weather;

	public static void main(String[] args) {
		launch(args);
	}

	//feel free to remove the starter code from this method
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("I'm a professional Weather App!");
		ArrayList<Period> forecast = WeatherAPI.getForecast("LOT",77,70);
		if (forecast == null){
			throw new RuntimeException("Forecast did not load");
		}
		temperature = new TextField();
		weather = new TextField();
		temperature.setText("Today's weather is: "+String.valueOf(forecast.get(0).temperature));
		weather.setText(forecast.get(0).shortForecast);
		for (Period p : forecast) {
			System.out.println("--- Period " + p.number + ": " + p.name + " ---");
			System.out.println("Time: " + p.startTime + " to " + p.endTime);
			System.out.println("Is Daytime: " + p.isDaytime);
			System.out.println("Temp: " + p.temperature + " " + p.temperatureUnit);
			System.out.println("Trend: " + p.temperatureTrend);
			System.out.println("Precip: " + (p.probabilityOfPrecipitation != null ? p.probabilityOfPrecipitation.value : "0") + "%");
			System.out.println("Wind: " + p.windSpeed + " from " + p.windDirection);
			System.out.println("Short: " + p.shortForecast);
			System.out.println("Detailed: " + p.detailedForecast);
			System.out.println("Icon URL: " + p.icon);
			System.out.println("-------------------------------------\n");
		}

		ArrayList<HourlyPeriod> hourlyForecast = model.MyWeatherAPI.getHourlyForecast("LOT",77,70);
		for (HourlyPeriod p : hourlyForecast) {
			System.out.println("--- Hourly Period " + p.number + ": " + p.name + " ---");
			System.out.println("Time: " + p.startTime + " to " + p.endTime);
			System.out.println("Temp: " + p.temperature + " " + p.temperatureUnit);
			System.out.println("Precip: " + (p.probabilityOfPrecipitation != null ? p.probabilityOfPrecipitation.value : "0") + "%");
			System.out.println("Wind: " + p.windSpeed + " from " + p.windDirection);
			System.out.println("DewPoint " + p.dewpoint.value);
			System.out.println("Humidity " + p.relativeHumidity.value);
			System.out.println("Short: " + p.shortForecast);
			System.out.println("Detailed: " + p.detailedForecast);
			System.out.println("Icon URL: " + p.icon);
		}



		Scene scene = new Scene(new VBox(temperature,weather), 700,700);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
