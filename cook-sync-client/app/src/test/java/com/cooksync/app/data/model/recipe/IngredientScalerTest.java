package com.cooksync.app.data.model.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.math.BigDecimal;

import org.junit.Test;

/**
 * Unit test suite for {@link IngredientScaler}'s scaling arithmetic.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class IngredientScalerTest {

    @Test
    public void scale_doublesQuantity_whenServingsDouble() {
        BigDecimal result = IngredientScaler.scale(new BigDecimal("2"), 4, 8);
        assertEquals(0, result.compareTo(new BigDecimal("4")));
    }

    @Test
    public void scale_halvesQuantity_whenServingsHalve() {
        BigDecimal result = IngredientScaler.scale(new BigDecimal("2"), 4, 2);
        assertEquals(0, result.compareTo(new BigDecimal("1")));
    }

    @Test
    public void scale_returnsSameInstance_whenServingsUnchanged() {
        BigDecimal quantity = new BigDecimal("1.5");
        assertSame(quantity, IngredientScaler.scale(quantity, 4, 4));
    }

    @Test
    public void scale_returnsNull_whenQuantityIsNull() {
        assertNull(IngredientScaler.scale(null, 4, 8));
    }

    @Test
    public void scale_returnsQuantityUnchanged_whenFromServingsNotPositive() {
        BigDecimal quantity = new BigDecimal("2");
        assertSame(quantity, IngredientScaler.scale(quantity, 0, 8));
    }

    @Test
    public void scale_handlesNonTerminatingDecimal_withoutThrowing() {
        // 1 / 3 servings ratio would repeat forever as a plain division; scale() must round.
        BigDecimal result = IngredientScaler.scale(new BigDecimal("1"), 3, 1);
        assertEquals(0, result.compareTo(new BigDecimal("0.3333")));
    }
}
