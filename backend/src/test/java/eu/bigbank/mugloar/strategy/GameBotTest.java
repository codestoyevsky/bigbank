package eu.bigbank.mugloar.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bigbank.mugloar.client.GameApi;
import eu.bigbank.mugloar.client.MugloarApiException;
import eu.bigbank.mugloar.domain.Ad;
import eu.bigbank.mugloar.domain.Game;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.domain.PurchaseResult;
import eu.bigbank.mugloar.domain.Reputation;
import eu.bigbank.mugloar.domain.ShopItem;
import eu.bigbank.mugloar.domain.SolveResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class GameBotTest {

    /** In-memory game: every solve succeeds unless the ad id starts with "fail". */
    public static class FakeGameApi implements GameApi {
        int lives = 3, gold = 0, level = 0, score = 0, turn = 0;
        public Function<Integer, List<Ad>> board = t -> List.of(new Ad("ad" + t, "m", 50, 5, null, "Sure thing"));
        public final List<String> calls = new ArrayList<>();
        final Deque<RuntimeException> failures = new ArrayDeque<>();

        @Override
        public Game startGame() {
            return new Game("g1", lives, gold, level, score, 0, turn);
        }

        @Override
        public List<Ad> getAds(String gameId) {
            if (!failures.isEmpty()) {
                throw failures.pop();
            }
            return board.apply(turn);
        }

        @Override
        public SolveResult solve(String gameId, String adId) {
            calls.add("solve " + adId);
            turn++;
            boolean ok = !adId.startsWith("fail");
            if (ok) {
                gold += 50;
                score += 50;
            } else {
                lives--;
            }
            return new SolveResult(ok, lives, gold, score, 0, turn, ok ? "yay" : "nay");
        }

        @Override
        public List<ShopItem> getShop(String gameId) {
            return ShopPolicyTest.SHOP;
        }

        @Override
        public PurchaseResult buy(String gameId, String itemId) {
            calls.add("buy " + itemId);
            turn++;
            int cost = ShopPolicyTest.SHOP.stream().filter(i -> i.id().equals(itemId)).findFirst().orElseThrow().cost();
            gold -= cost;
            if (itemId.equals("hpot")) {
                lives++;
            } else {
                level++;
            }
            return new PurchaseResult(true, gold, lives, level, turn);
        }

        @Override
        public Reputation investigate(String gameId) {
            return new Reputation(0, 0, 0);
        }
    }

    private final FakeGameApi api = new FakeGameApi();
    private final List<BotEvent> events = new ArrayList<>();

    private GameBot bot(int maxTurns) {
        return new GameBot(api, new AdEvaluator(), new ShopPolicy(), maxTurns);
    }

    @Test
    void playsUntilTurnLimitAndReportsEveryStep() {
        GameState end = bot(20).playNewGame(events::add, () -> false);

        assertThat(end.turn()).isEqualTo(20);
        assertThat(end.score()).isPositive();
        assertThat(events.getFirst()).isInstanceOf(BotEvent.Started.class);
        assertThat(events.getLast()).isInstanceOfSatisfying(BotEvent.Finished.class,
                f -> assertThat(f.reason()).isEqualTo("Turn limit reached"));
        assertThat(api.calls).contains("buy cs"); // spends surplus gold on upgrades
    }

    @Test
    void stopsWhenOutOfLives() {
        api.board = t -> List.of(new Ad("fail" + t, "m", 50, 5, null, "Sure thing"));

        GameState end = bot(100).playNewGame(events::add, () -> false);

        assertThat(end.lives()).isZero();
        assertThat(api.calls).containsExactly("solve fail0", "solve fail1", "solve fail2");
    }

    @Test
    void buysPotionInsteadOfGamblingWhenBoardIsRisky() {
        api.gold = 100;
        api.board = t -> t == 0
                ? List.of(new Ad("risky", "m", 300, 5, null, "Playing with fire"))
                : List.of(new Ad("safe" + t, "m", 50, 5, null, "Sure thing"));

        bot(2).playNewGame(events::add, () -> false);

        assertThat(api.calls).containsExactly("buy hpot", "solve safe1");
    }

    @Test
    void retriesAfterTransientErrorsAndGivesUpAfterTooMany() {
        api.failures.add(new MugloarApiException(400, "Bad Request"));
        GameState end = bot(1).playNewGame(events::add, () -> false);
        assertThat(end.turn()).isEqualTo(1);

        FakeGameApi broken = new FakeGameApi();
        for (int i = 0; i < GameBot.MAX_CONSECUTIVE_ERRORS; i++) {
            broken.failures.add(new MugloarApiException(503, "down"));
        }
        List<BotEvent> brokenEvents = new ArrayList<>();
        new GameBot(broken, new AdEvaluator(), new ShopPolicy(), 10).playNewGame(brokenEvents::add, () -> false);
        assertThat(brokenEvents.getLast()).isInstanceOfSatisfying(BotEvent.Finished.class,
                f -> assertThat(f.reason()).startsWith("Game API keeps failing"));
    }

    @Test
    void endsOnGameOverFromApi() {
        api.failures.add(new MugloarApiException(410, "Gone"));

        GameState end = bot(10).playNewGame(events::add, () -> false);

        assertThat(end.isOver()).isTrue();
    }

    @Test
    void canBeStopped() {
        GameState end = bot(100).playNewGame(events::add, () -> true);

        assertThat(end.turn()).isZero();
        assertThat(events.getLast()).isInstanceOfSatisfying(BotEvent.Finished.class,
                f -> assertThat(f.reason()).isEqualTo("Stopped"));
    }

    @Test
    void endsWhenBoardIsEmpty() {
        api.board = t -> List.of();

        bot(10).playNewGame(events::add, () -> false);

        assertThat(events.getLast()).isInstanceOfSatisfying(BotEvent.Finished.class,
                f -> assertThat(f.reason()).isEqualTo("No ads left to solve"));
    }
}
