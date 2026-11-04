package eu.bigbank.mugloar.service;

import eu.bigbank.mugloar.client.GameApi;
import eu.bigbank.mugloar.config.MugloarProperties;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.domain.PurchaseResult;
import eu.bigbank.mugloar.domain.Reputation;
import eu.bigbank.mugloar.domain.ShopItem;
import eu.bigbank.mugloar.domain.SolveResult;
import eu.bigbank.mugloar.strategy.AdAdvice;
import eu.bigbank.mugloar.strategy.AdEvaluator;
import eu.bigbank.mugloar.strategy.BotListener;
import eu.bigbank.mugloar.strategy.GameBot;
import eu.bigbank.mugloar.strategy.ShopPolicy;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    // Used for advice when we don't know the game (after a restart).
    static final int DEFAULT_LIVES = 3;

    private final GameApi api;
    private final GameRegistry registry;
    private final AdEvaluator evaluator;
    private final ShopPolicy shopPolicy;
    private final int maxTurns;

    public GameService(GameApi api, GameRegistry registry, AdEvaluator evaluator, ShopPolicy shopPolicy,
            MugloarProperties props) {
        this.api = api;
        this.registry = registry;
        this.evaluator = evaluator;
        this.shopPolicy = shopPolicy;
        this.maxTurns = props.bot().maxTurns();
    }

    public GameState start() {
        return registry.save(GameState.from(api.startGame()));
    }

    public List<AdAdvice> ads(String gameId) {
        int lives = registry.find(gameId).map(GameState::lives).orElse(DEFAULT_LIVES);
        return evaluator.advise(api.getAds(gameId), lives);
    }

    public SolveResult solve(String gameId, String adId) {
        requireNoBot(gameId);
        SolveResult result = api.solve(gameId, adId);
        registry.update(gameId, s -> s.apply(result));
        return result;
    }

    public List<ShopItem> shop(String gameId) {
        return api.getShop(gameId);
    }

    public PurchaseResult buy(String gameId, String itemId) {
        requireNoBot(gameId);
        PurchaseResult result = api.buy(gameId, itemId);
        registry.update(gameId, s -> s.apply(result));
        return result;
    }

    public Reputation reputation(String gameId) {
        requireNoBot(gameId);
        Reputation reputation = api.investigate(gameId);
        registry.update(gameId, GameState::nextTurn);
        return reputation;
    }

    /** Called before the event stream opens, so these errors still get a normal HTTP status. */
    public GameState reserveAutoplay(String gameId) {
        GameState start = registry.get(gameId);
        if (start.isOver()) {
            throw new GameNotFoundException(gameId);
        }
        registry.startAutoplay(gameId);
        return start;
    }

    public GameState runAutoplay(GameState start, BotListener listener, BooleanSupplier stopRequested) {
        try {
            GameBot bot = new GameBot(api, evaluator, shopPolicy, maxTurns);
            return bot.play(start, event -> {
                registry.save(event.state());
                listener.onEvent(event);
            }, stopRequested);
        } finally {
            registry.stopAutoplay(start.gameId());
        }
    }

    private void requireNoBot(String gameId) {
        if (registry.isAutoplaying(gameId)) {
            throw new AutoplayConflictException(gameId);
        }
    }
}
