package tar;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.nio.file.Path;

import model.Konyvtar;

/**
 * Storage (1): Java serialization with ObjectOutputStream / ObjectInputStream.
 * The whole Konyvtar object is written as one binary object.
 */
public class SzerializaltTar implements KonyvtarTar {

    @Override
    public void ment(Konyvtar k, Path fajl) throws IOException {
        Path tmp = BiztonsagosMentes.ideiglenesFajl(fajl);
        // try-with-resources -> the stream is closed even if there is an exception
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            out.writeObject(k);
        } catch (IOException e) {
            BiztonsagosMentes.takarit(tmp);
            throw e;
        }
        BiztonsagosMentes.csere(tmp, fajl); // only after the stream was closed
    }

    @Override
    public Konyvtar betolt(Path fajl) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(fajl))) {
            Object o = in.readObject();
            if (!(o instanceof Konyvtar)) {
                throw new SerultFajlKivetel("The file doesn't contain a library: " + fajl);
            }
            return (Konyvtar) o;
        } catch (ObjectStreamException | ClassNotFoundException e) {
            // not a serialized file, or written by a different version of the classes
            throw new SerultFajlKivetel("The file is damaged or not a library save: " + fajl, e);
        } catch (EOFException e) {
            // the file ends too early (e.g. cut in half)
            throw new SerultFajlKivetel("The file is incomplete: " + fajl, e);
        }
    }
}
