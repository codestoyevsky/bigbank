package eu.bigbank.mugloar.strategy;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The game only gives labels, no numbers. These chances are rough estimates, checked against a few test games. */
public enum Probability {
    SURE_THING("Sure thing", 0.97),
    PIECE_OF_CAKE("Piece of cake", 0.92),
    WALK_IN_THE_PARK("Walk in the park", 0.88),
    QUITE_LIKELY("Quite likely", 0.78),
    HMMM("Hmmm....", 0.62),
    GAMBLE("Gamble", 0.5),
    RISKY("Risky", 0.42),
    RATHER_DETRIMENTAL("Rather detrimental", 0.33),
    PLAYING_WITH_FIRE("Playing with fire", 0.25),
    SUICIDE_MISSION("Suicide mission", 0.15),
    IMPOSSIBLE("Impossible", 0.05);

    // A label we haven't seen before: assume it's risky.
    public static final double UNKNOWN_CHANCE = 0.1;

    private final String label;
    private final double chance;

    Probability(String label, double chance) {
        this.label = label;
        this.chance = chance;
    }

    public String label() {
        return label;
    }

    public double chance() {
        return chance;
    }

    public static Optional<Probability> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String normalized = normalize(label);
        return Arrays.stream(values()).filter(p -> normalize(p.label).equals(normalized)).findFirst();
    }

    public static double chanceOf(String label) {
        return fromLabel(label).map(Probability::chance).orElse(UNKNOWN_CHANCE);
    }

    private static String normalize(String s) {
        // "Hmmm...." sometimes shows up with a different number of dots.
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\.+$", "");
    }
}
