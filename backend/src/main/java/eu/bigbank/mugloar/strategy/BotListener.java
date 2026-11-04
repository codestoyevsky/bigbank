package eu.bigbank.mugloar.strategy;

@FunctionalInterface
public interface BotListener {

    void onEvent(BotEvent event);
}
