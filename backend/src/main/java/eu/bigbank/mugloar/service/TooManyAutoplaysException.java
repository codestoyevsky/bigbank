package eu.bigbank.mugloar.service;

public class TooManyAutoplaysException extends RuntimeException {

    public TooManyAutoplaysException() {
        super("Too many bots are playing right now. Try again in a minute.");
    }
}
