package utils;

/**
 * Global temperature unit state and conversion utility.
 *
 * All NWS data arrives in °F. This class holds the current display
 * unit and converts on demand. The toggle is app-wide — one call
 * affects all labels that use convert().
 *
 * Usage:
 *   TempConverter.convert(period.temperature)  → display int
 *   TempConverter.symbol()                      → "°F" or "°C"
 *   TempConverter.toggle()                      → flip unit
 */
public class TempConverter {

    public enum Unit { FAHRENHEIT, CELSIUS }

    // Default unit matches the NWS source data
    private static Unit current = Unit.FAHRENHEIT;

    // ---------------------------------------------------------------
    // Unit control
    // ---------------------------------------------------------------

    /** Toggles between °F and °C app-wide. */
    public static void toggle() {
        current = (current == Unit.FAHRENHEIT) ? Unit.CELSIUS : Unit.FAHRENHEIT;
    }

    /** Sets the unit explicitly. */
    public static void setUnit(Unit unit) {
        current = unit;
    }

    /** Returns the currently active unit. */
    public static Unit getUnit() {
        return current;
    }

    // ---------------------------------------------------------------
    // Conversion
    // ---------------------------------------------------------------

    /**
     * Converts a temperature value from °F to the current display unit.
     *
     * @param tempF temperature in Fahrenheit (as stored in Period.temperature)
     * @return      integer value in the current display unit
     */
    public static int convert(int tempF) {
        if (current == Unit.CELSIUS) {
            return (int) Math.round((tempF - 32) * 5.0 / 9.0);
        }
        return tempF;
    }

    /**
     * Returns the degree symbol + unit character for the current unit.
     * e.g. "°F" or "°C"
     */
    public static String symbol() {
        return current == Unit.FAHRENHEIT ? "°F" : "°C";
    }

    /**
     * Convenience: formats a temperature with its unit symbol.
     * e.g. "72°F" or "22°C"
     */
    public static String format(int tempF) {
        return convert(tempF) + symbol();
    }
}
