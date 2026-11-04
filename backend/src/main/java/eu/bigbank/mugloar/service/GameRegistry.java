package eu.bigbank.mugloar.service;

import eu.bigbank.mugloar.domain.GameState;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

/** The game API can't return a game's state, so we keep the last one we saw. In memory only. */
@Component
public class GameRegistry {

    static final int MAX_GAMES = 1_000;
    // Each bot run keeps a connection open and calls the game API in a loop.
    static final int MAX_CONCURRENT_AUTOPLAYS = 10;

    // Access-ordered, so the least recently used game is dropped first.
    private final Map<String, GameState> games = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, GameState> eldest) {
            return size() > MAX_GAMES;
        }
    });
    private final Set<String> autoplaying = ConcurrentHashMap.newKeySet();

    public GameState save(GameState state) {
        games.put(state.gameId(), state);
        return state;
    }

    /** Games we don't know (after a restart) are added back, so the next move's response fills them in. */
    public GameState update(String gameId, UnaryOperator<GameState> change) {
        return games.compute(gameId, (id, current) -> change.apply(current == null ? GameState.unknown(id) : current));
    }

    public Optional<GameState> find(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    public GameState get(String gameId) {
        return find(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public synchronized void startAutoplay(String gameId) {
        if (autoplaying.contains(gameId)) {
            throw new AutoplayConflictException(gameId);
        }
        if (autoplaying.size() >= MAX_CONCURRENT_AUTOPLAYS) {
            throw new TooManyAutoplaysException();
        }
        autoplaying.add(gameId);
    }

    public void stopAutoplay(String gameId) {
        autoplaying.remove(gameId);
    }

    public boolean isAutoplaying(String gameId) {
        return autoplaying.contains(gameId);
    }
}
