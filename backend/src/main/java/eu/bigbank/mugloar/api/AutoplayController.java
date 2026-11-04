package eu.bigbank.mugloar.api;

import eu.bigbank.mugloar.client.MugloarApiException;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.service.GameService;
import eu.bigbank.mugloar.strategy.BotEvent;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Events: started, purchased, solved, finished, failed. Closing the connection stops the bot. */
@RestController
@RequestMapping("/api/games")
public class AutoplayController {

    private static final Logger log = LoggerFactory.getLogger(AutoplayController.class);
    static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final GameService games;

    public AutoplayController(GameService games) {
        this.games = games;
    }

    @GetMapping(path = "/{gameId}/autoplay", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter autoplay(
            @PathVariable @Pattern(regexp = GameController.ID, message = GameController.ID_MESSAGE) String gameId) {
        GameState start = games.reserveAutoplay(gameId);
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        AtomicBoolean stop = new AtomicBoolean(false);
        emitter.onCompletion(() -> stop.set(true));
        emitter.onTimeout(() -> stop.set(true));
        emitter.onError(e -> stop.set(true));

        Thread.ofVirtual().name("autoplay-" + gameId).start(() -> {
            try {
                games.runAutoplay(start, event -> send(emitter, event, stop), stop::get);
            } catch (MugloarApiException e) {
                log.warn("Auto-play for {} failed: {}", gameId, e.getMessage());
                trySend(emitter, "failed", Map.of("message", "The game API failed: " + e.getMessage()));
            } catch (RuntimeException e) {
                log.error("Auto-play for {} crashed", gameId, e);
                trySend(emitter, "failed", Map.of("message", "Auto-play stopped because of a server error."));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private static void send(SseEmitter emitter, BotEvent event, AtomicBoolean stop) {
        String name = switch (event) {
            case BotEvent.Started s -> "started";
            case BotEvent.Purchased p -> "purchased";
            case BotEvent.Solved s -> "solved";
            case BotEvent.Finished f -> "finished";
        };
        if (!trySend(emitter, name, event)) {
            stop.set(true); // client went away
        }
    }

    private static boolean trySend(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }
}
