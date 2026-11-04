package eu.bigbank.mugloar.domain;

/** {@code encrypted}: null for plain ads, otherwise how the text fields are encoded (see AdDecoder). */
public record Ad(String adId, String message, int reward, int expiresIn, Integer encrypted, String probability) {}
