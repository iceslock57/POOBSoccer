package persistencia;

import Domain.EscritorPartida;
import Domain.ExcepcionJuego;
import Domain.LectorPartida;
import Domain.POOBSoccer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Formato con el que se salvan y se abren las partidas: datos de Java en un archivo .dat. */
public class ArchivoDat extends ArchivoPartida {

    public ArchivoDat() {
        super("dat", "Partida de DOPOSoccer (*.dat)", "DSDT");
    }

    @Override
    protected byte[] escribir(POOBSoccer juego) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream datos = new DataOutputStream(bytes)) {
            juego.guardar(new Salida(datos));
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
        return bytes.toByteArray();
    }

    @Override
    protected void leer(POOBSoccer juego, byte[] contenido) throws IOException {
        try (DataInputStream datos = new DataInputStream(new ByteArrayInputStream(contenido))) {
            juego.restaurar(new Entrada(datos));
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    /** Operación de archivo que puede fallar. */
    private interface Operacion<T> {
        T hacer() throws IOException;
    }

    private static <T> T intentar(Operacion<T> operacion) {
        try {
            return operacion.hacer();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static final class Salida implements EscritorPartida {
        private final DataOutputStream datos;

        Salida(DataOutputStream datos) {
            this.datos = datos;
        }

        /** En este formato todos los enteros ocupan lo mismo, así que no importa la cantidad de bits. */
        @Override
        public void entero(int valor, int bits) {
            intentar(() -> { datos.writeInt(valor); return null; });
        }

        @Override
        public void largo(long valor) {
            intentar(() -> { datos.writeLong(valor); return null; });
        }

        @Override
        public void decimal(double valor) {
            intentar(() -> { datos.writeDouble(valor); return null; });
        }

        @Override
        public void logico(boolean valor) {
            intentar(() -> { datos.writeBoolean(valor); return null; });
        }

        @Override
        public void texto(String valor) {
            intentar(() -> { datos.writeUTF(valor); return null; });
        }
    }

    private static final class Entrada implements LectorPartida {
        private final DataInputStream datos;

        Entrada(DataInputStream datos) {
            this.datos = datos;
        }

        @Override
        public int entero(int bits) {
            return intentar(datos::readInt);
        }

        @Override
        public long largo() {
            return intentar(datos::readLong);
        }

        @Override
        public double decimal() {
            return numeroValido(intentar(datos::readDouble));
        }

        @Override
        public boolean logico() {
            return intentar(datos::readBoolean);
        }

        @Override
        public String texto() {
            return intentar(datos::readUTF);
        }

        @Override
        public void terminar() {
            if (intentar(datos::available) > 0) throw new ExcepcionJuego("El archivo tiene datos de más");
        }
    }
}
