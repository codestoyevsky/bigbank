package eu.bigbank.mugloar.api;

import eu.bigbank.mugloar.strategy.AdAdvice;
import eu.bigbank.mugloar.strategy.RiskLevel;

public record AdView(
        String adId,
        String message,
        int reward,
        int expiresIn,
        String probability,
        double successChance,
        double expectedValue,
        RiskLevel risk,
        boolean recommended,
        String warning) {

    static AdView of(AdAdvice a) {
        return new AdView(a.ad().adId(), a.ad().message(), a.ad().reward(), a.ad().expiresIn(), a.ad().probability(),
                a.successChance(), Math.round(a.expectedValue() * 10) / 10.0, a.risk(), a.recommended(), a.warning());
    }
}
