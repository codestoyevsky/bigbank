package eu.bigbank.mugloar.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProbabilityTest {

    @ParameterizedTest
    @ValueSource(strings = {"Hmmm....", "Hmmm...", "hmmm", " HMMM.... "})
    void matchesLabelsLeniently(String label) {
        assertThat(Probability.fromLabel(label)).contains(Probability.HMMM);
    }

    @Test
    void unknownLabelsGetAConservativeChance() {
        assertThat(Probability.chanceOf("Totally new label")).isEqualTo(Probability.UNKNOWN_CHANCE);
        assertThat(Probability.chanceOf(null)).isEqualTo(Probability.UNKNOWN_CHANCE);
    }

    @Test
    void chancesDecreaseWithDifficulty() {
        Probability[] all = Probability.values();
        for (int i = 1; i < all.length; i++) {
            assertThat(all[i].chance()).isLessThan(all[i - 1].chance());
        }
    }
}
