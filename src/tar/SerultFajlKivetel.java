package tar;

import java.io.IOException;

/**
 * Thrown when the file exists but its content is broken.
 * It is an IOException, so it fits the KonyvtarTar method signatures.
 */
public class SerultFajlKivetel extends IOException {

    public SerultFajlKivetel(String uzenet) {
        super(uzenet);
    }

    public SerultFajlKivetel(String uzenet, Throwable ok) {
        super(uzenet, ok);
    }
}
