package eu.bigbank.mugloar.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bigbank.mugloar.client.MugloarApiException;
import eu.bigbank.mugloar.domain.Ad;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.service.AutoplayConflictException;
import eu.bigbank.mugloar.service.GameService;
import eu.bigbank.mugloar.strategy.AdEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GameService games;

    @Test
    void startsGame() throws Exception {
        when(games.start()).thenReturn(new GameState("g1", 3, 0, 0, 0, 0));

        mvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("g1"))
                .andExpect(jsonPath("$.lives").value(3));
    }

    @Test
    void returnsAdvisedAds() throws Exception {
        var advice = new AdEvaluator().advise(List.of(new Ad("a1", "Help", 40, 5, null, "Sure thing")), 3);
        when(games.ads("g1")).thenReturn(advice);

        mvc.perform(get("/api/games/g1/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adId").value("a1"))
                .andExpect(jsonPath("$[0].recommended").value(true))
                .andExpect(jsonPath("$[0].risk").value("SAFE"))
                .andExpect(jsonPath("$[0].expectedValue").value(38.8));
    }

    @Test
    void rejectsInvalidIdsWithoutCallingTheGame() throws Exception {
        mvc.perform(post("/api/games/g1/ads/bad id!/solve"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post("/api/games/" + "x".repeat(65) + "/shop/hpot"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(games);
    }

    @Test
    void mapsGameOverTo410() throws Exception {
        when(games.solve(anyString(), anyString())).thenThrow(new MugloarApiException(410, "Gone"));

        mvc.perform(post("/api/games/g1/ads/a1/solve"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.title").value("Game over"));
    }

    @Test
    void mapsRejectedRequestTo400() throws Exception {
        when(games.buy("g1", "nope")).thenThrow(new MugloarApiException(400, "Bad Request"));

        mvc.perform(post("/api/games/g1/shop/nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Rejected by game API"));
    }

    @Test
    void mapsUnreachableApiTo502() throws Exception {
        when(games.shop("g1")).thenThrow(new MugloarApiException("down", new RuntimeException()));

        mvc.perform(get("/api/games/g1/shop"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Game API unavailable"));
    }

    @Test
    void mapsMoveDuringAutoplayTo409() throws Exception {
        when(games.solve("g1", "a1")).thenThrow(new AutoplayConflictException("g1"));

        mvc.perform(post("/api/games/g1/ads/a1/solve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Auto-play already running"));
    }

    @Test
    void hidesUnexpectedErrors() throws Exception {
        when(games.shop("g1")).thenThrow(new IllegalStateException("secret internals"));

        mvc.perform(get("/api/games/g1/shop"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side."));
    }
}
