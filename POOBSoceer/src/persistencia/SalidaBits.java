package persistencia;

import Domain.EscritorPartida;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Escribe los datos bit por bit, uno detrás de otro y sin dejar espacios.
 * Un nivel de 1 a 10, por ejemplo, ocupa cuatro bits en lugar de un entero completo.
 */
class SalidaBits implements EscritorPartida {

    private static final int MAXIMO_TEXTO = 255;

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private int actual, usados;

    private void bit(int valor) {
        actual = actual << 1 | valor;
        usados++;
        if (usados == 8) {
            salida.write(actual);
            actual = 0;
            usados = 0;
        }
    }

    /** Empieza por el bit de mayor valor. */
    private void bits(long valor, int cantidad) {
        for (int i = cantidad - 1; i >= 0; i--) bit((int) (valor >>> i & 1));
    }

    @Override
    public void entero(int valor, int bits) {
        if (valor < 0 || bits < 31 && valor >>> bits != 0) {
            throw new IllegalArgumentException("El valor " + valor + " no cabe en " + bits + " bits");
        }
        bits(valor, bits);
    }

    @Override
    public void largo(long valor) {
        bits(valor, 64);
    }

    @Override
    public void decimal(double valor) {
        bits(Double.doubleToLongBits(valor), 64);
    }

    @Override
    public void logico(boolean valor) {
        bit(valor ? 1 : 0);
    }

    /** Primero la cantidad de letras en ocho bits y luego las letras. */
    @Override
    public void texto(String valor) {
        byte[] letras = valor.getBytes(StandardCharsets.UTF_8);
        if (letras.length > MAXIMO_TEXTO) throw new IllegalArgumentException("El texto es demasiado largo");
        entero(letras.length, 8);
        for (byte letra : letras) bits(letra & 0xFF, 8);
    }

    /** Lo escrito hasta ahora. Si el último byte quedó a medias se completa con ceros. */
    byte[] getContenido() {
        while (usados != 0) bit(0);
        return salida.toByteArray();
    }
}
