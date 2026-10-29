package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the model: search, sorting, borrowing rules, late fee, equals.
 */
public class KonyvtarTest {

    private Konyvtar k;
    private final LocalDate nap = LocalDate.of(2026, 10, 1);

    @BeforeEach
    public void setUp() {
        k = new Konyvtar();
        k.addKonyv(new Konyv("111", "Effective Java", "Joshua Bloch", 2018, 2));
        k.addKonyv(new Konyv("222", "Clean Code", "Robert Martin", 2008, 1));
        k.addKonyv(new Konyv("333", "Head First Java", "Kathy Sierra", 2005, 1));
        k.addOlvaso(new Olvaso("R1", "Anna"));
        k.addOlvaso(new Olvaso("R2", "Peter"));
    }

    // ---------- search & sort ----------

    @Test
    public void searchIsCaseInsensitiveOnTitle() {
        List<Konyv> t = k.keres("jAvA");
        assertEquals(2, t.size()); // Effective Java + Head First Java
    }

    @Test
    public void searchAlsoLooksAtAuthor() {
        List<Konyv> t = k.keres("martin");
        assertEquals(1, t.size());
        assertEquals("222", t.get(0).getIsbn());
    }

    @Test
    public void sortByTitleAndByYear() {
        assertEquals(List.of("Clean Code", "Effective Java", "Head First Java"),
                k.rendezveCimSzerint().stream().map(Konyv::getCim).toList());
        assertEquals(List.of(2005, 2008, 2018),
                k.rendezveEvSzerint().stream().map(Konyv::getKiadasEve).toList());
    }

    // ---------- borrowing ----------

    @Test
    public void noFreeCopyThrows() {
        k.kolcsonoz("222", "R1", nap); // the only copy
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> k.kolcsonoz("222", "R2", nap));
        assertEquals("There is no free copy of \"Clean Code\".", e.getMessage());
    }

    @Test
    public void returningFreesTheCopy() {
        k.kolcsonoz("222", "R1", nap);
        assertEquals(0, k.szabadPeldany(k.findKonyv("222").get()));
        k.visszahoz("222", "R1", nap.plusDays(3));
        assertEquals(1, k.szabadPeldany(k.findKonyv("222").get()));
    }

    @Test
    public void dueDateIs14DaysLater() {
        Kolcsonzes kol = k.kolcsonoz("111", "R1", nap);
        assertEquals(LocalDate.of(2026, 10, 15), kol.getHatarido());
    }

    // ---------- late fee ----------

    @Test
    public void noFeeWhenReturnedOnTime() {
        k.kolcsonoz("111", "R1", nap);
        assertEquals(0, k.visszahoz("111", "R1", nap.plusDays(14))); // exactly on the due date
    }

    @Test
    public void lateFeeIsCountedPerDay() {
        k.kolcsonoz("111", "R1", nap);
        // due date + 3 days -> 3 * 100 Ft
        assertEquals(300, k.visszahoz("111", "R1", nap.plusDays(14 + 3)));
    }

    // ---------- equals / hashCode ----------

    @Test
    public void booksAreEqualByIsbn() {
        Konyv a = new Konyv("999", "A", "X", 2000, 1);
        Konyv b = new Konyv("999", "Totally different", "Y", 1990, 5);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Konyv("998", "A", "X", 2000, 1));
    }

    @Test
    public void readersAreEqualByCardNumber() {
        assertEquals(new Olvaso("R1", "Anna"), new Olvaso("R1", "Someone Else"));
    }

    @Test
    public void sameIsbnCantBeAddedTwice() {
        assertThrows(IllegalArgumentException.class,
                () -> k.addKonyv(new Konyv("111", "Copy", "Someone", 2020, 1)));
    }
}
