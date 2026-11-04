package eu.bigbank.mugloar.client;

import eu.bigbank.mugloar.domain.Ad;
import eu.bigbank.mugloar.domain.Game;
import eu.bigbank.mugloar.domain.PurchaseResult;
import eu.bigbank.mugloar.domain.Reputation;
import eu.bigbank.mugloar.domain.ShopItem;
import eu.bigbank.mugloar.domain.SolveResult;
import java.util.List;

/** All methods throw {@link MugloarApiException} on failure. */
public interface GameApi {

    Game startGame();

    List<Ad> getAds(String gameId);

    SolveResult solve(String gameId, String adId);

    List<ShopItem> getShop(String gameId);

    PurchaseResult buy(String gameId, String itemId);

    Reputation investigate(String gameId);
}
