package com.dressme.dressme_database.util;

public class ColorTheoryUtils {
    private static final int MAX_HUE = 360;
    private static final int COMPLEMENTARY_SHIFT = 180;
    private static final int ANALOGOUS_SHIFT = 30;
    /* Margen de toleracia para encontrar un color similar en la DB
        Ej: se busca un amarillo con Hue 65, pero en la DB tenemos
        amarillo con Hue 60
    */
    private static final int HUE_TOLERANCE = 15;

    private ColorTheoryUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static int getComplementaryHue(int baseHue) {
        return (baseHue + COMPLEMENTARY_SHIFT) % MAX_HUE;
    }

    public static int[] getAnalogousHues(int baseHue) {
        int upper = (baseHue + ANALOGOUS_SHIFT) % MAX_HUE;
        int lower = (baseHue - ANALOGOUS_SHIFT + MAX_HUE) % MAX_HUE;
        return new int[]{upper, lower};
    }

    public static boolean isWithinTolerance(int currentHue, int targetHue) {
        int diff = Math.abs(currentHue - targetHue);
        int circularDistance = Math.min(diff, MAX_HUE - diff);
        return circularDistance <= HUE_TOLERANCE;
    }
}
