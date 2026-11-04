package eu.bigbank.mugloar.strategy;

import eu.bigbank.mugloar.domain.Ad;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the highest expected value (chance × reward) among ads that are safe enough. If none are, picks the safest.
 *
 * <p>Steal ads fail much more often than their label says: a "Sure thing" steal worked 14 of 25 times in my
 * test games. Hence {@link #STEAL_PENALTY}.
 */
public class AdEvaluator {

    static final double MIN_CHANCE = 0.75;
    static final double MIN_CHANCE_LAST_LIFE = 0.85;
    static final double STEAL_PENALTY = 0.6;
    static final String STEAL_WARNING =
            "Steal jobs fail more often than their label says and hurt your reputation.";

    private static final Comparator<AdAdvice> BY_VALUE = Comparator.comparingDouble(AdAdvice::expectedValue)
            .thenComparing(a -> -a.ad().expiresIn());
    private static final Comparator<AdAdvice> BY_SAFETY = Comparator.comparingDouble(AdAdvice::successChance)
            .thenComparing(BY_VALUE);

    public AdAdvice evaluate(Ad ad) {
        double chance = Probability.chanceOf(ad.probability());
        String warning = null;
        if (isSteal(ad)) {
            chance *= STEAL_PENALTY;
            warning = STEAL_WARNING;
        }
        return new AdAdvice(ad, chance, chance * ad.reward(), RiskLevel.of(chance), false, warning);
    }

    public List<AdAdvice> advise(List<Ad> ads, int lives) {
        List<AdAdvice> scored = ads.stream().map(AdDecoder::decode).map(this::evaluate).toList();
        Optional<AdAdvice> best = pick(scored, lives);
        return scored.stream()
                .map(a -> best.isPresent() && a == best.get() ? a.withRecommended(true) : a)
                .sorted(Comparator.comparing(AdAdvice::recommended).thenComparing(BY_VALUE).reversed())
                .toList();
    }

    public Optional<AdAdvice> pick(List<AdAdvice> ads, int lives) {
        double min = minChance(lives);
        return ads.stream()
                .filter(a -> a.successChance() >= min)
                .max(BY_VALUE)
                .or(() -> ads.stream().max(BY_SAFETY));
    }

    public boolean isWorthTheRisk(AdAdvice ad, int lives) {
        return ad.successChance() >= minChance(lives);
    }

    static boolean isSteal(Ad ad) {
        return ad.message() != null && ad.message().trim().toLowerCase(java.util.Locale.ROOT).startsWith("steal");
    }

    static double minChance(int lives) {
        return lives <= 1 ? MIN_CHANCE_LAST_LIFE : MIN_CHANCE;
    }
}
