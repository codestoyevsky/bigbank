package eu.bigbank.mugloar.domain;

public record Game(String gameId, int lives, int gold, int level, int score, int highScore, int turn) {}
