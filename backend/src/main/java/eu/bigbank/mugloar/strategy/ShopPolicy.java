package eu.bigbank.mugloar.strategy;

import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.domain.ShopItem;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Low on lives: buy a potion. Otherwise buy the cheapest upgrade we can afford while keeping gold for a potion.
 * Upgrades raise the level, which makes the better-paying ads succeed more often.
 */
public class ShopPolicy {

    public static final String HEALING_POTION = "hpot";
    static final int LOW_LIVES = 3;

    /** When no ad is safe enough. A potion costs a turn, so the board changes, and we get a spare life. */
    public Optional<ShopItem> waitingMove(GameState state, List<ShopItem> shop) {
        return shop.stream()
                .filter(i -> HEALING_POTION.equals(i.id()))
                .filter(i -> state.gold() >= i.cost())
                .findFirst();
    }

    public Optional<ShopItem> choose(GameState state, List<ShopItem> shop, Set<String> alreadyBought) {
        Optional<ShopItem> potion = shop.stream().filter(i -> HEALING_POTION.equals(i.id())).findFirst();

        if (state.lives() < LOW_LIVES && potion.isPresent() && state.gold() >= potion.get().cost()) {
            return potion;
        }

        int reserve = potion.map(ShopItem::cost).orElse(0);
        return shop.stream()
                .filter(i -> !HEALING_POTION.equals(i.id()))
                .filter(i -> state.gold() >= i.cost() + reserve)
                .min(Comparator.comparing((ShopItem i) -> alreadyBought.contains(i.id()))
                        .thenComparingInt(ShopItem::cost)
                        .thenComparing(ShopItem::id));
    }
}
