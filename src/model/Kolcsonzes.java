package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One loan: which book, who has it, when it was borrowed and when it is due.
 */
public class Kolcsonzes implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int KOLCSONZESI_IDO_NAP = 14; // loan period in days
    public static final int NAPI_DIJ = 100;           // late fee in Ft per day

    private final Konyv konyv;
    private final Olvaso olvaso;
    private final LocalDate datum;
    private final LocalDate hatarido;

    public Kolcsonzes(Konyv konyv, Olvaso olvaso, LocalDate datum, LocalDate hatarido) {
        if (konyv == null || olvaso == null || datum == null || hatarido == null) {
            throw new IllegalArgumentException("Every field of a loan is required.");
        }
        if (hatarido.isBefore(datum)) {
            throw new IllegalArgumentException("The due date can't be before the borrow date.");
        }
        this.konyv = konyv;
        this.olvaso = olvaso;
        this.datum = datum;
        this.hatarido = hatarido;
    }

    public Konyv getKonyv() {
        return konyv;
    }

    public Olvaso getOlvaso() {
        return olvaso;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public LocalDate getHatarido() {
        return hatarido;
    }

    // how many days late on the given day (0 if not late)
    public long kesesNapok(LocalDate ma) {
        long napok = ChronoUnit.DAYS.between(hatarido, ma);
        return Math.max(0, napok);
    }

    // late fee in Ft on the given day
    public long kesedelmiDij(LocalDate ma) {
        return kesesNapok(ma) * NAPI_DIJ;
    }

    @Override
    public String toString() {
        return "Kolcsonzes[" + konyv.getIsbn() + " (" + konyv.getCim() + "), "
                + olvaso.getOlvasojegySzam() + " (" + olvaso.getNev() + "), "
                + datum + " -> " + hatarido + "]";
    }
}
