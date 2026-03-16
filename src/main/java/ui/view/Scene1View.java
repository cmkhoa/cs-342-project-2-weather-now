package ui.view;

import ui.component.HourlyCard;
import ui.component.WeekdayRow;

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

/**
 * Builds the full Scene 1 layout from live data.
 * Figma: node 1:276  "Scene 1 - Main Screen"
 * Canvas size: 540 × 1080
 *
 * Structure:
 *   VBox (root, 540w)
 *   ├── MenuBar        HBox: [locationBtn]  [homeBtn]
 *   ├── QuickInfo      VBox: heroIcon + temp + shortDesc
 *   ├── currentInfo    HBox: precip | wind | humidity  ← from hourly[0]
 *   ├── hourlyInfo     VBox: "Today"/date + HBox scroll strip  ← from hourly list
 *   └── 3DayInfo       VBox: "Next Forecast"/"More>" + 3 WeekdayRows  ← from 12-hr periods
 *
 * Phase 3 change: build() now accepts a locationName string so the
 * location button in the menu bar reflects the current city rather
 * than the hardcoded "Chicago" from Phase 1.
 */
public class Scene1View {

    // Callbacks wired by Scene1Controller
    private Runnable onMoreForecastClick;
    private Runnable onLocationClick;
    private Runnable onHomeClick;

    // ---------------------------------------------------------------
    // Public builder
    // ---------------------------------------------------------------

    /**
     * Constructs and returns a fully populated Scene 1.
     *
     * @param periods12hr   The 14 12-hour periods from WeatherAPI (must not be null)
     * @param hourlyPeriods The hourly periods from MyWeatherAPI (may be empty)
     * @param locationName  Display name for the location button, e.g. "Chicago, IL"
     */
    public Scene build(ArrayList<Period> periods12hr,
                       ArrayList<HourlyPeriod> hourlyPeriods,
                       String locationName) {
        VBox root = new VBox();
        root.getStyleClass().add("scene1-root");

        root.getChildren().addAll(
                buildMenuBar(locationName),
                buildQuickInfo(periods12hr, hourlyPeriods),
                buildCurrentInfo(hourlyPeriods),
                buildHourlyInfo(hourlyPeriods),
                buildThreeDayInfo(periods12hr)
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    /**
     * Backwards-compatible overload for the Phase 1 hardcoded path.
     * Passes "Chicago, IL" as the location name.
     */
    public Scene build(ArrayList<Period> periods12hr,
                       ArrayList<HourlyPeriod> hourlyPeriods) {
        return build(periods12hr, hourlyPeriods, "Chicago, IL");
    }

    // ---------------------------------------------------------------
    // Callback setters (called by Scene1Controller before build())
    // ---------------------------------------------------------------

    public void setOnMoreForecastClick(Runnable r) { this.onMoreForecastClick = r; }
    public void setOnLocationClick(Runnable r)     { this.onLocationClick = r; }
    public void setOnHomeClick(Runnable r)          { this.onHomeClick = r; }

    // ---------------------------------------------------------------
    // Section builders
    // ---------------------------------------------------------------

    /**
     * Menu bar — Figma node 2:338
     * [ <Location> button (left) ]  [ <home> button (right) ]
     *
     * Phase 3: locationName is now dynamic instead of hardcoded "Chicago".
     */
    private HBox buildMenuBar(String locationName) {
        HBox bar = new HBox();
        bar.getStyleClass().add("menu-bar");

        Button locationBtn = new Button(locationName != null ? locationName : "—");
        locationBtn.getStyleClass().add("location-btn");
        locationBtn.setOnAction(e -> { if (onLocationClick != null) onLocationClick.run(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button homeBtn = new Button("Home");
        homeBtn.getStyleClass().add("home-btn");
        homeBtn.setOnAction(e -> { if (onHomeClick != null) onHomeClick.run(); });

        bar.getChildren().addAll(locationBtn, spacer, homeBtn);
        return bar;
    }

    /**
     * Quick info hero — Figma node 5:350
     * Centered: icon (180×180) → temp (40px) → shortDesc (20px)
     */
    private VBox buildQuickInfo(ArrayList<Period> periods12hr,
                                ArrayList<HourlyPeriod> hourlyPeriods) {
        VBox box = new VBox();
        box.getStyleClass().add("quick-info");

        Period current = (hourlyPeriods != null && !hourlyPeriods.isEmpty())
                ? hourlyPeriods.get(0)
                : (periods12hr != null && !periods12hr.isEmpty() ? periods12hr.get(0) : null);

        if (current != null) {
            // Hero icon
            Region icon = SvgIcon.load(IconRouter.getLocalPath(current.icon, current.isDaytime));
            icon.getStyleClass().add("hero-icon");

            // Temperature
            Label tempLabel = new Label(TempConverter.format(current.temperature));
            tempLabel.getStyleClass().add("hero-temp");

            // Short description
            Label descLabel = new Label(current.shortForecast != null ? current.shortForecast : "");
            descLabel.getStyleClass().add("hero-desc");

            box.getChildren().addAll(icon, tempLabel, descLabel);
        }

        return box;
    }

    /**
     * Current conditions bar — Figma node 15:359
     * Precip | Wind speed+direction | Humidity
     */
    private HBox buildCurrentInfo(ArrayList<HourlyPeriod> hourlyPeriods) {
        HBox bar = new HBox();
        bar.getStyleClass().add("current-info-bar");

        HourlyPeriod h = (hourlyPeriods != null && !hourlyPeriods.isEmpty())
                ? hourlyPeriods.get(0) : null;

        String precip   = h != null && h.probabilityOfPrecipitation != null
                ? h.probabilityOfPrecipitation.value + "%" : "0%";
        String wind     = h != null && h.windSpeed != null ? h.windSpeed : "--";
        String humidity = h != null && h.relativeHumidity != null
                ? h.relativeHumidity.value + "%" : "--%";

        bar.getChildren().addAll(
                buildStatSlot("💧", precip),
                buildStatSlot("💨", wind),
                buildStatSlot("🌢", humidity)
        );

        return bar;
    }

    /**
     * Hourly forecast strip — Figma node 8:354
     */
    private VBox buildHourlyInfo(ArrayList<HourlyPeriod> hourlyPeriods) {
        VBox box = new VBox();
        box.getStyleClass().add("hourly-info-box");

        // Header row
        HBox header = new HBox();
        header.getStyleClass().add("hourly-header");

        Label todayLabel = new Label("Today");
        todayLabel.getStyleClass().add("today-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(formatTodayDate());
        dateLabel.getStyleClass().add("date-label");

        header.getChildren().addAll(todayLabel, spacer, dateLabel);

        // Hourly scroll strip
        HBox strip = new HBox();
        strip.getStyleClass().add("hourly-strip");

        if (hourlyPeriods != null) {
            int limit = Math.min(hourlyPeriods.size(), 24);
            for (int i = 0; i < limit; i++) {
                strip.getChildren().add(new HourlyCard(hourlyPeriods.get(i)));
            }
        }

        ScrollPane scrollPane = new ScrollPane(strip);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("hourly-scroll");

        box.getChildren().addAll(header, scrollPane);
        return box;
    }

    /**
     * 3-day next forecast — Figma node 16:360
     */
    private VBox buildThreeDayInfo(ArrayList<Period> periods12hr) {
        VBox box = new VBox();
        box.getStyleClass().add("three-day-box");

        HBox header = new HBox();
        header.getStyleClass().add("three-day-header");

        Label nextLabel = new Label("Next Forecast");
        nextLabel.getStyleClass().add("next-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button moreBtn = new Button("More >");
        moreBtn.getStyleClass().add("more-btn");
        moreBtn.setOnAction(e -> { if (onMoreForecastClick != null) onMoreForecastClick.run(); });

        header.getChildren().addAll(nextLabel, spacer, moreBtn);

        VBox rows = new VBox();
        rows.getStyleClass().add("three-day-rows");

        if (periods12hr != null) {
            for (int i = 0; i < 3; i++) {
                int dayIdx   = i * 2;
                int nightIdx = i * 2 + 1;
                Period day   = (dayIdx   < periods12hr.size()) ? periods12hr.get(dayIdx)   : null;
                Period night = (nightIdx < periods12hr.size()) ? periods12hr.get(nightIdx) : null;
                if (day != null) {
                    rows.getChildren().add(new WeekdayRow(day, night));
                }
            }
        }

        box.getChildren().addAll(header, rows);
        return box;
    }

    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------

    private HBox buildStatSlot(String iconText, String valueText) {
        HBox slot = new HBox();
        slot.getStyleClass().add("stat-slot");

        Label iconLabel = new Label(iconText);
        iconLabel.getStyleClass().add("stat-icon-label");

        Label valueLabel = new Label(valueText);
        valueLabel.getStyleClass().add("stat-value-label");

        slot.getChildren().addAll(iconLabel, valueLabel);
        return slot;
    }

    private String formatTodayDate() {
        try {
            return new SimpleDateFormat("MMM d").format(new Date());
        } catch (Exception e) {
            return "";
        }
    }
}
