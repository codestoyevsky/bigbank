package eu.bigbank.mugloar.client;

import eu.bigbank.mugloar.config.MugloarProperties;
import eu.bigbank.mugloar.domain.Ad;
import eu.bigbank.mugloar.domain.Game;
import eu.bigbank.mugloar.domain.PurchaseResult;
import eu.bigbank.mugloar.domain.Reputation;
import eu.bigbank.mugloar.domain.ShopItem;
import eu.bigbank.mugloar.domain.SolveResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

/** Only reads are retried. Retrying a solve or buy could do it twice. */
@Component
public class MugloarHttpClient implements GameApi {

    private static final Logger log = LoggerFactory.getLogger(MugloarHttpClient.class);
    // Errors come back as HTML, e.g. <pre>Bad Request</pre>
    private static final Pattern PRE_TAG = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);

    static final Duration RETRY_BACKOFF = Duration.ofMillis(300);

    private final RestClient rest;
    private final int maxRetries;

    public MugloarHttpClient(RestClient mugloarRestClient, MugloarProperties props) {
        this.rest = mugloarRestClient;
        this.maxRetries = props.api().maxRetries();
    }

    @Override
    public Game startGame() {
        return call(() -> post("/game/start", Game.class));
    }

    @Override
    public List<Ad> getAds(String gameId) {
        return withRetry(() -> call(() -> rest.get()
                .uri("/{gameId}/messages", gameId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, MugloarHttpClient::raise)
                .body(new ParameterizedTypeReference<List<Ad>>() {})));
    }

    @Override
    public SolveResult solve(String gameId, String adId) {
        return call(() -> post("/{gameId}/solve/{adId}", SolveResult.class, gameId, adId));
    }

    @Override
    public List<ShopItem> getShop(String gameId) {
        return withRetry(() -> call(() -> rest.get()
                .uri("/{gameId}/shop", gameId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, MugloarHttpClient::raise)
                .body(new ParameterizedTypeReference<List<ShopItem>>() {})));
    }

    @Override
    public PurchaseResult buy(String gameId, String itemId) {
        return call(() -> post("/{gameId}/shop/buy/{itemId}", PurchaseResult.class, gameId, itemId));
    }

    @Override
    public Reputation investigate(String gameId) {
        return call(() -> post("/{gameId}/investigate/reputation", Reputation.class, gameId));
    }

    private <T> T post(String uri, Class<T> type, Object... vars) {
        return rest.post()
                .uri(uri, vars)
                .retrieve()
                .onStatus(HttpStatusCode::isError, MugloarHttpClient::raise)
                .body(type);
    }

    private static <T> T call(Supplier<T> request) {
        try {
            T body = request.get();
            if (body == null) {
                throw new MugloarApiException(502, "Empty response from game API");
            }
            return body;
        } catch (ResourceAccessException e) {
            throw new MugloarApiException("Game API is unreachable", e);
        } catch (MugloarApiException e) {
            throw e;
        } catch (RestClientException e) {
            // e.g. a 200 with an unexpected body
            throw new MugloarApiException(502, "Unexpected response from game API: " + e.getMessage());
        }
    }

    private <T> T withRetry(Supplier<T> request) {
        for (int attempt = 0; ; attempt++) {
            try {
                return request.get();
            } catch (MugloarApiException e) {
                if (!e.isUnreachable() || attempt >= maxRetries) {
                    throw e;
                }
                log.warn("Game API call failed ({}), retrying ({}/{})", e.getMessage(), attempt + 1, maxRetries);
                sleep(RETRY_BACKOFF.multipliedBy(attempt + 1));
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MugloarApiException("Interrupted while waiting to retry", e);
        }
    }

    private static void raise(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        throw new MugloarApiException(response.getStatusCode().value(), extractReason(body, response));
    }

    static String extractReason(String body, ClientHttpResponse response) throws IOException {
        Matcher m = PRE_TAG.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }
        String text = response.getStatusText();
        return text == null || text.isBlank() ? "Game API error" : text;
    }
}
