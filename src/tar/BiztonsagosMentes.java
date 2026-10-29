package tar;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Helper for safe saving, used by every storage format:
 *  1. write everything into "fajl.tmp"
 *  2. rename "fajl.tmp" -> "fajl" in one step (ATOMIC_MOVE)
 * If the power goes out during step 1, the old file is still OK.
 */
public class BiztonsagosMentes {

    private BiztonsagosMentes() {
        // only static methods
    }

    // the temporary file next to the real one, e.g. konyvtar.csv -> konyvtar.csv.tmp
    public static Path ideiglenesFajl(Path fajl) {
        return fajl.resolveSibling(fajl.getFileName() + ".tmp");
    }

    /**
     * Moves the finished temp file over the real file.
     */
    public static void csere(Path tmp, Path fajl) throws IOException {
        try {
            Files.move(tmp, fajl, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // some file systems can't do it atomically -> normal move as a fallback
            Files.move(tmp, fajl, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // if writing failed, remove the half-written temp file
    public static void takarit(Path tmp) {
        try {
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            // nothing else we can do, the real file is untouched anyway
        }
    }
}
