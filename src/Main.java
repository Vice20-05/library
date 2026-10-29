import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

import model.Kolcsonzes;
import model.Konyv;
import model.Konyvtar;
import model.Olvaso;
import tar.CsvTar;
import tar.JsonTar;
import tar.KonyvtarTar;
import tar.SzerializaltTar;

/**
 * Console program for the library.
 *
 * Usage:  java ... Main [csv|ser|json]      (default: csv)
 * The data is in konyvtar.csv / konyvtar.ser / konyvtar.json in the current folder.
 */
public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        String formatum = args.length > 0 ? args[0] : "csv";

        // the only place where the concrete storage is chosen
        KonyvtarTar tar;
        switch (formatum) {
            case "ser" -> tar = new SzerializaltTar();
            case "json" -> tar = new JsonTar();
            case "csv" -> tar = new CsvTar();
            default -> {
                System.out.println("Unknown format: " + formatum + " (use csv, ser or json)");
                return;
            }
        }
        Path fajl = Path.of("konyvtar." + formatum);

        Konyvtar konyvtar = betolt(tar, fajl);

        boolean fut = true;
        while (fut) {
            menu();
            String valasztas = sc.nextLine().trim();
            // one bad input must not crash the whole program
            try {
                switch (valasztas) {
                    case "1" -> listaz(konyvtar, konyvtar.rendezveCimSzerint());
                    case "2" -> listaz(konyvtar, konyvtar.rendezveEvSzerint());
                    case "3" -> listaz(konyvtar, konyvtar.keres(beker("Search text (title or author): ")));
                    case "4" -> ujKonyv(konyvtar);
                    case "5" -> ujOlvaso(konyvtar);
                    case "6" -> konyvtar.getOlvasok().forEach(o ->
                            System.out.println("  " + o.getOlvasojegySzam() + "  " + o.getNev()));
                    case "7" -> kolcsonzes(konyvtar);
                    case "8" -> visszahozas(konyvtar);
                    case "9" -> kolcsonzesekListaja(konyvtar);
                    case "s" -> mentes(tar, konyvtar, fajl);
                    case "0" -> {
                        mentes(tar, konyvtar, fajl);
                        fut = false;
                    }
                    default -> System.out.println("Unknown menu item.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: that is not a number.");
            } catch (DateTimeParseException e) {
                System.out.println("Error: wrong date, use YYYY-MM-DD.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                // the business rules throw these (e.g. no free copy)
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Bye!");
    }

    private static void menu() {
        System.out.println();
        System.out.println("===== LIBRARY =====");
        System.out.println(" 1 - List books (by title)");
        System.out.println(" 2 - List books (by year)");
        System.out.println(" 3 - Search books");
        System.out.println(" 4 - Add book");
        System.out.println(" 5 - Add reader");
        System.out.println(" 6 - List readers");
        System.out.println(" 7 - Borrow a book");
        System.out.println(" 8 - Return a book");
        System.out.println(" 9 - List loans");
        System.out.println(" s - Save");
        System.out.println(" 0 - Save and exit");
        System.out.print("> ");
    }

    // ---------- load / save ----------

    private static Konyvtar betolt(KonyvtarTar tar, Path fajl) {
        if (!Files.exists(fajl)) {
            System.out.println("No data file yet (" + fajl + "), starting with sample data.");
            return mintaAdatok();
        }
        try {
            Konyvtar k = tar.betolt(fajl);
            System.out.println("Loaded: " + fajl + " (" + k.getKonyvek().size() + " books, "
                    + k.getOlvasok().size() + " readers, " + k.getKolcsonzesek().size() + " loans)");
            return k;
        } catch (IOException e) {
            // damaged file -> message instead of a crash
            System.out.println("Could not load " + fajl + ": " + e.getMessage());
            // keep a copy of the damaged file, so saving later doesn't destroy it
            Path masolat = Path.of(fajl + ".hibas");
            try {
                Files.copy(fajl, masolat, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("The damaged file was copied to " + masolat);
            } catch (IOException e2) {
                System.out.println("Could not copy the damaged file: " + e2.getMessage());
            }
            System.out.println("Starting with an EMPTY library.");
            return new Konyvtar();
        }
    }

    private static void mentes(KonyvtarTar tar, Konyvtar k, Path fajl) {
        try {
            tar.ment(k, fajl);
            System.out.println("Saved to " + fajl.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage() + " (the old file is untouched)");
        }
    }

    // ---------- menu actions ----------

    private static void listaz(Konyvtar k, List<Konyv> konyvek) {
        if (konyvek.isEmpty()) {
            System.out.println("  (no books)");
            return;
        }
        System.out.printf("  %-15s %-30s %-22s %5s %s%n", "ISBN", "Title", "Author", "Year", "Free");
        for (Konyv b : konyvek) {
            System.out.printf("  %-15s %-30s %-22s %5d %d/%d%n", b.getIsbn(), b.getCim(), b.getSzerzo(),
                    b.getKiadasEve(), k.szabadPeldany(b), b.getPeldanyszam());
        }
    }

    private static void ujKonyv(Konyvtar k) {
        String isbn = beker("ISBN: ");
        String cim = beker("Title: ");
        String szerzo = beker("Author: ");
        int ev = Integer.parseInt(beker("Year: "));
        int db = Integer.parseInt(beker("Copies: "));
        k.addKonyv(new Konyv(isbn, cim, szerzo, ev, db));
        System.out.println("Book added.");
    }

    private static void ujOlvaso(Konyvtar k) {
        String jegy = beker("Card number: ");
        String nev = beker("Name: ");
        k.addOlvaso(new Olvaso(jegy, nev));
        System.out.println("Reader added.");
    }

    private static void kolcsonzes(Konyvtar k) {
        String isbn = beker("ISBN: ");
        String jegy = beker("Card number: ");
        Kolcsonzes kol = k.kolcsonoz(isbn, jegy, LocalDate.now());
        System.out.println("OK, due date: " + kol.getHatarido());
    }

    private static void visszahozas(Konyvtar k) {
        String isbn = beker("ISBN: ");
        String jegy = beker("Card number: ");
        // a date can be typed in to try out the late fee
        String datum = beker("Return date (YYYY-MM-DD, empty = today): ");
        LocalDate nap = datum.isEmpty() ? LocalDate.now() : LocalDate.parse(datum);
        long dij = k.visszahoz(isbn, jegy, nap);
        if (dij == 0) {
            System.out.println("Returned on time, no fee.");
        } else {
            System.out.println("Returned LATE. Late fee: " + dij + " Ft");
        }
    }

    private static void kolcsonzesekListaja(Konyvtar k) {
        LocalDate ma = LocalDate.now();
        if (k.getKolcsonzesek().isEmpty()) {
            System.out.println("  (no loans)");
        }
        for (Kolcsonzes kol : k.getKolcsonzesek()) {
            System.out.printf("  %-30s %-18s due %s  fee today: %d Ft%n", kol.getKonyv().getCim(),
                    kol.getOlvaso().getNev(), kol.getHatarido(), kol.kesedelmiDij(ma));
        }
    }

    private static String beker(String kerdes) {
        System.out.print(kerdes);
        return sc.nextLine().trim();
    }

    // some data so the program is not empty on the first start
    private static Konyvtar mintaAdatok() {
        Konyvtar k = new Konyvtar();
        k.addKonyv(new Konyv("978-0134685991", "Effective Java", "Joshua Bloch", 2018, 2));
        k.addKonyv(new Konyv("978-0132350884", "Clean Code", "Robert C. Martin", 2008, 1));
        k.addKonyv(new Konyv("978-0596009205", "Head First Java", "Kathy Sierra", 2005, 3));
        k.addKonyv(new Konyv("978-9634155000", "A Pál utcai fiúk", "Molnár Ferenc", 1906, 2));
        k.addOlvaso(new Olvaso("R001", "Kiss Anna"));
        k.addOlvaso(new Olvaso("R002", "Nagy Péter"));
        k.kolcsonoz("978-0132350884", "R001", LocalDate.now().minusDays(20)); // already 6 days late
        return k;
    }
}
