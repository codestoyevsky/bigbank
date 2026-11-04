package eu.bigbank.mugloar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bigbank.mugloar.domain.GameState;
import org.junit.jupiter.api.Test;

class GameRegistryTest {

    private final GameRegistry registry = new GameRegistry();

    @Test
    void forgetsLeastRecentlyUsedGamesBeyondTheLimit() {
        for (int i = 0; i <= GameRegistry.MAX_GAMES; i++) {
            registry.save(GameState.unknown("g" + i));
            if (i == 0) continue;
            registry.find("g0"); // keep g0 fresh
        }

        assertThat(registry.find("g0")).isPresent();
        assertThat(registry.find("g1")).isEmpty();
    }

    @Test
    void updateCreatesUnknownGames() {
        GameState s = registry.update("new", GameState::nextTurn);

        assertThat(s.turn()).isEqualTo(1);
        assertThat(registry.find("new")).contains(s);
    }

    @Test
    void limitsConcurrentBots() {
        for (int i = 0; i < GameRegistry.MAX_CONCURRENT_AUTOPLAYS; i++) {
            registry.startAutoplay("g" + i);
        }
        assertThatThrownBy(() -> registry.startAutoplay("one-too-many")).isInstanceOf(TooManyAutoplaysException.class);

        registry.stopAutoplay("g0");
        registry.startAutoplay("one-too-many");
        assertThat(registry.isAutoplaying("one-too-many")).isTrue();
    }
}
