package eu.bigbank.mugloar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bigbank.mugloar.config.MugloarProperties;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.strategy.AdEvaluator;
import eu.bigbank.mugloar.strategy.BotEvent;
import eu.bigbank.mugloar.strategy.GameBotTest;
import eu.bigbank.mugloar.strategy.ShopPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameServiceTest {

    private final GameBotTest.FakeGameApi api = new GameBotTest.FakeGameApi();
    private final GameRegistry registry = new GameRegistry();
    private final GameService service = new GameService(api, registry, new AdEvaluator(), new ShopPolicy(),
            new MugloarProperties(
                    new MugloarProperties.Api("http://x", Duration.ofSeconds(1), Duration.ofSeconds(1), 0),
                    new MugloarProperties.Web(List.of("http://localhost")),
                    new MugloarProperties.Bot(10)));

    @Test
    void tracksGameStateAcrossCalls() {
        GameState started = service.start();
        service.solve(started.gameId(), "ad0");
        service.buy(started.gameId(), "hpot");
        service.reputation(started.gameId());

        GameState now = registry.get(started.gameId());
        assertThat(now.score()).isEqualTo(50);
        assertThat(now.lives()).isEqualTo(4);
        assertThat(now.turn()).isEqualTo(3); // investigating uses a turn too
    }

    @Test
    void relearnsGamesItHasNotSeenFromMoveResults() {
        // e.g. the server restarted while the player kept the game open
        service.solve("g1", "ad0");

        assertThat(registry.get("g1")).extracting(GameState::lives, GameState::score).containsExactly(3, 50);
    }

    @Test
    void autoplayRequiresAKnownLiveGame() {
        assertThatThrownBy(() -> service.reserveAutoplay("nope")).isInstanceOf(GameNotFoundException.class);

        registry.save(new GameState("dead", 0, 0, 0, 0, 5));
        assertThatThrownBy(() -> service.reserveAutoplay("dead")).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void autoplayPlaysAndReleasesTheGame() {
        GameState started = service.start();
        List<BotEvent> events = new ArrayList<>();

        GameState end = service.runAutoplay(service.reserveAutoplay(started.gameId()), events::add, () -> false);

        assertThat(end.turn()).isBetween(10, 11); // a purchase may take the last turn past the cap
        assertThat(registry.get(started.gameId())).isEqualTo(end);
        assertThat(registry.isAutoplaying(started.gameId())).isFalse();
    }

    @Test
    void blocksManualMovesWhileTheBotPlays() {
        GameState started = service.start();
        service.reserveAutoplay(started.gameId());

        assertThatThrownBy(() -> service.reserveAutoplay(started.gameId())).isInstanceOf(AutoplayConflictException.class);
        assertThatThrownBy(() -> service.solve(started.gameId(), "ad0")).isInstanceOf(AutoplayConflictException.class);
        assertThatThrownBy(() -> service.buy(started.gameId(), "hpot")).isInstanceOf(AutoplayConflictException.class);
        assertThat(api.calls).isEmpty();
    }
}
