package com.cooksync.app.data.model.recipe;

import java.math.BigDecimal;

import com.dtos.response.unit.UnitResponse;

/**
 * Picks a measurement unit's singular or plural display name to match a quantity — e.g. "1
 * Gram" but "200 Grams" — so a scaled ingredient reads grammatically correctly regardless of how
 * many servings are selected. {@link UnitResponse#name()} and {@link UnitResponse#namePlural()}
 * are both already resolved to the request's locale server-side; this class only chooses between
 * the two, it does no translation itself.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public final class UnitDisplayFormatter {

    private UnitDisplayFormatter() {
    }

    /**
     * Chooses the singular or plural display name for a unit, based on a quantity.
     *
     * @param unit the unit to name, or {@code null}
     * @param quantity the quantity paired with this unit (typically already {@link
     *                 IngredientScaler}-scaled); exactly {@code 1} selects the singular form,
     *                 every other value (including {@code null}, {@code 0}, and fractions)
     *                 selects the plural
     * @return the chosen display name, or {@code ""} if {@code unit} is {@code null}
     */
    public static String displayName(UnitResponse unit, BigDecimal quantity) {
        if (unit == null) {
            return "";
        }
        boolean isExactlyOne = quantity != null && quantity.compareTo(BigDecimal.ONE) == 0;
        return isExactlyOne ? unit.name() : unit.namePlural();
    }
}
