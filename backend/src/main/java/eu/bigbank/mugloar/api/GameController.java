package eu.bigbank.mugloar.api;

import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.domain.PurchaseResult;
import eu.bigbank.mugloar.domain.Reputation;
import eu.bigbank.mugloar.domain.ShopItem;
import eu.bigbank.mugloar.domain.SolveResult;
import eu.bigbank.mugloar.service.GameService;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    // Game ids look like "zSM65ghV". Anything else is rejected before we call the game API.
    static final String ID = "^[A-Za-z0-9_-]{1,64}$";
    static final String ID_MESSAGE = "must be 1-64 letters, digits, '-' or '_'";

    private final GameService games;

    public GameController(GameService games) {
        this.games = games;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameState start() {
        return games.start();
    }

    @GetMapping("/{gameId}/ads")
    public List<AdView> ads(@PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String gameId) {
        return games.ads(gameId).stream().map(AdView::of).toList();
    }

    @PostMapping("/{gameId}/ads/{adId}/solve")
    public SolveResult solve(
            @PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String gameId,
            @PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String adId) {
        return games.solve(gameId, adId);
    }

    @GetMapping("/{gameId}/shop")
    public List<ShopItem> shop(@PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String gameId) {
        return games.shop(gameId);
    }

    @PostMapping("/{gameId}/shop/{itemId}")
    public PurchaseResult buy(
            @PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String gameId,
            @PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String itemId) {
        return games.buy(gameId, itemId);
    }

    @PostMapping("/{gameId}/reputation")
    public Reputation reputation(@PathVariable @Pattern(regexp = ID, message = ID_MESSAGE) String gameId) {
        return games.reputation(gameId);
    }
}
