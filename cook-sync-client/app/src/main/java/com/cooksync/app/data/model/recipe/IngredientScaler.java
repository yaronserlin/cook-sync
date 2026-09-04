package com.cooksync.app.data.model.recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure arithmetic for scaling an ingredient's quantity when the user changes a recipe's serving
 * count on the recipe detail screen — {@code quantity * (toServings / fromServings)}. Uses
 * {@link BigDecimal} throughout rather than {@code double}, deliberately: {@code
 * IngredientResponse#quantity()} is already a {@code BigDecimal} on the wire, and multiplying by
 * a serving ratio is exactly the kind of arithmetic that makes binary floating-point rounding
 * error (e.g. {@code 0.1 + 0.2 != 0.3}) visible to a user for the first time if {@code double}
 * were used instead.
 *
 * <p>This is a purely client-side, ephemeral display calculation — nothing here is sent to the
 * server or persisted; {@code servings}/{@code quantity} continue to be submitted and stored
 * exactly as before.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public final class IngredientScaler {

    /** Decimal places kept internally before display formatting rounds/simplifies further. */
    private static final int SCALE = 4;

    private IngredientScaler() {
    }

    /**
     * Scales an ingredient quantity from one serving count to another.
     *
     * @param quantity the quantity as written for {@code fromServings}, or {@code null}
     * @param fromServings the recipe's original serving count
     * @param toServings the serving count to scale to
     * @return the scaled quantity, or {@code quantity} unchanged if it's {@code null} or either
     *         serving count isn't positive (defensive — both are expected to always be positive)
     */
    public static BigDecimal scale(BigDecimal quantity, int fromServings, int toServings) {
        if (quantity == null || fromServings <= 0 || toServings <= 0) {
            return quantity;
        }
        if (fromServings == toServings) {
            return quantity;
        }
        return quantity.multiply(BigDecimal.valueOf(toServings))
                .divide(BigDecimal.valueOf(fromServings), SCALE, RoundingMode.HALF_UP);
    }
}
