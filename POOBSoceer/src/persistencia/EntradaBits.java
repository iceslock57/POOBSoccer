package persistencia;

import Domain.ExcepcionJuego;
import Domain.LectorPartida;
import java.nio.charset.StandardCharsets;

/** Lee bit por bit lo que escribió SalidaBits. */
class EntradaBits implements LectorPartida {

    private final byte[] contenido;
    private int posicion;

    EntradaBits(byte[] contenido) {
        this.contenido = contenido;
    }

    private int bit() {
        int numeroDeByte = posicion / 8;
        if (numeroDeByte >= contenido.length) throw new ExcepcionJuego("El archivo está incompleto");
        int valor = contenido[numeroDeByte] >> 7 - posicion % 8 & 1;
        posicion++;
        return valor;
    }

    private long bits(int cantidad) {
        long valor = 0;
        for (int i = 0; i < cantidad; i++) valor = valor << 1 | bit();
        return valor;
    }

    @Override
    public int entero(int bits) {
        return (int) bits(bits);
    }

    @Override
    public long largo() {
        return bits(64);
    }

    @Override
    public double decimal() {
        return ArchivoPartida.numeroValido(Double.longBitsToDouble(bits(64)));
    }

    @Override
    public boolean logico() {
        return bit() == 1;
    }

    /** Después del último dato solo pueden quedar los ceros que completan el byte. */
    @Override
    public void terminar() {
        if (contenido.length * 8 - posicion >= 8) throw new ExcepcionJuego("El archivo tiene datos de más");
    }

    @Override
    public String texto() {
        byte[] letras = new byte[entero(8)];
        for (int i = 0; i < letras.length; i++) letras[i] = (byte) bits(8);
        return new String(letras, StandardCharsets.UTF_8);
    }
}
