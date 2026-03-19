package utils;

/**
 * Global temperature unit state and conversion utility.
 */
public class TempConverter {
    public enum Unit { FAHRENHEIT, CELSIUS }
    // Default unit matches the NWS source data
    private static Unit current = Unit.FAHRENHEIT;

    // Toggles between °F and °C weatherIcons-wide.
    public static void toggle() {
        current = (current == Unit.FAHRENHEIT) ? Unit.CELSIUS : Unit.FAHRENHEIT;
    }

    // Sets the unit explicitly.
    public static void setUnit(Unit unit) {
        current = unit;
    }

    // Returns the currently active unit.
    public static Unit getUnit() {
        return current;
    }

    //Converts a temperature value from °F to the current display unit.
    public static int convert(int tempF) {
        if (current == Unit.CELSIUS) { return (int) Math.round((tempF - 32) * 5.0 / 9.0); }
        return tempF;
    }

    // Returns the degree symbol + unit character for the current unit.
    public static String symbol() {
        return current == Unit.FAHRENHEIT ? "°F" : "°C";
    }

    // Convenience: formats a temperature with its unit symbol.
    public static String format(int tempF) {
        return convert(tempF) + symbol();
    }
}
