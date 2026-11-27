package persistencia;

import Domain.ExcepcionJuego;
import Domain.POOBSoccer;
import Domain.RegistroErrores;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Guarda y recupera una partida en un archivo. Cada formato decide cómo se escribe el contenido; esta clase
 * le pone delante una marca, la versión y un código de revisión para reconocer archivos ajenos o dañados.
 */
public abstract class ArchivoPartida {

    private static final int VERSION = 1;
    private static final long TAMANO_MAXIMO = 1 << 20;

    private final String extension, descripcion, marca;

    /**
     * @param extension   terminación del archivo, sin el punto
     * @param descripcion texto que ve el usuario al elegir el archivo
     * @param marca       cuatro letras que identifican el formato
     */
    protected ArchivoPartida(String extension, String descripcion, String marca) {
        this.extension = extension;
        this.descripcion = descripcion;
        this.marca = marca;
    }

    /** Convierte la partida en el contenido propio del formato. */
    protected abstract byte[] escribir(POOBSoccer juego) throws IOException;

    /** Pone en el juego la partida que trae ese contenido. */
    protected abstract void leer(POOBSoccer juego, byte[] contenido) throws IOException;

    public void guardar(POOBSoccer juego, File archivo) {
        try {
            byte[] contenido = escribir(juego);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream datos = new DataOutputStream(bytes)) {
                datos.writeBytes(marca);
                datos.writeByte(VERSION);
                datos.writeLong(codigoDeRevision(contenido));
                datos.write(contenido);
            }
            reemplazar(archivo.toPath().toAbsolutePath(), bytes.toByteArray());
            RegistroErrores.anotar("Se guardó la partida en " + archivo.getName());
        } catch (IOException ex) {
            throw new ExcepcionJuego("No se pudo guardar la partida en " + archivo.getName(), ex);
        }
    }

    /** Escribe primero en un archivo temporal, para no dañar la partida anterior si algo falla a la mitad. */
    private static void reemplazar(Path destino, byte[] bytes) throws IOException {
        Path temporal = Files.createTempFile(destino.getParent(), "partida", ".tmp");
        try {
            Files.write(temporal, bytes);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public void abrir(POOBSoccer juego, File archivo) {
        byte[] contenido = leerArchivo(archivo);
        try {
            leer(juego, contenido);
            RegistroErrores.anotar("Se abrió la partida de " + archivo.getName());
        } catch (IOException | RuntimeException ex) {
            throw new ExcepcionJuego("La partida de " + archivo.getName() + " tiene datos que no son válidos", ex);
        }
    }

    /** Revisa que el archivo sea de este formato y esté completo, y devuelve su contenido. */
    private byte[] leerArchivo(File archivo) {
        if (!archivo.isFile()) throw new ExcepcionJuego("No existe el archivo " + archivo.getName());
        if (archivo.length() > TAMANO_MAXIMO) throw noEsUnaPartida(archivo, null);

        try (DataInputStream datos = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(archivo.toPath())))) {
            byte[] inicio = new byte[marca.length()];
            datos.readFully(inicio);
            if (!Arrays.equals(inicio, marca.getBytes())) throw noEsUnaPartida(archivo, null);

            int version = datos.readUnsignedByte();
            if (version != VERSION) throw new ExcepcionJuego("La partida es de otra versión del juego: " + version);

            long codigo = datos.readLong();
            byte[] contenido = datos.readAllBytes();
            if (codigo != codigoDeRevision(contenido)) {
                throw new ExcepcionJuego("El archivo " + archivo.getName() + " está dañado");
            }
            return contenido;
        } catch (IOException ex) {
            throw noEsUnaPartida(archivo, ex);
        }
    }

    private ExcepcionJuego noEsUnaPartida(File archivo, Throwable causa) {
        return new ExcepcionJuego("El archivo " + archivo.getName() + " no es una partida ." + extension, causa);
    }

    private static long codigoDeRevision(byte[] contenido) {
        CRC32 codigo = new CRC32();
        codigo.update(contenido);
        return codigo.getValue();
    }

    /** Rechaza los decimales con los que no se puede jugar, como los infinitos. */
    static double numeroValido(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) throw new ExcepcionJuego("Hay un número que no es válido");
        return valor;
    }

    /** Le agrega la terminación del formato al nombre si no la tiene. */
    public File conExtension(File archivo) {
        boolean yaLaTiene = archivo.getName().toLowerCase().endsWith("." + extension);
        return yaLaTiene ? archivo : new File(archivo.getPath() + "." + extension);
    }

    public String getExtension() {
        return extension;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
