package eu.bigbank.mugloar.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bigbank.mugloar.domain.Ad;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AdDecoderTest {

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void plainAdIsReturnedUnchanged() {
        Ad ad = new Ad("abc", "Help someone", 10, 7, null, "Sure thing");
        assertThat(AdDecoder.decode(ad)).isSameAs(ad);
    }

    @Test
    void decodesBase64Ads() {
        Ad ad = new Ad(b64("id42"), b64("Steal a chicken"), 30, 5, AdDecoder.BASE64, b64("Piece of cake"));

        Ad decoded = AdDecoder.decode(ad);

        assertThat(decoded).isEqualTo(new Ad("id42", "Steal a chicken", 30, 5, null, "Piece of cake"));
    }

    @Test
    void decodesRot13Ads() {
        Ad ad = new Ad("vq42", "Uryc fbzrbar", 30, 5, AdDecoder.ROT13, "Cvrpr bs pnxr");

        Ad decoded = AdDecoder.decode(ad);

        assertThat(decoded.adId()).isEqualTo("id42");
        assertThat(decoded.message()).isEqualTo("Help someone");
        assertThat(decoded.probability()).isEqualTo("Piece of cake");
        assertThat(decoded.encrypted()).isNull();
    }

    @Test
    void rot13KeepsNonLettersAndIsItsOwnInverse() {
        String text = "Hmmm.... 123 Zz";
        assertThat(AdDecoder.rot13(AdDecoder.rot13(text))).isEqualTo(text);
        assertThat(AdDecoder.rot13("Zz.")).isEqualTo("Mm.");
    }

    @Test
    void invalidBase64IsLeftAsIs() {
        assertThat(AdDecoder.base64("not base64!")).isEqualTo("not base64!");
    }

    @Test
    void unknownEncryptionIsLeftAsIs() {
        Ad ad = new Ad("x", "y", 1, 1, 99, "z");
        assertThat(AdDecoder.decode(ad)).isSameAs(ad);
    }
}
