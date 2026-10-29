package tar;

import java.io.IOException;
import java.nio.file.Path;

import model.Konyvtar;

/**
 * Common interface of the storage formats (serialization, CSV, JSON).
 * Main only works with this, so the format can be swapped easily.
 */
public interface KonyvtarTar {

    void ment(Konyvtar k, Path fajl) throws IOException;

    Konyvtar betolt(Path fajl) throws IOException;
}
