package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The whole library: books, readers and the current loans.
 * This is the object that gets saved / loaded.
 */
public class Konyvtar implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Konyv> konyvek = new ArrayList<>();
    private final List<Olvaso> olvasok = new ArrayList<>();
    private final List<Kolcsonzes> kolcsonzesek = new ArrayList<>();

    // ---------- adding ----------

    public void addKonyv(Konyv konyv) {
        if (konyvek.contains(konyv)) { // contains() uses equals -> ISBN
            throw new IllegalArgumentException("A book with this ISBN already exists: " + konyv.getIsbn());
        }
        konyvek.add(konyv);
    }

    public void addOlvaso(Olvaso olvaso) {
        if (olvasok.contains(olvaso)) {
            throw new IllegalArgumentException("A reader with this card number already exists: "
                    + olvaso.getOlvasojegySzam());
        }
        olvasok.add(olvaso);
    }

    /**
     * Adds an existing loan (used when loading from a file).
     * The book and the reader must already be in the library.
     */
    public void addKolcsonzes(Kolcsonzes k) {
        if (!konyvek.contains(k.getKonyv())) {
            throw new IllegalArgumentException("Unknown book in loan: " + k.getKonyv().getIsbn());
        }
        if (!olvasok.contains(k.getOlvaso())) {
            throw new IllegalArgumentException("Unknown reader in loan: " + k.getOlvaso().getOlvasojegySzam());
        }
        if (szabadPeldany(k.getKonyv()) <= 0) {
            throw new IllegalStateException("No free copy of: " + k.getKonyv().getCim());
        }
        kolcsonzesek.add(k);
    }

    // ---------- getters (read-only lists) ----------

    public List<Konyv> getKonyvek() {
        return Collections.unmodifiableList(konyvek);
    }

    public List<Olvaso> getOlvasok() {
        return Collections.unmodifiableList(olvasok);
    }

    public List<Kolcsonzes> getKolcsonzesek() {
        return Collections.unmodifiableList(kolcsonzesek);
    }

    public Optional<Konyv> findKonyv(String isbn) {
        return konyvek.stream().filter(k -> k.getIsbn().equals(isbn)).findFirst();
    }

    public Optional<Olvaso> findOlvaso(String olvasojegySzam) {
        return olvasok.stream().filter(o -> o.getOlvasojegySzam().equals(olvasojegySzam)).findFirst();
    }

    // ---------- search and sort ----------

    /**
     * Books whose title OR author contains the given text (case-insensitive).
     */
    public List<Konyv> keres(String reszlet) {
        String kicsi = reszlet.toLowerCase();
        List<Konyv> talalat = new ArrayList<>();
        for (Konyv k : konyvek) {
            if (k.getCim().toLowerCase().contains(kicsi) || k.getSzerzo().toLowerCase().contains(kicsi)) {
                talalat.add(k);
            }
        }
        return talalat;
    }

    // returns a sorted COPY, the original order stays
    public List<Konyv> rendezveCimSzerint() {
        List<Konyv> masolat = new ArrayList<>(konyvek);
        masolat.sort(Konyv.CIM_SZERINT);
        return masolat;
    }

    public List<Konyv> rendezveEvSzerint() {
        List<Konyv> masolat = new ArrayList<>(konyvek);
        masolat.sort(Konyv.EV_SZERINT);
        return masolat;
    }

    // ---------- borrowing rules ----------

    // number of copies on the shelf = all copies - borrowed copies
    public int szabadPeldany(Konyv konyv) {
        int kint = 0;
        for (Kolcsonzes k : kolcsonzesek) {
            if (k.getKonyv().equals(konyv)) {
                kint++;
            }
        }
        return konyv.getPeldanyszam() - kint;
    }

    /**
     * Borrow a book on the given day. Due date = day + 14 days.
     * Throws if the book/reader doesn't exist or there is no free copy.
     */
    public Kolcsonzes kolcsonoz(String isbn, String olvasojegySzam, LocalDate datum) {
        Konyv konyv = findKonyv(isbn)
                .orElseThrow(() -> new IllegalArgumentException("No book with ISBN " + isbn));
        Olvaso olvaso = findOlvaso(olvasojegySzam)
                .orElseThrow(() -> new IllegalArgumentException("No reader with card number " + olvasojegySzam));

        if (szabadPeldany(konyv) <= 0) {
            throw new IllegalStateException("There is no free copy of \"" + konyv.getCim() + "\".");
        }

        Kolcsonzes uj = new Kolcsonzes(konyv, olvaso, datum, datum.plusDays(Kolcsonzes.KOLCSONZESI_IDO_NAP));
        kolcsonzesek.add(uj);
        return uj;
    }

    /**
     * Return a book on the given day. Returns the late fee in Ft (0 if on time).
     */
    public long visszahoz(String isbn, String olvasojegySzam, LocalDate ma) {
        for (Kolcsonzes k : kolcsonzesek) {
            if (k.getKonyv().getIsbn().equals(isbn) && k.getOlvaso().getOlvasojegySzam().equals(olvasojegySzam)) {
                kolcsonzesek.remove(k); // OK: we return right after, the loop doesn't continue
                return k.kesedelmiDij(ma);
            }
        }
        throw new IllegalArgumentException("This reader has not borrowed this book.");
    }
}
