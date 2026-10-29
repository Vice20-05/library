package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A reader. Two readers are the same if their library card number is the same.
 */
public class Olvaso implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String olvasojegySzam;
    private final String nev;

    public Olvaso(String olvasojegySzam, String nev) {
        if (olvasojegySzam == null || olvasojegySzam.isBlank()) {
            throw new IllegalArgumentException("Card number can't be empty.");
        }
        if (nev == null || nev.isBlank()) {
            throw new IllegalArgumentException("Name can't be empty.");
        }
        this.olvasojegySzam = olvasojegySzam;
        this.nev = nev;
    }

    public String getOlvasojegySzam() {
        return olvasojegySzam;
    }

    public String getNev() {
        return nev;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Olvaso)) return false;
        Olvaso other = (Olvaso) o;
        return olvasojegySzam.equals(other.olvasojegySzam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(olvasojegySzam);
    }

    @Override
    public String toString() {
        return "Olvaso[" + olvasojegySzam + ", " + nev + "]";
    }
}
