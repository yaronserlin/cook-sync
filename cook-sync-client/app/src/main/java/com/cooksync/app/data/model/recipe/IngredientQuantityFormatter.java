package com.cooksync.app.data.model.recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formats a (possibly {@link IngredientScaler}-scaled) ingredient quantity for display, using
 * common cooking fractions (½ ⅓ ¼ ...) instead of raw decimals — {@code "2.33 cups"} reads as a
 * rounding artifact, while {@code "2⅓ cups"} reads like a recipe a human wrote.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public final class IngredientQuantityFormatter {

    /** How close a fractional remainder must be to a known fraction to display as that glyph. */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.03");

    /** Ordered so the first close-enough match wins; values are eighths, sorted ascending. */
    private static final Map<BigDecimal, String> KNOWN_FRACTIONS = new LinkedHashMap<>();

    static {
        KNOWN_FRACTIONS.put(new BigDecimal("0.125"), "⅛"); // ⅛
        KNOWN_FRACTIONS.put(new BigDecimal("0.25"), "¼");  // ¼
        KNOWN_FRACTIONS.put(new BigDecimal("0.333"), "⅓"); // ⅓
        KNOWN_FRACTIONS.put(new BigDecimal("0.375"), "⅜"); // ⅜
        KNOWN_FRACTIONS.put(new BigDecimal("0.5"), "½");   // ½
        KNOWN_FRACTIONS.put(new BigDecimal("0.625"), "⅝"); // ⅝
        KNOWN_FRACTIONS.put(new BigDecimal("0.667"), "⅔"); // ⅔
        KNOWN_FRACTIONS.put(new BigDecimal("0.75"), "¾");  // ¾
        KNOWN_FRACTIONS.put(new BigDecimal("0.875"), "⅞"); // ⅞
    }

    private IngredientQuantityFormatter() {
    }

    /**
     * Formats a quantity as a whole number, a bare fraction glyph, a whole-number-plus-fraction
     * (e.g. {@code "2⅓"}), or — if it isn't close to any common cooking fraction — a plain
     * decimal, rounded to 2 places with trailing zeros stripped.
     *
     * @param quantity the quantity to format, or {@code null}
     * @return the formatted display string, or {@code ""} for {@code null}
     */
    public static String format(BigDecimal quantity) {
        if (quantity == null) {
            return "";
        }
        BigDecimal rounded = quantity.setScale(2, RoundingMode.HALF_UP);
        BigDecimal wholePart = rounded.setScale(0, RoundingMode.DOWN);
        BigDecimal remainder = rounded.subtract(wholePart).abs();

        if (remainder.signum() == 0) {
            return wholePart.stripTrailingZeros().toPlainString();
        }

        String fraction = closestFraction(remainder);
        if (fraction != null) {
            return wholePart.signum() == 0 ? fraction : wholePart.toPlainString() + fraction;
        }

        return rounded.stripTrailingZeros().toPlainString();
    }

    private static String closestFraction(BigDecimal remainder) {
        for (Map.Entry<BigDecimal, String> entry : KNOWN_FRACTIONS.entrySet()) {
            if (remainder.subtract(entry.getKey()).abs().compareTo(TOLERANCE) <= 0) {
                return entry.getValue();
            }
        }
        return null;
    }
}
