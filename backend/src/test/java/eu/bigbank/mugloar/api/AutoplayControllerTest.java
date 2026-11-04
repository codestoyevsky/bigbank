package eu.bigbank.mugloar.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bigbank.mugloar.client.MugloarApiException;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.service.AutoplayConflictException;
import eu.bigbank.mugloar.service.GameNotFoundException;
import eu.bigbank.mugloar.service.GameService;
import eu.bigbank.mugloar.strategy.BotEvent;
import eu.bigbank.mugloar.strategy.BotListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AutoplayController.class)
class AutoplayControllerTest {

    private static final GameState START = new GameState("g1", 3, 0, 0, 0, 0);
    private static final GameState END = new GameState("g1", 0, 10, 2, 1500, 40);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GameService games;

    private String stream() throws Exception {
        MvcResult result = mvc.perform(get("/api/games/g1/autoplay").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        // The bot runs on its own thread; wait for the stream to complete.
        result.getAsyncResult(5_000);
        return result.getResponse().getContentAsString();
    }

    @Test
    void streamsBotEventsAsServerSentEvents() throws Exception {
        when(games.reserveAutoplay("g1")).thenReturn(START);
        when(games.runAutoplay(eq(START), any(), any())).thenAnswer(inv -> {
            BotListener listener = inv.getArgument(1);
            listener.onEvent(new BotEvent.Started(START));
            listener.onEvent(new BotEvent.Solved(END, "a1", "Help", "Sure thing", 50, true, "ok"));
            listener.onEvent(new BotEvent.Finished(END, "Game over"));
            return END;
        });

        String body = stream();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event:started")
                .contains("event:solved")
                .contains("\"adId\":\"a1\"")
                .contains("event:finished")
                .contains("\"reason\":\"Game over\"")
                .doesNotContain("\"over\"");
    }

    @Test
    void reportsUpstreamFailureAsFailedEvent() throws Exception {
        when(games.reserveAutoplay("g1")).thenReturn(START);
        when(games.runAutoplay(eq(START), any(), any())).thenThrow(new MugloarApiException(503, "down"));

        org.assertj.core.api.Assertions.assertThat(stream()).contains("event:failed").contains("down");
    }

    @Test
    void hidesInternalErrorDetails() throws Exception {
        when(games.reserveAutoplay("g1")).thenReturn(START);
        when(games.runAutoplay(eq(START), any(), any())).thenThrow(new IllegalStateException("secret internals"));

        org.assertj.core.api.Assertions.assertThat(stream()).contains("event:failed").doesNotContain("secret");
    }

    @Test
    void rejectsUnknownGameAndRunningBotBeforeStreaming() throws Exception {
        when(games.reserveAutoplay("nope")).thenThrow(new GameNotFoundException("nope"));
        when(games.reserveAutoplay("busy")).thenThrow(new AutoplayConflictException("busy"));

        mvc.perform(get("/api/games/nope/autoplay").accept(MediaType.TEXT_EVENT_STREAM, MediaType.ALL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/api/games/busy/autoplay").accept(MediaType.TEXT_EVENT_STREAM, MediaType.ALL))
                .andExpect(status().isConflict());
    }
}
