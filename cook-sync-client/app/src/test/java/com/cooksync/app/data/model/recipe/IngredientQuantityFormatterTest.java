package com.cooksync.app.data.model.recipe;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

/**
 * Unit test suite for {@link IngredientQuantityFormatter}'s fraction-friendly display formatting.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class IngredientQuantityFormatterTest {

    @Test
    public void format_rendersWholeNumber_withNoFraction() {
        assertEquals("3", IngredientQuantityFormatter.format(new BigDecimal("3")));
    }

    @Test
    public void format_rendersBareFraction_belowOne() {
        assertEquals("¾", IngredientQuantityFormatter.format(new BigDecimal("0.75")));
    }

    @Test
    public void format_rendersWholeNumberPlusFraction() {
        assertEquals("1½", IngredientQuantityFormatter.format(new BigDecimal("1.5")));
    }

    @Test
    public void format_recognizesThirds() {
        assertEquals("2⅓", IngredientQuantityFormatter.format(new BigDecimal("2.3333")));
        assertEquals("2⅔", IngredientQuantityFormatter.format(new BigDecimal("2.6667")));
    }

    @Test
    public void format_recognizesEighths() {
        assertEquals("⅛", IngredientQuantityFormatter.format(new BigDecimal("0.125")));
        assertEquals("⅝", IngredientQuantityFormatter.format(new BigDecimal("0.625")));
    }

    @Test
    public void format_fallsBackToDecimal_whenNoFractionIsCloseEnough() {
        assertEquals("2.17", IngredientQuantityFormatter.format(new BigDecimal("2.17")));
    }

    @Test
    public void format_returnsEmptyString_forNull() {
        assertEquals("", IngredientQuantityFormatter.format(null));
    }

    @Test
    public void format_toleratesSmallRoundingArtifacts_fromScaling() {
        // Simulates a scale() result like 2 * 7 / 3 = 4.6667, which should still read as 4⅔.
        assertEquals("4⅔", IngredientQuantityFormatter.format(new BigDecimal("4.6667")));
    }
}
