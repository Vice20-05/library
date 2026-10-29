package tar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import model.Konyv;
import model.Konyvtar;
import model.Olvaso;

/**
 * Save -> load tests for all three storage formats.
 * @TempDir: JUnit makes an empty temporary folder for every test and deletes it afterwards.
 */
public class KonyvtarTarTest {

    @TempDir
    Path mappa;

    private Konyvtar mintaKonyvtar() {
        Konyvtar k = new Konyvtar();
        k.addKonyv(new Konyv("111", "Effective Java", "Joshua Bloch", 2018, 2));
        k.addKonyv(new Konyv("222", "Tom; Jerry \\ and \"friends\"", "Árvíztűrő Tükörfúrógép", 1999, 1)); // tricky text
        k.addOlvaso(new Olvaso("R1", "Kiss Anna"));
        k.addOlvaso(new Olvaso("R2", "Nagy Péter"));
        k.kolcsonoz("111", "R1", LocalDate.of(2026, 10, 1));
        k.kolcsonoz("222", "R2", LocalDate.of(2026, 9, 20));
        return k;
    }

    /**
     * equals() only compares ISBN / card number, so we compare the toString()
     * of the lists -> every field is checked.
     */
    private void ugyanaz(Konyvtar elvart, Konyvtar kapott) {
        assertEquals(elvart.getKonyvek().toString(), kapott.getKonyvek().toString());
        assertEquals(elvart.getOlvasok().toString(), kapott.getOlvasok().toString());
        assertEquals(elvart.getKolcsonzesek().toString(), kapott.getKolcsonzesek().toString());
    }

    // ---------- save -> load gives the same state ----------

    @Test
    public void serializationSaveAndLoad() throws IOException {
        Konyvtar eredeti = mintaKonyvtar();
        Path fajl = mappa.resolve("konyvtar.ser");
        KonyvtarTar tar = new SzerializaltTar();

        tar.ment(eredeti, fajl);
        ugyanaz(eredeti, tar.betolt(fajl));
    }

    @Test
    public void csvSaveAndLoad() throws IOException {
        Konyvtar eredeti = mintaKonyvtar();
        Path fajl = mappa.resolve("konyvtar.csv");
        KonyvtarTar tar = new CsvTar();

        tar.ment(eredeti, fajl);
        ugyanaz(eredeti, tar.betolt(fajl));
    }

    @Test
    public void jsonSaveAndLoad() throws IOException {
        Konyvtar eredeti = mintaKonyvtar();
        Path fajl = mappa.resolve("konyvtar.json");
        KonyvtarTar tar = new JsonTar();

        tar.ment(eredeti, fajl);
        ugyanaz(eredeti, tar.betolt(fajl));
    }

    @Test
    public void loadedLibraryStillKnowsTheLoans() throws IOException {
        Path fajl = mappa.resolve("konyvtar.csv");
        new CsvTar().ment(mintaKonyvtar(), fajl);
        Konyvtar k = new CsvTar().betolt(fajl);

        // "111" has 2 copies and 1 is borrowed
        assertEquals(1, k.szabadPeldany(k.findKonyv("111").get()));
    }

    // ---------- safe save ----------

    @Test
    public void noTempFileIsLeftAndOldFileIsReplaced() throws IOException {
        Path fajl = mappa.resolve("konyvtar.csv");
        Files.writeString(fajl, "old content");

        new CsvTar().ment(mintaKonyvtar(), fajl);

        assertFalse(Files.exists(mappa.resolve("konyvtar.csv.tmp")));
        assertTrue(Files.readString(fajl).startsWith(CsvTar.FEJLEC));
    }

    // ---------- damaged files -> own exception, no crash ----------

    @Test
    public void damagedCsvGivesClearError() throws IOException {
        Path fajl = mappa.resolve("rossz.csv");
        Files.writeString(fajl, CsvTar.FEJLEC + "\nKONYV;111;Title;Author;not-a-year;2\n");

        SerultFajlKivetel e = assertThrows(SerultFajlKivetel.class, () -> new CsvTar().betolt(fajl));
        assertTrue(e.getMessage().startsWith("Error in line 2"));
    }

    @Test
    public void csvWithoutHeaderIsRejected() throws IOException {
        Path fajl = mappa.resolve("rossz.csv");
        Files.writeString(fajl, "just some random text\n");
        assertThrows(SerultFajlKivetel.class, () -> new CsvTar().betolt(fajl));
    }

    @Test
    public void damagedSerFileGivesClearError() throws IOException {
        Path fajl = mappa.resolve("rossz.ser");
        Files.writeString(fajl, "this is not a serialized object");
        assertThrows(SerultFajlKivetel.class, () -> new SzerializaltTar().betolt(fajl));
    }

    @Test
    public void halfWrittenSerFileGivesClearError() throws IOException {
        Path fajl = mappa.resolve("konyvtar.ser");
        new SzerializaltTar().ment(mintaKonyvtar(), fajl);
        // cut the file in half, like when the power goes out
        byte[] adat = Files.readAllBytes(fajl);
        Files.write(fajl, Arrays.copyOf(adat, adat.length / 2));

        assertThrows(SerultFajlKivetel.class, () -> new SzerializaltTar().betolt(fajl));
    }

    @Test
    public void damagedJsonGivesClearError() throws IOException {
        Path fajl = mappa.resolve("rossz.json");
        Files.writeString(fajl, "{ \"konyvek\": [ { \"isbn\": \"111\" ");
        assertThrows(SerultFajlKivetel.class, () -> new JsonTar().betolt(fajl));
    }

    // ---------- CSV escaping ----------

    @Test
    public void csvEscapingWorksBothWays() {
        String szoveg = "a;b\\c";
        String vedett = CsvTar.vedes(szoveg);
        assertEquals("a\\;b\\\\c", vedett);
        assertEquals(List.of("x", szoveg, "y"), CsvTar.feldarabol("x;" + vedett + ";y"));
    }
}
