package eu.bigbank.mugloar.strategy;

public enum RiskLevel {
    SAFE,
    MODERATE,
    RISKY,
    DEADLY;

    static RiskLevel of(double chance) {
        if (chance >= 0.85) return SAFE;
        if (chance >= 0.6) return MODERATE;
        if (chance >= 0.4) return RISKY;
        return DEADLY;
    }
}
