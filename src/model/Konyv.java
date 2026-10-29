package model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * A book. Two books are the same if their ISBN is the same.
 * Serializable -> can be saved with ObjectOutputStream.
 */
public class Konyv implements Serializable {
    private static final long serialVersionUID = 1L;

    // ready-made comparators for sorting
    public static final Comparator<Konyv> CIM_SZERINT =
            Comparator.comparing(Konyv::getCim, String.CASE_INSENSITIVE_ORDER);
    public static final Comparator<Konyv> EV_SZERINT =
            Comparator.comparingInt(Konyv::getKiadasEve).thenComparing(CIM_SZERINT);

    private final String isbn;
    private final String cim;
    private final String szerzo;
    private final int kiadasEve;
    private int peldanyszam; // how many copies the library owns

    public Konyv(String isbn, String cim, String szerzo, int kiadasEve, int peldanyszam) {
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN can't be empty.");
        }
        if (cim == null || cim.isBlank()) {
            throw new IllegalArgumentException("Title can't be empty.");
        }
        if (szerzo == null || szerzo.isBlank()) {
            throw new IllegalArgumentException("Author can't be empty.");
        }
        if (peldanyszam < 0) {
            throw new IllegalArgumentException("Number of copies can't be negative.");
        }
        this.isbn = isbn;
        this.cim = cim;
        this.szerzo = szerzo;
        this.kiadasEve = kiadasEve;
        this.peldanyszam = peldanyszam;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getCim() {
        return cim;
    }

    public String getSzerzo() {
        return szerzo;
    }

    public int getKiadasEve() {
        return kiadasEve;
    }

    public int getPeldanyszam() {
        return peldanyszam;
    }

    public void setPeldanyszam(int peldanyszam) {
        if (peldanyszam < 0) {
            throw new IllegalArgumentException("Number of copies can't be negative.");
        }
        this.peldanyszam = peldanyszam;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Konyv)) return false;
        Konyv other = (Konyv) o;
        return isbn.equals(other.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    @Override
    public String toString() {
        return "Konyv[" + isbn + ", " + cim + ", " + szerzo + ", " + kiadasEve + ", " + peldanyszam + " db]";
    }
}
