package eu.bigbank.mugloar.strategy;

import eu.bigbank.mugloar.client.GameApi;
import eu.bigbank.mugloar.client.MugloarApiException;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.domain.PurchaseResult;
import eu.bigbank.mugloar.domain.ShopItem;
import eu.bigbank.mugloar.domain.SolveResult;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameBot {

    private static final Logger log = LoggerFactory.getLogger(GameBot.class);
    // After this many errors in a row the API is probably down.
    static final int MAX_CONSECUTIVE_ERRORS = 5;

    private final GameApi api;
    private final AdEvaluator evaluator;
    private final ShopPolicy shopPolicy;
    private final int maxTurns;

    public GameBot(GameApi api, AdEvaluator evaluator, ShopPolicy shopPolicy, int maxTurns) {
        this.api = api;
        this.evaluator = evaluator;
        this.shopPolicy = shopPolicy;
        this.maxTurns = maxTurns;
    }

    public GameState playNewGame(BotListener listener, BooleanSupplier stopRequested) {
        return play(GameState.from(api.startGame()), listener, stopRequested);
    }

    public GameState play(GameState start, BotListener listener, BooleanSupplier stopRequested) {
        GameState state = start;
        listener.onEvent(new BotEvent.Started(state));
        List<ShopItem> shop = api.getShop(state.gameId());
        Set<String> bought = new HashSet<>();
        int errors = 0;

        while (!state.isOver()) {
            if (stopRequested.getAsBoolean()) {
                return finish(state, "Stopped", listener);
            }
            if (state.turn() >= maxTurns) {
                return finish(state, "Turn limit reached", listener);
            }
            try {
                state = shopIfWorthIt(state, shop, bought, listener);
                state = takeTurn(state, shop, listener);
                errors = 0;
            } catch (MugloarApiException e) {
                if (e.isGameOver()) {
                    return finish(state.withNoLives(), "Game over", listener);
                }
                if (++errors >= MAX_CONSECUTIVE_ERRORS) {
                    return finish(state, "Game API keeps failing: " + e.getMessage(), listener);
                }
                // Usually the ad expired. Reload the board and try again.
                log.debug("Turn failed, retrying: {}", e.getMessage());
            } catch (NoAdsException e) {
                return finish(state, "No ads left to solve", listener);
            }
        }
        return finish(state, "Game over", listener);
    }

    private GameState shopIfWorthIt(GameState state, List<ShopItem> shop, Set<String> bought, BotListener listener) {
        Optional<ShopItem> choice;
        while ((choice = shopPolicy.choose(state, shop, bought)).isPresent()) {
            ShopItem item = choice.get();
            int goldBefore = state.gold();
            state = buy(state, item, listener);
            if (state.gold() >= goldBefore) {
                break; // purchase was refused
            }
            bought.add(item.id());
        }
        return state;
    }

    private GameState buy(GameState state, ShopItem item, BotListener listener) {
        PurchaseResult result = api.buy(state.gameId(), item.id());
        GameState next = state.apply(result);
        listener.onEvent(new BotEvent.Purchased(next, item.id(), item.name(), result.shoppingSuccess()));
        return next;
    }

    private GameState takeTurn(GameState state, List<ShopItem> shop, BotListener listener) {
        List<AdAdvice> ads = evaluator.advise(api.getAds(state.gameId()), state.lives());
        AdAdvice pick = ads.stream().filter(AdAdvice::recommended).findFirst().orElseThrow(NoAdsException::new);
        if (!evaluator.isWorthTheRisk(pick, state.lives())) {
            Optional<ShopItem> wait = shopPolicy.waitingMove(state, shop);
            if (wait.isPresent()) {
                return buy(state, wait.get(), listener);
            }
        }
        SolveResult result = api.solve(state.gameId(), pick.ad().adId());
        state = state.apply(result);
        listener.onEvent(new BotEvent.Solved(state, pick.ad().adId(), pick.ad().message(), pick.ad().probability(),
                pick.ad().reward(), result.success(), result.message()));
        return state;
    }

    private static GameState finish(GameState state, String reason, BotListener listener) {
        listener.onEvent(new BotEvent.Finished(state, reason));
        return state;
    }

    private static final class NoAdsException extends RuntimeException {
        NoAdsException() {
            super(null, null, false, false);
        }
    }
}
