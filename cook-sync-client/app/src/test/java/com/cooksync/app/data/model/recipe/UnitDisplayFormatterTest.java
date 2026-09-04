package com.cooksync.app.data.model.recipe;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import com.dtos.response.unit.UnitResponse;

/**
 * Unit test suite for {@link UnitDisplayFormatter}'s singular/plural selection.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class UnitDisplayFormatterTest {

    private static final UnitResponse GRAM = new UnitResponse("u1", "g", "Gram", "Grams", null, null);

    @Test
    public void displayName_returnsSingular_forExactlyOne() {
        assertEquals("Gram", UnitDisplayFormatter.displayName(GRAM, BigDecimal.ONE));
    }

    @Test
    public void displayName_returnsSingular_forOneWithTrailingZeros() {
        assertEquals("Gram", UnitDisplayFormatter.displayName(GRAM, new BigDecimal("1.0000")));
    }

    @Test
    public void displayName_returnsPlural_forZero() {
        assertEquals("Grams", UnitDisplayFormatter.displayName(GRAM, BigDecimal.ZERO));
    }

    @Test
    public void displayName_returnsPlural_forGreaterThanOne() {
        assertEquals("Grams", UnitDisplayFormatter.displayName(GRAM, new BigDecimal("200")));
    }

    @Test
    public void displayName_returnsPlural_forFraction() {
        assertEquals("Grams", UnitDisplayFormatter.displayName(GRAM, new BigDecimal("0.5")));
    }

    @Test
    public void displayName_returnsPlural_forNullQuantity() {
        assertEquals("Grams", UnitDisplayFormatter.displayName(GRAM, null));
    }

    @Test
    public void displayName_returnsEmptyString_forNullUnit() {
        assertEquals("", UnitDisplayFormatter.displayName(null, BigDecimal.ONE));
    }
}
