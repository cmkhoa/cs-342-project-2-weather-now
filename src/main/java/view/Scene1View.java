package view;

import component.HourlyCard;
import component.WeekdayRow;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import hourlyForecast.HourlyPeriod;
import util.IconRouter;
import util.SvgIcon;
import util.TempConverter;
import weather.Period;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Builds the full Scene 1 layout from live data.
 * Figma: node 1:276  "Scene 1 - Main Screen"
 * Canvas size: 540 × 1080
 * Structure:
 *   VBox (root, 540w)
 *   ├── MenuBar        HBox: [locationBtn]  [homeBtn]
 *   ├── QuickInfo      VBox: heroIcon + temp + shortDesc
 *   ├── currentInfo    HBox: precip | wind | humidity  ← from hourly[0]
 *   ├── hourlyInfo     VBox: "Today"/date + HBox scroll strip  ← from hourly list
 *   └── 3DayInfo       VBox: "Next Forecast"/"More>" + 3 WeekdayRows  ← from 12-hr periods
 */
public class Scene1View {

    // Callbacks wired by Scene1Controller
    private Runnable onMoreForecastClick;
    private Runnable onLocationClick;
    private Runnable onHomeClick;

    /**
     * Constructs and returns a fully populated Scene 1.
     *
     * @param periods12hr  The 14 12-hour periods from WeatherAPI (must not be null)
     * @param hourlyPeriods The hourly periods from model.MyWeatherAPI (must not be null)
     */
    public Scene build(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods) {
        VBox root = new VBox();
        root.getStyleClass().add("scene1-root");

        root.getChildren().addAll(
                buildMenuBar(),
                buildQuickInfo(periods12hr, hourlyPeriods),
                buildCurrentInfo(hourlyPeriods),
                buildHourlyInfo(hourlyPeriods),
                buildThreeDayInfo(periods12hr)
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
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
     */
    private HBox buildMenuBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("menu-bar");

        Button locationBtn = new Button("Chicago"); // hardcoded for Phase 1
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
     * Hero temp and shortDesc come from the 12-hr period[0].
     * Hero icon comes from 12-hr period[0].
     */
    private VBox buildQuickInfo(ArrayList<Period> periods12hr, ArrayList<HourlyPeriod> hourlyPeriods) {
        VBox box = new VBox();
        box.getStyleClass().add("quick-info");

        Period current = hourlyPeriods.isEmpty() ? null : hourlyPeriods.get(0);

        // Hero icon — rendered as a scalable JavaFX Region via SvgIcon
        Region heroIcon = new Region();
        if (current != null) {
            String path = IconRouter.getLocalPath(current.icon, current.isDaytime);
            heroIcon = SvgIcon.load(path);
        }
        heroIcon.getStyleClass().add("hero-icon");

        // Temperature label — 40px
        String tempText = current != null
                ? TempConverter.format(current.temperature)
                : "--";
        Label tempLabel = new Label(tempText);
        tempLabel.getStyleClass().add("hero-temp");

        // Short description — 20px
        String descText = current != null && current.shortForecast != null
                ? current.shortForecast
                : "--";
        Label descLabel = new Label(descText);
        descLabel.getStyleClass().add("hero-desc");

        box.getChildren().addAll(heroIcon, tempLabel, descLabel);
        return box;
    }

    /**
     * Current conditions strip — Figma node 15:359
     * Three icon+label slots: precipitation | wind speed | humidity
     * All values sourced from hourlyPeriods.get(0).
     */
    private HBox buildCurrentInfo(ArrayList<HourlyPeriod> hourlyPeriods) {
        HBox bar = new HBox();
        bar.getStyleClass().add("current-info-bar");

        HourlyPeriod h0 = (hourlyPeriods != null && !hourlyPeriods.isEmpty()) ? hourlyPeriods.get(0) : null;

        String precipText = h0 != null ? h0.probabilityOfPrecipitation.value + "%" : "--";
        String windText   = h0 != null && h0.windSpeed != null ? h0.windSpeed : "--";
        String humidText  = h0 != null ? h0.relativeHumidity.value + "%" : "--";

        HBox precipSlot   = buildStatSlot("💧", precipText);  // TODO: replace emoji with SVG icon
        HBox windSlot     = buildStatSlot("💨", windText);
        HBox humidSlot    = buildStatSlot("🌫", humidText);

        HBox.setHgrow(precipSlot, Priority.ALWAYS);
        HBox.setHgrow(windSlot,   Priority.ALWAYS);
        HBox.setHgrow(humidSlot,  Priority.ALWAYS);

        bar.getChildren().addAll(precipSlot, windSlot, humidSlot);
        return bar;
    }

    /**
     * Hourly forecast panel — Figma node 8:354
     * Header: "Today" (left) + date (right)
     * Horizontally scrollable row of HourlyCard components.
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
            // Show up to 24 hours ahead
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
     * 3-day next forecast section — Figma node 16:360
     * Header: "Next Forecast" (left) + "More >" button (right)
     * Three WeekdayRow components.
     */
    private VBox buildThreeDayInfo(ArrayList<Period> periods12hr) {
        VBox box = new VBox();
        box.getStyleClass().add("three-day-box");

        // Header row
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

        // 3 weekday rows — NWS periods alternate Day(even)/Night(odd)
        // Skip period[0] (today's current period); use pairs [0/1], [2/3], [4/5]
        VBox rows = new VBox();
        rows.getStyleClass().add("three-day-rows");

        for (int i = 0; i < 3; i++) {
            int dayIdx   = i * 2;
            int nightIdx = i * 2 + 1;
            Period day   = (dayIdx   < periods12hr.size()) ? periods12hr.get(dayIdx)   : null;
            Period night = (nightIdx < periods12hr.size()) ? periods12hr.get(nightIdx) : null;
            if (day != null) {
                rows.getChildren().add(new WeekdayRow(day, night));
            }
        }

        box.getChildren().addAll(header, rows);
        return box;
    }

    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------

    /** Builds an icon+text stat slot for the current conditions bar. */
    private HBox buildStatSlot(String iconText, String valueText) {
        HBox slot = new HBox();
        slot.getStyleClass().add("stat-slot");

        // Placeholder label — replace with actual ImageView from resources
        Label iconLabel = new Label(iconText);
        iconLabel.getStyleClass().add("stat-icon-label");

        Label valueLabel = new Label(valueText);
        valueLabel.getStyleClass().add("stat-value-label");

        slot.getChildren().addAll(iconLabel, valueLabel);
        return slot;
    }

    /** Formats today's date as "Mar 15", etc. */
    private String formatTodayDate() {
        try {
            return new SimpleDateFormat("MMM d").format(new Date());
        } catch (Exception e) {
            return "";
        }
    }
}
