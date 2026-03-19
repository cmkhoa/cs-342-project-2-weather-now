package ui.view;

import ui.component.HourlyCard;
import ui.component.WeekdayRow;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import models.hourlyForecast.HourlyPeriod;
import utils.IconRouter;
import utils.SvgIcon;
import utils.TempConverter;
import weather.Period;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javafx.scene.input.ScrollEvent;

/**
 * Scene 1 view — Figma-aligned glassmorphism over a weather-reactive background.
 *
 * Fixes applied:
 *  - Stat slots use equal Priority.ALWAYS HGrow so each takes 1/3 width, centered
 *  - Home button hidden when on Chicago, visible otherwise
 *  - Weather theme class drives both Scene1 and (via controller) Scene2 backgrounds
 *  - All font sizes ≥ 14px (hero desc = 16, stat value = 15, weekday = 14, etc.)
 *  - Text in stat slots is centered using VBox alignment + HBox.setHgrow equal thirds
 */
public class Scene1View {

    private Runnable onMoreForecastClick;
    private Runnable onLocationClick;
    private Runnable onHomeClick;

    // Expose the detected weather theme so Scene2Controller can reuse it
    private String detectedWeatherClass = "";

    public String getDetectedWeatherClass() { return detectedWeatherClass; }

    // ---------------------------------------------------------------
    // Public builder
    // ---------------------------------------------------------------

    public Scene build(ArrayList<Period> periods12hr,
                       ArrayList<HourlyPeriod> hourlyPeriods,
                       String locationName) {
        VBox root = new VBox();
        root.getStyleClass().add("scene1-root");

        Period current = (hourlyPeriods != null && !hourlyPeriods.isEmpty())
                ? hourlyPeriods.get(0)
                : (periods12hr != null && !periods12hr.isEmpty() ? periods12hr.get(0) : null);

        detectedWeatherClass = detectWeatherClass(current);
        if (!detectedWeatherClass.isEmpty()) {
            root.getStyleClass().add(detectedWeatherClass);
        }

        boolean isHome = isHomeLocation(locationName);

        root.getChildren().addAll(
                buildMenuBar(locationName, isHome),
                buildQuickInfo(periods12hr, hourlyPeriods),
                buildCurrentInfo(hourlyPeriods),
                buildHourlyInfo(hourlyPeriods),
                buildThreeDayInfo(periods12hr)
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    public Scene build(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods) {
        return build(periods12hr, hourlyPeriods, "Chicago, IL");
    }

    public void setOnMoreForecastClick(Runnable r) { this.onMoreForecastClick = r; }
    public void setOnLocationClick(Runnable r)     { this.onLocationClick = r; }
    public void setOnHomeClick(Runnable r)          { this.onHomeClick = r; }

    // ---------------------------------------------------------------
    // Weather theme detection
    // ---------------------------------------------------------------

    /** Returns the CSS class to add to root for weather-reactive background. */
    private String detectWeatherClass(Period period) {
        if (period == null) return "";
        String icon = period.icon != null ? period.icon.toLowerCase() : "";
        boolean day = period.isDaytime;

        if (icon.contains("tsra") || icon.contains("thunder"))               return "weather-storm";
        if (icon.contains("rain") || icon.contains("shra") || icon.contains("shower")) return "weather-rain";
        if (icon.contains("snow") || icon.contains("blizzard"))              return "weather-snow";
        if (icon.contains("fog")  || icon.contains(",fg"))                   return "weather-fog";
        if (!day)                                                             return "weather-clear-night";
        if (icon.contains("ovc")  || icon.contains("bkn"))                  return "weather-cloudy";
        return "weather-sunny";
    }

    private boolean isHomeLocation(String locationName) {
        return locationName == null || locationName.toLowerCase().contains("chicago");
    }

    // ---------------------------------------------------------------
    // Section builders
    // ---------------------------------------------------------------

    /** Menu bar: [ 📍 City ]  [spacer]  [ 🏠 (only when not home) ] */
    private HBox buildMenuBar(String locationName, boolean isHome) {
        HBox bar = new HBox();
        bar.getStyleClass().add("menu-bar");

        Button locationBtn = buildLocationButton(locationName);
        locationBtn.setOnAction(e -> { if (onLocationClick != null) onLocationClick.run(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(locationBtn, spacer);

        if (!isHome) {
            Button homeBtn = buildHomeButton();
            homeBtn.setOnAction(e -> { if (onHomeClick != null) onHomeClick.run(); });
            bar.getChildren().add(homeBtn);
        }

        return bar;
    }

    private Button buildLocationButton(String locationName) {
        Region pinIcon = SvgIcon.loadTinted("/ui-icons/location.svg", 16);
        pinIcon.getStyleClass().add("location-icon");

        Label nameLabel = new Label(locationName != null ? locationName : "—");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        HBox content = new HBox(6, pinIcon, nameLabel);
        content.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.getStyleClass().add("location-btn");
        return btn;
    }

    private Button buildHomeButton() {
        Region homeIcon = SvgIcon.loadTinted("/ui-icons/homeButton.svg", 20);
        homeIcon.getStyleClass().add("home-icon");

        Button btn = new Button();
        btn.setGraphic(homeIcon);
        btn.getStyleClass().add("home-btn");
        return btn;
    }

    /** Hero: centered icon → big temperature → short description */
    private VBox buildQuickInfo(ArrayList<Period> periods12hr,
                                ArrayList<HourlyPeriod> hourlyPeriods) {
        VBox box = new VBox();
        box.getStyleClass().add("quick-info");

        Period current = (hourlyPeriods != null && !hourlyPeriods.isEmpty())
                ? hourlyPeriods.get(0)
                : (periods12hr != null && !periods12hr.isEmpty() ? periods12hr.get(0) : null);

        if (current != null) {
            Region icon = SvgIcon.load(IconRouter.getLocalPath(current.icon, current.isDaytime));
            icon.getStyleClass().add("hero-icon");

            Label tempLabel = new Label(TempConverter.format(current.temperature));
            tempLabel.getStyleClass().add("hero-temp");

            Label descLabel = new Label(current.shortForecast != null ? current.shortForecast : "");
            descLabel.getStyleClass().add("hero-desc");

            box.getChildren().addAll(icon, tempLabel, descLabel);
        }
        return box;
    }

    /**
     * Current conditions bar — 3 equal columns, each centered.
     *
     * Layout:  [ slot1 (grow) ] [ divider ] [ slot2 (grow) ] [ divider ] [ slot3 (grow) ]
     *
     * Each slot is a VBox(icon, value, key) with Priority.ALWAYS HGrow so all three
     * columns are exactly equal width and their content is centered within the bar.
     */
    private HBox buildCurrentInfo(ArrayList<HourlyPeriod> hourlyPeriods) {
        HBox bar = new HBox();
        bar.getStyleClass().add("current-info-bar");

        HourlyPeriod h = (hourlyPeriods != null && !hourlyPeriods.isEmpty())
                ? hourlyPeriods.get(0) : null;

        String precip   = (h != null && h.probabilityOfPrecipitation != null)
                ? h.probabilityOfPrecipitation.value + "%" : "0%";
        String wind     = (h != null && h.windSpeed != null) ? h.windSpeed : "--";
        String humidity = (h != null && h.relativeHumidity != null)
                ? h.relativeHumidity.value + "%" : "--%";

        VBox slot1 = buildStatColumn("💧", precip,   "Precip");
        VBox slot2 = buildStatColumn("💨", wind,     "Wind");
        VBox slot3 = buildStatColumn("💦", humidity, "Humidity");

        // Give each slot equal width
        HBox.setHgrow(slot1, Priority.ALWAYS);
        HBox.setHgrow(slot2, Priority.ALWAYS);
        HBox.setHgrow(slot3, Priority.ALWAYS);

        Region div1 = new Region();
        div1.getStyleClass().add("stat-separator");

        Region div2 = new Region();
        div2.getStyleClass().add("stat-separator");

        bar.getChildren().addAll(slot1, div1, slot2, div2, slot3);
        return bar;
    }

    /**
     * A single stat column: icon / value / key, all centered.
     */
    private VBox buildStatColumn(String iconText, String valueText, String keyText) {
        Label iconLabel  = new Label(iconText);
        iconLabel.getStyleClass().add("stat-icon-label");
        iconLabel.setMaxWidth(Double.MAX_VALUE);
        iconLabel.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(valueText);
        valueLabel.getStyleClass().add("stat-value-label");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER);

        Label keyLabel   = new Label(keyText);
        keyLabel.getStyleClass().add("stat-key-label");
        keyLabel.setMaxWidth(Double.MAX_VALUE);
        keyLabel.setAlignment(Pos.CENTER);

        VBox col = new VBox(2, iconLabel, valueLabel, keyLabel);
        col.setAlignment(Pos.CENTER);
        col.setMaxWidth(Double.MAX_VALUE);
        return col;
    }

    /** Hourly strip with invisible horizontal scroll. */
    private VBox buildHourlyInfo(ArrayList<HourlyPeriod> hourlyPeriods) {
        VBox box = new VBox();
        box.getStyleClass().add("hourly-info-box");

        HBox header = new HBox();
        header.getStyleClass().add("hourly-header");

        Label todayLabel = new Label("Today");
        todayLabel.getStyleClass().add("today-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(formatTodayDate());
        dateLabel.getStyleClass().add("date-label");

        header.getChildren().addAll(todayLabel, spacer, dateLabel);

        HBox strip = new HBox();
        strip.getStyleClass().add("hourly-strip");

        if (hourlyPeriods != null) {
            int limit = Math.min(hourlyPeriods.size(), 24);
            for (int i = 0; i < limit; i++) {
                HourlyCard card = new HourlyCard(hourlyPeriods.get(i));
                if (i == 0) card.getStyleClass().add("current-hour");
                strip.getChildren().add(card);
            }
        }

        ScrollPane scrollPane = new ScrollPane(strip);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(false);
        scrollPane.setPrefHeight(108);
        scrollPane.getStyleClass().add("hourly-scroll");

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY() != 0 ? event.getDeltaY() : event.getDeltaX();
            scrollPane.setHvalue(scrollPane.getHvalue() - (delta / 500.0) * scrollPane.getHmax());
            event.consume();
        });

        box.getChildren().addAll(header, scrollPane);
        return box;
    }

    /** 3-day forecast panel with more-icon button. */
    private VBox buildThreeDayInfo(ArrayList<Period> periods12hr) {
        VBox box = new VBox();
        box.getStyleClass().add("three-day-box");

        HBox header = new HBox();
        header.getStyleClass().add("three-day-header");

        Label nextLabel = new Label("Next Forecast");
        nextLabel.getStyleClass().add("next-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button moreBtn = buildMoreButton();
        moreBtn.setOnAction(e -> { if (onMoreForecastClick != null) onMoreForecastClick.run(); });

        header.getChildren().addAll(nextLabel, spacer, moreBtn);

        VBox rows = new VBox();
        rows.getStyleClass().add("three-day-rows");

        if (periods12hr != null) {
            // Start from first daytime period
            int startIdx = (!periods12hr.isEmpty() && !periods12hr.get(0).isDaytime) ? 1 : 0;
            int dayCount = 0;
            for (int i = startIdx; i + 1 < periods12hr.size() && dayCount < 3; i += 2) {
                Period day   = periods12hr.get(i);
                Period night = (i + 1 < periods12hr.size()) ? periods12hr.get(i + 1) : null;
                if (day.isDaytime) {
                    WeekdayRow row = new WeekdayRow(day, night);
                    if (dayCount == 2) row.getStyleClass().add("last-row");
                    rows.getChildren().add(row);
                    dayCount++;
                }
            }
        }

        box.getChildren().addAll(header, rows);
        return box;
    }

    private Button buildMoreButton() {
        Region moreIcon = SvgIcon.loadTinted("/ui-icons/moreButton.svg", 18);
        moreIcon.getStyleClass().add("more-icon");
        Button btn = new Button();
        btn.setGraphic(moreIcon);
        btn.getStyleClass().add("more-btn");
        return btn;
    }

    private String formatTodayDate() {
        try { return new SimpleDateFormat("MMM d").format(new Date()); }
        catch (Exception e) { return ""; }
    }
}