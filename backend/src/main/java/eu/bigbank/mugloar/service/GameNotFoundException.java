package eu.bigbank.mugloar.service;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String gameId) {
        super("Game " + gameId + " is not known to this server. Start a new game.");
    }
}
