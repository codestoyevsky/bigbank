package eu.bigbank.mugloar.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.domain.ShopItem;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ShopPolicyTest {

    public static final List<ShopItem> SHOP = List.of(
            new ShopItem("hpot", "Healing potion", 50),
            new ShopItem("cs", "Claw Sharpening", 100),
            new ShopItem("gas", "Gasoline", 100),
            new ShopItem("ch", "Claw Honing", 300));

    private final ShopPolicy policy = new ShopPolicy();

    private static GameState state(int lives, int gold) {
        return new GameState("g", lives, gold, 0, 0, 0);
    }

    private String choose(int lives, int gold, Set<String> bought) {
        return policy.choose(state(lives, gold), SHOP, bought).map(ShopItem::id).orElse(null);
    }

    @Test
    void buysPotionWhenLowOnLives() {
        assertThat(choose(2, 60, Set.of())).isEqualTo("hpot");
    }

    @Test
    void buysNothingWhenPoor() {
        assertThat(choose(3, 40, Set.of())).isNull();
        assertThat(choose(1, 40, Set.of())).isNull();
    }

    @Test
    void keepsAPotionReserveWhenUpgrading() {
        assertThat(choose(3, 120, Set.of())).isNull();
        assertThat(choose(3, 150, Set.of())).isEqualTo("cs");
    }

    @Test
    void prefersUpgradesNotBoughtYet() {
        assertThat(choose(3, 150, Set.of("cs"))).isEqualTo("gas");
        assertThat(choose(3, 150, Set.of("cs", "gas"))).isEqualTo("cs");
    }

    @Test
    void waitingMoveIsAPotionIfAffordable() {
        assertThat(policy.waitingMove(state(3, 50), SHOP)).map(ShopItem::id).contains("hpot");
        assertThat(policy.waitingMove(state(3, 49), SHOP)).isEmpty();
    }
}
