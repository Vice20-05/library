package tar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import model.Kolcsonzes;
import model.Konyv;
import model.Konyvtar;
import model.Olvaso;

/**
 * Storage (2): our own CSV format, one record per line, ';' between the fields.
 *
 *   # KONYVTAR CSV v1
 *   KONYV;isbn;title;author;year;copies
 *   OLVASO;cardNumber;name
 *   KOLCSONZES;isbn;cardNumber;borrowDate;dueDate
 *
 * Special characters in the text are escaped:  ';' -> "\;"   '\' -> "\\"   newline -> "\n"
 */
public class CsvTar implements KonyvtarTar {

    static final String FEJLEC = "# KONYVTAR CSV v1";

    @Override
    public void ment(Konyvtar k, Path fajl) throws IOException {
        Path tmp = BiztonsagosMentes.ideiglenesFajl(fajl);
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(FEJLEC);
            w.newLine();
            // books and readers first, because the loans refer to them
            for (Konyv konyv : k.getKonyvek()) {
                sorIr(w, "KONYV", konyv.getIsbn(), konyv.getCim(), konyv.getSzerzo(),
                        String.valueOf(konyv.getKiadasEve()), String.valueOf(konyv.getPeldanyszam()));
            }
            for (Olvaso o : k.getOlvasok()) {
                sorIr(w, "OLVASO", o.getOlvasojegySzam(), o.getNev());
            }
            for (Kolcsonzes kol : k.getKolcsonzesek()) {
                sorIr(w, "KOLCSONZES", kol.getKonyv().getIsbn(), kol.getOlvaso().getOlvasojegySzam(),
                        kol.getDatum().toString(), kol.getHatarido().toString());
            }
        } catch (IOException e) {
            BiztonsagosMentes.takarit(tmp);
            throw e;
        }
        BiztonsagosMentes.csere(tmp, fajl);
    }

    @Override
    public Konyvtar betolt(Path fajl) throws IOException {
        Konyvtar k = new Konyvtar();
        try (BufferedReader r = Files.newBufferedReader(fajl, StandardCharsets.UTF_8)) {
            String elso = r.readLine();
            if (elso == null || !elso.equals(FEJLEC)) {
                throw new SerultFajlKivetel("Not a library CSV file (missing header): " + fajl);
            }

            String sor;
            int sorSzam = 1;
            while ((sor = r.readLine()) != null) {
                sorSzam++;
                if (sor.isBlank()) {
                    continue; // empty lines are allowed
                }
                try {
                    sorFeldolgoz(k, feldarabol(sor));
                } catch (IllegalArgumentException | IllegalStateException | DateTimeParseException e) {
                    // NumberFormatException is also an IllegalArgumentException
                    throw new SerultFajlKivetel("Error in line " + sorSzam + ": " + e.getMessage(), e);
                }
            }
        } catch (MalformedInputException e) {
            throw new SerultFajlKivetel("The file is not a text file: " + fajl, e);
        }
        return k;
    }

    // one line -> one object added to the library
    private void sorFeldolgoz(Konyvtar k, List<String> m) {
        switch (m.get(0)) {
            case "KONYV" -> {
                mezoSzam(m, 6);
                k.addKonyv(new Konyv(m.get(1), m.get(2), m.get(3),
                        Integer.parseInt(m.get(4)), Integer.parseInt(m.get(5))));
            }
            case "OLVASO" -> {
                mezoSzam(m, 3);
                k.addOlvaso(new Olvaso(m.get(1), m.get(2)));
            }
            case "KOLCSONZES" -> {
                mezoSzam(m, 5);
                Konyv konyv = k.findKonyv(m.get(1))
                        .orElseThrow(() -> new IllegalArgumentException("unknown ISBN " + m.get(1)));
                Olvaso olvaso = k.findOlvaso(m.get(2))
                        .orElseThrow(() -> new IllegalArgumentException("unknown reader " + m.get(2)));
                k.addKolcsonzes(new Kolcsonzes(konyv, olvaso,
                        LocalDate.parse(m.get(3)), LocalDate.parse(m.get(4))));
            }
            default -> throw new IllegalArgumentException("unknown record type \"" + m.get(0) + "\"");
        }
    }

    private void mezoSzam(List<String> m, int vart) {
        if (m.size() != vart) {
            throw new IllegalArgumentException(m.get(0) + " needs " + vart + " fields, found " + m.size());
        }
    }

    // ---------- escaping ----------

    private void sorIr(BufferedWriter w, String... mezok) throws IOException {
        for (int i = 0; i < mezok.length; i++) {
            if (i > 0) {
                w.write(';');
            }
            w.write(vedes(mezok[i]));
        }
        w.newLine();
    }

    // "Tom; Jerry" -> "Tom\; Jerry"
    static String vedes(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }

    /**
     * Splits a line at the ';' characters, but not at "\;".
     * Goes through the line character by character.
     */
    static List<String> feldarabol(String sor) {
        List<String> mezok = new ArrayList<>();
        StringBuilder aktualis = new StringBuilder();
        for (int i = 0; i < sor.length(); i++) {
            char c = sor.charAt(i);
            if (c == '\\' && i + 1 < sor.length()) {
                char kov = sor.charAt(++i); // the escaped character
                aktualis.append(kov == 'n' ? '\n' : kov);
            } else if (c == ';') {
                mezok.add(aktualis.toString()); // end of a field
                aktualis.setLength(0);
            } else {
                aktualis.append(c);
            }
        }
        mezok.add(aktualis.toString()); // the last field
        return mezok;
    }
}
