package eu.bigbank.mugloar.service;

public class AutoplayConflictException extends RuntimeException {

    public AutoplayConflictException(String gameId) {
        super("Auto-play is already running for game " + gameId);
    }
}
