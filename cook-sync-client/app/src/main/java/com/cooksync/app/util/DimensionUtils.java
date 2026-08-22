package com.cooksync.app.util;

import android.content.Context;

/**
 * Utility class for density-independent pixel (dp) to pixel conversions.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 22/08/2026
 */
public final class DimensionUtils {

    private DimensionUtils() {
    }

    /**
     * Converts a dp value to pixels using the given context's display density.
     *
     * @param context context supplying the display metrics
     * @param dp the dimension in density-independent pixels
     * @return the equivalent pixel value, rounded to the nearest integer
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
