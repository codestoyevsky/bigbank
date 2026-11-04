package eu.bigbank.mugloar.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bigbank.mugloar.domain.Ad;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdEvaluatorTest {

    private final AdEvaluator evaluator = new AdEvaluator();

    private static Ad ad(String id, int reward, String probability) {
        return ad(id, reward, probability, 5);
    }

    private static Ad ad(String id, int reward, String probability, int expiresIn) {
        return new Ad(id, "msg " + id, reward, expiresIn, null, probability);
    }

    private static String recommendedId(List<AdAdvice> advice) {
        return advice.stream().filter(AdAdvice::recommended).findFirst().orElseThrow().ad().adId();
    }

    @Test
    void computesExpectedValueAndRisk() {
        AdAdvice a = evaluator.evaluate(ad("a", 100, "Gamble"));

        assertThat(a.successChance()).isEqualTo(0.5);
        assertThat(a.expectedValue()).isEqualTo(50.0);
        assertThat(a.risk()).isEqualTo(RiskLevel.RISKY);
    }

    @Test
    void picksHighestExpectedValueAmongSafeEnoughAds() {
        List<AdAdvice> advice = evaluator.advise(List.of(
                ad("cheap", 10, "Sure thing"),
                ad("good", 60, "Walk in the park"),
                ad("tempting", 500, "Suicide mission")), 3);

        assertThat(recommendedId(advice)).isEqualTo("good");
        assertThat(advice).first().extracting(a -> a.ad().adId()).isEqualTo("good");
        assertThat(advice).filteredOn(AdAdvice::recommended).hasSize(1);
    }

    @Test
    void isStricterOnTheLastLife() {
        List<Ad> ads = List.of(ad("likely", 100, "Quite likely"), ad("sure", 40, "Sure thing"));

        assertThat(recommendedId(evaluator.advise(ads, 3))).isEqualTo("likely");
        assertThat(recommendedId(evaluator.advise(ads, 1))).isEqualTo("sure");
    }

    @Test
    void fallsBackToSafestAdWhenNothingClearsTheThreshold() {
        List<AdAdvice> advice = evaluator.advise(List.of(
                ad("fire", 300, "Playing with fire"),
                ad("gamble", 50, "Gamble")), 3);

        AdAdvice pick = advice.getFirst();
        assertThat(pick.ad().adId()).isEqualTo("gamble");
        assertThat(evaluator.isWorthTheRisk(pick, 3)).isFalse();
    }

    @Test
    void prefersAdExpiringSoonerOnTie() {
        List<AdAdvice> advice = evaluator.advise(List.of(
                ad("later", 50, "Sure thing", 9),
                ad("sooner", 50, "Sure thing", 2)), 3);

        assertThat(recommendedId(advice)).isEqualTo("sooner");
    }

    @Test
    void decodesEncryptedAdsBeforeScoring() {
        Ad encrypted = new Ad("vq1", "Uryc", 50, 3, AdDecoder.ROT13, "Fher guvat");

        AdAdvice a = evaluator.advise(List.of(encrypted), 3).getFirst();

        assertThat(a.ad().adId()).isEqualTo("id1");
        assertThat(a.successChance()).isEqualTo(Probability.SURE_THING.chance());
    }

    @Test
    void discountsStealAdsAndWarnsAboutThem() {
        AdAdvice steal = evaluator.evaluate(new Ad("s", "Steal a goat from Bob", 100, 5, null, "Sure thing"));
        AdAdvice honest = evaluator.evaluate(ad("h", 100, "Sure thing"));

        assertThat(steal.successChance()).isCloseTo(0.97 * AdEvaluator.STEAL_PENALTY, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(steal.warning()).isNotBlank();
        assertThat(honest.warning()).isNull();
        assertThat(recommendedId(evaluator.advise(List.of(steal.ad(), honest.ad()), 3))).isEqualTo("h");
    }

    @Test
    void emptyBoardGivesNoAdvice() {
        assertThat(evaluator.advise(List.of(), 3)).isEmpty();
    }
}
