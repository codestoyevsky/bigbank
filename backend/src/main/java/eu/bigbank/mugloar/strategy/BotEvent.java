package eu.bigbank.mugloar.strategy;

import eu.bigbank.mugloar.domain.GameState;

public sealed interface BotEvent {

    GameState state();

    record Started(GameState state) implements BotEvent {}

    record Purchased(GameState state, String itemId, String itemName, boolean success) implements BotEvent {}

    record Solved(GameState state, String adId, String adMessage, String probability, int reward, boolean success,
            String message) implements BotEvent {}

    record Finished(GameState state, String reason) implements BotEvent {}
}
