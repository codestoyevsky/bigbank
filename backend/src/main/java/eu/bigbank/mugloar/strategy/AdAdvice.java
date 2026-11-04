package eu.bigbank.mugloar.strategy;

import eu.bigbank.mugloar.domain.Ad;

/** {@code recommended} marks the one ad the bot would pick now. */
public record AdAdvice(Ad ad, double successChance, double expectedValue, RiskLevel risk, boolean recommended,
        String warning) {

    AdAdvice withRecommended(boolean value) {
        return new AdAdvice(ad, successChance, expectedValue, risk, value, warning);
    }
}
