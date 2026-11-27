package persistencia;

import Domain.ExcepcionJuego;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la escritura y la lectura bit por bit. */
public class BitsTest {

    private SalidaBits salida;

    @BeforeEach
    public void preparar() {
        salida = new SalidaBits();
    }

    private EntradaBits entrada() {
        return new EntradaBits(salida.getContenido());
    }

    @Test
    public void sinDatosNoOcupaNada() {
        assertEquals(0, salida.getContenido().length);
    }

    @Test
    public void losBitsSeEmpaquetanSinEspacios() {
        salida.entero(0b1010, 4);
        salida.entero(0b0011, 4);
        assertArrayEquals(new byte[]{(byte) 0b10100011}, salida.getContenido());
    }

    @Test
    public void elUltimoByteSeCompletaConCeros() {
        salida.entero(0b101, 3);
        assertArrayEquals(new byte[]{(byte) 0b10100000}, salida.getContenido());
    }

    @Test
    public void ochoDatosLogicosCabenEnUnByte() {
        for (int i = 0; i < 8; i++) salida.logico(i % 2 == 0);
        assertArrayEquals(new byte[]{(byte) 0b10101010}, salida.getContenido());
    }

    @Test
    public void unDatoPuedeQuedarRepartidoEntreDosBytes() {
        salida.logico(true);
        salida.entero(300, 10);
        EntradaBits entrada = entrada();
        assertTrue(entrada.logico());
        assertEquals(300, entrada.entero(10));
    }

    @Test
    public void recuperaLosEnterosDeCualquierTamano() {
        int[][] datos = {{0, 1}, {1, 1}, {3, 2}, {10, 4}, {255, 8}, {65535, 16}, {Integer.MAX_VALUE, 31}};
        for (int[] d : datos) salida.entero(d[0], d[1]);
        EntradaBits entrada = entrada();
        for (int[] d : datos) assertEquals(d[0], entrada.entero(d[1]));
    }

    @Test
    public void recuperaLosLargosYLosDecimales() {
        salida.largo(Long.MAX_VALUE);
        salida.largo(-1);
        salida.decimal(-12.75);
        salida.decimal(Math.PI);
        EntradaBits entrada = entrada();
        assertEquals(Long.MAX_VALUE, entrada.largo());
        assertEquals(-1, entrada.largo());
        assertEquals(-12.75, entrada.decimal());
        assertEquals(Math.PI, entrada.decimal(), "Sin perder precisión");
    }

    @Test
    public void recuperaLosTextosConTildes() {
        salida.texto("Japón");
        salida.texto("");
        salida.texto("Camerún");
        EntradaBits entrada = entrada();
        assertEquals("Japón", entrada.texto());
        assertEquals("", entrada.texto());
        assertEquals("Camerún", entrada.texto());
    }

    @Test
    public void unValorQueNoCabeSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> salida.entero(16, 4));
        assertThrows(IllegalArgumentException.class, () -> salida.entero(-1, 8));
    }

    @Test
    public void unTextoDemasiadoLargoSeRechaza() {
        assertDoesNotThrow(() -> salida.texto("a".repeat(255)));
        assertThrows(IllegalArgumentException.class, () -> salida.texto("a".repeat(256)));
    }

    @Test
    public void leerMasDeLoEscritoSeRechaza() {
        salida.entero(5, 8);
        EntradaBits entrada = entrada();
        entrada.entero(8);
        assertThrows(ExcepcionJuego.class, () -> entrada.entero(1));
    }

    @Test
    public void unDecimalConElQueNoSePuedeJugarSeRechaza() {
        for (double malo : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            SalidaBits otra = new SalidaBits();
            otra.decimal(malo);
            assertThrows(ExcepcionJuego.class, () -> new EntradaBits(otra.getContenido()).decimal());
        }
    }
}
