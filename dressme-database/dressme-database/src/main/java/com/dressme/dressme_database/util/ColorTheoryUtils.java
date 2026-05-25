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
    //Constnte para nivelar la Saturación y Luminosidad (0-100) al nivel
    //del Hue (180)
    private static final double HSL_SCALE_FACTOR = 1.8;

    //Scors de armonía cromática
    public static final double ANALOGOUS_SCORE     = 0.80;
    public static final double COMPLEMENTARY_SCORE = 0.70;
    public static final double NO_HARMONY_SCORE    = 0.0;

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
        int circularDistance = ColorTheoryUtils.getCircularHueDistance(currentHue, targetHue);
        return circularDistance <= HUE_TOLERANCE;
    }

    public static double calculateColorDistance(
        int targetHue, int targetSaturation, int targetLightness,
        int dbHue, int dbSaturation, int dbLightness){

        int deltaH = ColorTheoryUtils.getCircularHueDistance(targetHue, dbHue);
        double deltaS = Math.abs(targetSaturation -  dbSaturation) * HSL_SCALE_FACTOR;
        double deltaL = Math.abs(targetLightness -  dbLightness) * HSL_SCALE_FACTOR;
        return Math.sqrt(Math.pow(deltaH, 2) + Math.pow(deltaS, 2) + Math.pow(deltaL, 2));
    }

    public static int getCircularHueDistance(int hue1, int hue2){
        int diff = Math.abs(hue1 - hue2);
        return Math.min(diff, MAX_HUE - diff);
    }

    /*
      *Calcula score de compatibilidad basado en la teoría del color
      * cuando no existe un registro en la db.
     */
    public static double calculateHarmonyScore(int hue1, int hue2) {
        int complementary = getComplementaryHue(hue1);
        int[] analogous   = getAnalogousHues(hue1);

        if (isWithinTolerance(hue2, complementary)) {
            return COMPLEMENTARY_SCORE;
        }
        if (isWithinTolerance(hue2, analogous[0]) || isWithinTolerance(hue2, analogous[1])) {
            return ANALOGOUS_SCORE;
        }
        return NO_HARMONY_SCORE;
    }
}
