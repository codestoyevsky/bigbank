package eu.bigbank.mugloar.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import eu.bigbank.mugloar.config.MugloarProperties;
import eu.bigbank.mugloar.domain.Ad;
import eu.bigbank.mugloar.domain.Game;
import eu.bigbank.mugloar.domain.SolveResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MugloarHttpClientTest {

    private static final String BASE = "http://mugloar.test/api/v2";
    private static final String HTML_ERROR =
            "<!DOCTYPE html><html><body><pre>Bad Request</pre></body></html>";

    private MockRestServiceServer server;
    private MugloarHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        MugloarProperties props = new MugloarProperties(
                new MugloarProperties.Api(BASE, Duration.ofSeconds(1), Duration.ofSeconds(1), 2),
                new MugloarProperties.Web(List.of("http://localhost")),
                new MugloarProperties.Bot(100));
        client = new MugloarHttpClient(builder.build(), props);
    }

    @Test
    void startsGame() {
        server.expect(requestTo(BASE + "/game/start")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"gameId":"g1","lives":3,"gold":0,"level":0,"score":0,"highScore":0,"turn":0}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.startGame()).isEqualTo(new Game("g1", 3, 0, 0, 0, 0, 0));
    }

    @Test
    void readsAdsIncludingEncryptedOnes() {
        server.expect(requestTo(BASE + "/g1/messages"))
                .andRespond(withSuccess("""
                        [{"adId":"a1","message":"Help","reward":37,"expiresIn":7,"encrypted":null,"probability":"Gamble"},
                         {"adId":"YTI=","message":"SGk=","reward":5,"expiresIn":1,"encrypted":1,"probability":"Umlza3k="}]
                        """, MediaType.APPLICATION_JSON));

        List<Ad> ads = client.getAds("g1");

        assertThat(ads).hasSize(2);
        assertThat(ads.get(1).encrypted()).isEqualTo(1);
    }

    @Test
    void solvesAd() {
        server.expect(requestTo(BASE + "/g1/solve/a1")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"success":true,"lives":3,"gold":74,"score":74,"highScore":0,"turn":1,"message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        SolveResult r = client.solve("g1", "a1");

        assertThat(r.success()).isTrue();
        assertThat(r.score()).isEqualTo(74);
    }

    @Test
    void turnsHtmlErrorPagesIntoExceptions() {
        server.expect(requestTo(BASE + "/g1/solve/bogus"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML).body(HTML_ERROR));

        assertThatThrownBy(() -> client.solve("g1", "bogus"))
                .isInstanceOfSatisfying(MugloarApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(400);
                    assertThat(e.getMessage()).isEqualTo("Bad Request");
                    assertThat(e.isUnreachable()).isFalse();
                });
    }

    @Test
    void reportsGameOver() {
        server.expect(requestTo(BASE + "/g1/messages"))
                .andRespond(withStatus(HttpStatus.GONE).body("<pre>Gone</pre>"));

        assertThatThrownBy(() -> client.getAds("g1"))
                .isInstanceOfSatisfying(MugloarApiException.class, e -> assertThat(e.isGameOver()).isTrue());
    }

    @Test
    void retriesReadsOnServerErrors() {
        server.expect(times(2), requestTo(BASE + "/g1/shop")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/g1/shop"))
                .andRespond(withSuccess("[{\"id\":\"hpot\",\"name\":\"Healing potion\",\"cost\":50}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.getShop("g1")).singleElement().extracting("id").isEqualTo("hpot");
        server.verify();
    }

    @Test
    void doesNotRetryWrites() {
        server.expect(times(1), requestTo(BASE + "/g1/shop/buy/hpot")).andRespond(withServerError());

        assertThatThrownBy(() -> client.buy("g1", "hpot"))
                .isInstanceOfSatisfying(MugloarApiException.class, e -> assertThat(e.isUnreachable()).isTrue());
        server.verify();
    }
}
