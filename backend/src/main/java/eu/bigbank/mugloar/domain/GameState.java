package eu.bigbank.mugloar.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Rebuilt from start/solve/buy responses; the game API has no "get game" endpoint. */
public record GameState(String gameId, int lives, int gold, int level, int score, int turn) {

    public static GameState unknown(String gameId) {
        return new GameState(gameId, 0, 0, 0, 0, 0);
    }

    public static GameState from(Game game) {
        return new GameState(game.gameId(), game.lives(), game.gold(), game.level(), game.score(), game.turn());
    }

    public GameState apply(SolveResult r) {
        return new GameState(gameId, r.lives(), r.gold(), level, r.score(), r.turn());
    }

    public GameState apply(PurchaseResult r) {
        return new GameState(gameId, r.lives(), r.gold(), r.level(), score, r.turn());
    }

    public GameState withNoLives() {
        return new GameState(gameId, 0, gold, level, score, turn);
    }

    // Investigating costs a turn, but its response doesn't include the turn number.
    public GameState nextTurn() {
        return new GameState(gameId, lives, gold, level, score, turn + 1);
    }

    @JsonIgnore
    public boolean isOver() {
        return lives <= 0;
    }
}
