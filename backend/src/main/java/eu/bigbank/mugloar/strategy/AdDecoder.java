package eu.bigbank.mugloar.strategy;

import eu.bigbank.mugloar.domain.Ad;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** {@code encrypted = 1} is Base64, {@code 2} is ROT13. The id has to be decoded too, or solving fails. */
public final class AdDecoder {

    public static final int BASE64 = 1;
    public static final int ROT13 = 2;

    private AdDecoder() {}

    public static Ad decode(Ad ad) {
        if (ad.encrypted() == null) {
            return ad;
        }
        return switch (ad.encrypted()) {
            case BASE64 -> withText(ad, base64(ad.adId()), base64(ad.message()), base64(ad.probability()));
            case ROT13 -> withText(ad, rot13(ad.adId()), rot13(ad.message()), rot13(ad.probability()));
            // Unknown encoding: leave it. The label won't match, so it scores low.
            default -> ad;
        };
    }

    private static Ad withText(Ad ad, String adId, String message, String probability) {
        return new Ad(adId, message, ad.reward(), ad.expiresIn(), null, probability);
    }

    static String base64(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    static String rot13(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                out.append((char) ('a' + (c - 'a' + 13) % 26));
            } else if (c >= 'A' && c <= 'Z') {
                out.append((char) ('A' + (c - 'A' + 13) % 26));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
