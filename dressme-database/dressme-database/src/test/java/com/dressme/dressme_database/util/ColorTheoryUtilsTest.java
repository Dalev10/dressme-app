package com.dressme.dressme_database.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorTheoryUtilsTest {

    @Test
    @DisplayName("Colores complementarios (180° de diferencia) retornan score 0.70")
    void complementaryColors_returnComplementaryScore() {
        double score = ColorTheoryUtils.calculateHarmonyScore(0, 180);
        assertEquals(ColorTheoryUtils.COMPLEMENTARY_SCORE, score);
    }

    @Test
    @DisplayName("Colores análogos (+30°) retornan score 0.80")
    void analogousColors_returnAnalogousScore() {
        double score = ColorTheoryUtils.calculateHarmonyScore(0, 30);
        assertEquals(ColorTheoryUtils.ANALOGOUS_SCORE, score);
    }

    @Test
    @DisplayName("Colores análogos (-30°) retornan score 0.80")
    void analogousColorsMinus_returnAnalogousScore() {
        double score = ColorTheoryUtils.calculateHarmonyScore(0, 330);
        assertEquals(ColorTheoryUtils.ANALOGOUS_SCORE, score);
    }

    @Test
    @DisplayName("Colores sin armonía retornan score 0.0")
    void noHarmonyColors_returnZeroScore() {
        double score = ColorTheoryUtils.calculateHarmonyScore(0, 90);
        assertEquals(ColorTheoryUtils.NO_HARMONY_SCORE, score);
    }

    @Test
    @DisplayName("El complementario de 0° es 180°")
    void complementaryOf0_is180() {
        assertEquals(180, ColorTheoryUtils.getComplementaryHue(0));
    }

    @Test
    @DisplayName("El complementario de 350° es 170°")
    void complementaryHandlesWrapAround() {
        assertEquals(170, ColorTheoryUtils.getComplementaryHue(350));
    }
}
