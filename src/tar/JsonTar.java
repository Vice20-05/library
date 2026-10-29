package tar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import model.Kolcsonzes;
import model.Konyv;
import model.Konyvtar;
import model.Olvaso;

/**
 * Storage (3, stretch): JSON with Gson.
 * We build the JSON by hand (JsonObject / JsonArray), so on loading every
 * object goes through its constructor -> the validation runs again.
 * Loans only store the ISBN and card number, not the whole book/reader.
 */
public class JsonTar implements KonyvtarTar {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void ment(Konyvtar k, Path fajl) throws IOException {
        JsonObject gyoker = new JsonObject();

        JsonArray konyvek = new JsonArray();
        for (Konyv konyv : k.getKonyvek()) {
            JsonObject o = new JsonObject();
            o.addProperty("isbn", konyv.getIsbn());
            o.addProperty("cim", konyv.getCim());
            o.addProperty("szerzo", konyv.getSzerzo());
            o.addProperty("kiadasEve", konyv.getKiadasEve());
            o.addProperty("peldanyszam", konyv.getPeldanyszam());
            konyvek.add(o);
        }

        JsonArray olvasok = new JsonArray();
        for (Olvaso olvaso : k.getOlvasok()) {
            JsonObject o = new JsonObject();
            o.addProperty("olvasojegySzam", olvaso.getOlvasojegySzam());
            o.addProperty("nev", olvaso.getNev());
            olvasok.add(o);
        }

        JsonArray kolcsonzesek = new JsonArray();
        for (Kolcsonzes kol : k.getKolcsonzesek()) {
            JsonObject o = new JsonObject();
            o.addProperty("isbn", kol.getKonyv().getIsbn());
            o.addProperty("olvasojegySzam", kol.getOlvaso().getOlvasojegySzam());
            o.addProperty("datum", kol.getDatum().toString());
            o.addProperty("hatarido", kol.getHatarido().toString());
            kolcsonzesek.add(o);
        }

        gyoker.add("konyvek", konyvek);
        gyoker.add("olvasok", olvasok);
        gyoker.add("kolcsonzesek", kolcsonzesek);

        Path tmp = BiztonsagosMentes.ideiglenesFajl(fajl);
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            gson.toJson(gyoker, w);
        } catch (IOException e) {
            BiztonsagosMentes.takarit(tmp);
            throw e;
        }
        BiztonsagosMentes.csere(tmp, fajl);
    }

    @Override
    public Konyvtar betolt(Path fajl) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(fajl, StandardCharsets.UTF_8)) {
            JsonObject gyoker = JsonParser.parseReader(r).getAsJsonObject();
            Konyvtar k = new Konyvtar();

            for (JsonElement e : gyoker.getAsJsonArray("konyvek")) {
                JsonObject o = e.getAsJsonObject();
                k.addKonyv(new Konyv(
                        o.get("isbn").getAsString(),
                        o.get("cim").getAsString(),
                        o.get("szerzo").getAsString(),
                        o.get("kiadasEve").getAsInt(),
                        o.get("peldanyszam").getAsInt()));
            }

            for (JsonElement e : gyoker.getAsJsonArray("olvasok")) {
                JsonObject o = e.getAsJsonObject();
                k.addOlvaso(new Olvaso(o.get("olvasojegySzam").getAsString(), o.get("nev").getAsString()));
            }

            for (JsonElement e : gyoker.getAsJsonArray("kolcsonzesek")) {
                JsonObject o = e.getAsJsonObject();
                String isbn = o.get("isbn").getAsString();
                String jegy = o.get("olvasojegySzam").getAsString();
                Konyv konyv = k.findKonyv(isbn)
                        .orElseThrow(() -> new IllegalArgumentException("unknown ISBN " + isbn));
                Olvaso olvaso = k.findOlvaso(jegy)
                        .orElseThrow(() -> new IllegalArgumentException("unknown reader " + jegy));
                k.addKolcsonzes(new Kolcsonzes(konyv, olvaso,
                        LocalDate.parse(o.get("datum").getAsString()),
                        LocalDate.parse(o.get("hatarido").getAsString())));
            }
            return k;
        } catch (IOException e) {
            throw e; // real I/O problem (e.g. file can't be read) -> pass it on
        } catch (RuntimeException e) {
            // broken JSON, missing field (NullPointerException), wrong type, bad date...
            throw new SerultFajlKivetel("The JSON file is damaged: " + fajl + " (" + e.getMessage() + ")", e);
        }
    }
}
