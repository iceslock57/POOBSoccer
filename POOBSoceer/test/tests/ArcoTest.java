package tests;

import Domain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de Arco.contieneBalon y de sus getters.
 * Geometria de referencia (radio 10): el arco izquierdo contiene el balon si queda
 * completamente a la izquierda de posX, y el derecho si queda a la derecha de posX+ancho.
 * Verticalmente el balon debe estar dentro del hueco, con 5 px de margen por cada lado.
 */
public class ArcoTest {

    private static final int R = 10;
    private static final int[][] TAMANOS = {{800, 550}, {1000, 650}, {1200, 750}};

    // Izquierdo: posX=100, hueco vertical valido para el centro: (215, 335) con R=10
    private static final Arco IZQ = new Arco(100, 200, 50, 150, "Corea", true);
    // Derecho: borde interior en x=900
    private static final Arco DER = new Arco(850, 200, 50, 150, "Camerun", false);
    private static final double CENTRO_Y = 275;

    // Apoyo

    private static Balon balon(double x, double y, int radio) {
        return new Balon(x, y, radio);
    }

    /** X comodamente dentro del arco (lejos de los limites horizontales). */
    private static double xDentro(Arco a) {
        return a.isEsArcoIzquierdo() ? a.getPosX() - R - 20 : a.getPosX() + a.getAncho() + R + 20;
    }

    private static int contarVerdaderos(Arco a, double desde, double hasta, boolean barridoVertical) {
        int n = 0;
        for (int v = (int) desde; v <= hasta; v++) {
            Balon b = barridoVertical ? balon(xDentro(a), v, R) : balon(v, CENTRO_Y, R);
            if (a.contieneBalon(b)) n++;
        }
        return n;
    }

    // Getters

    @Test
    public void gettersDevuelvenLosValoresDelConstructor() {
        assertEquals(100, IZQ.getPosX(), "posX izquierdo");
        assertEquals(200, IZQ.getPosY(), "posY izquierdo");
        assertEquals(50, IZQ.getAncho(), "ancho izquierdo");
        assertEquals(150, IZQ.getAlto(), "alto izquierdo");
        assertEquals("Corea", IZQ.getEquipoPropietario(), "equipo izquierdo");
        assertTrue(IZQ.isEsArcoIzquierdo(), "bandera izquierda");
        assertEquals(850, DER.getPosX(), "posX derecho");
        assertEquals("Camerun", DER.getEquipoPropietario(), "equipo derecho");
        assertFalse(DER.isEsArcoIzquierdo(), "bandera derecha");
        assertNull(new Arco(0, 0, 1, 1, null, true).getEquipoPropietario(), "El equipo nulo se conserva");
    }

    // Travesaño

    /** Deja caer un balón sobre el palo de arriba de cada arco y lo sigue unos cuadros. */
    @Test
    public void elBalonQueCaeSobreElTravesanoRebotaYNoEsGol() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            for (Arco a : new Arco[]{c.getArcoIzquierdo(), c.getArcoDerecho()}) {
                Balon b = new Balon(a.getPosX() + a.getAncho() / 2.0, a.getPosY() - 60, 16);
                b.aplicarFuerza(a.isEsArcoIzquierdo() ? -1 : 1, 0);
                boolean reboto = false;
                for (int i = 0; i < 40; i++) {
                    b.aplicarGravedad();
                    b.mover();
                    b.rebotar(c);
                    assertNull(c.esGol(b), "Por encima del arco no es gol (" + t[0] + ")");
                    assertTrue(b.getPosY() + b.getRadio() <= a.getPosY(), "No atraviesa el palo (" + t[0] + ")");
                    reboto |= b.getVelY() < 0;
                }
                assertTrue(reboto, "Rebota hacia arriba (" + t[0] + ")");
            }
        }
    }

    @Test
    public void desdeAdentroElTravesanoLoDevuelveHaciaAbajo() {
        Cancha c = new Cancha(1000, 650);
        Arco a = c.getArcoIzquierdo();
        Balon b = new Balon(a.getAncho() / 2.0, a.getPosY() + 20, 16);
        b.aplicarFuerza(0, -8);
        b.mover();
        b.rebotar(c);
        assertTrue(b.getVelY() > 0, "Sale rebotado hacia abajo");
        assertTrue(b.getPosY() - b.getRadio() >= a.getPosY(), "Queda debajo del palo");
    }

    // Eje horizontal

    @Test
    public void arcoIzquierdoContieneBalonCompletamenteDetrasDeLaLinea() {
        for (double x : new double[]{90, 50, 0, -500}) {
            assertTrue(IZQ.contieneBalon(balon(x, CENTRO_Y, R)), "Izquierdo contiene x=" + x);
        }
    }

    @Test
    public void arcoIzquierdoRespetaElLimiteHorizontalExacto() {
        assertTrue(IZQ.contieneBalon(balon(90, CENTRO_Y, R)), "x+r == posX es valido (<=)");
        assertFalse(IZQ.contieneBalon(balon(90.0001, CENTRO_Y, R)), "Un pelo por encima del limite");
        assertFalse(IZQ.contieneBalon(balon(100, CENTRO_Y, R)), "Centro sobre la linea");
        assertFalse(IZQ.contieneBalon(balon(175, CENTRO_Y, R)), "Balon delante del arco");
    }

    @Test
    public void arcoDerechoContieneBalonYRespetaElLimiteHorizontalExacto() {
        for (double x : new double[]{910, 950, 1500}) {
            assertTrue(DER.contieneBalon(balon(x, CENTRO_Y, R)), "Derecho contiene x=" + x);
        }
        assertFalse(DER.contieneBalon(balon(909.9999, CENTRO_Y, R)), "Un pelo antes del limite (x-r >= posX+ancho)");
        assertFalse(DER.contieneBalon(balon(850, CENTRO_Y, R)), "Centro sobre posX");
        assertFalse(DER.contieneBalon(balon(800, CENTRO_Y, R)), "Balon delante del arco");
    }

    // Eje vertical y radio

    @Test
    public void limiteSuperiorVerticalEsEstricto() {
        for (Arco a : new Arco[]{IZQ, DER}) {
            double x = xDentro(a);
            assertFalse(a.contieneBalon(balon(x, 215, R)), "y-r == posY+5 no cuenta (>)");
            assertTrue(a.contieneBalon(balon(x, 215.0001, R)), "Un pelo por debajo del travesano");
            assertFalse(a.contieneBalon(balon(x, 214, R)), "Tocando el travesano");
        }
    }

    @Test
    public void limiteInferiorVerticalEsEstricto() {
        for (Arco a : new Arco[]{IZQ, DER}) {
            double x = xDentro(a);
            assertFalse(a.contieneBalon(balon(x, 335, R)), "y+r == posY+alto-5 no cuenta (<)");
            assertTrue(a.contieneBalon(balon(x, 334.9999, R)), "Un pelo por encima del limite inferior");
            assertFalse(a.contieneBalon(balon(x, 336, R)), "Por debajo del hueco");
        }
    }

    @Test
    public void balonFueraDelHuecoVerticalNoEsGol() {
        for (Arco a : new Arco[]{IZQ, DER}) {
            for (double y : new double[]{0, 100, 199, 205, 345, 400, 650}) {
                assertFalse(a.contieneBalon(balon(xDentro(a), y, R)), "Fuera del hueco y=" + y);
            }
        }
    }

    @Test
    public void elRadioInfluyeEnAmbosEjes() {
        // Vertical: radio 69 cabe justo en el hueco de 140 px; radio 70 ya no cabe
        assertTrue(IZQ.contieneBalon(balon(0, CENTRO_Y, 69)), "Radio 69 centrado cabe");
        assertFalse(IZQ.contieneBalon(balon(0, CENTRO_Y, 70)), "Radio 70 no cabe nunca");
        assertTrue(IZQ.contieneBalon(balon(0, CENTRO_Y, 1)), "Radio 1 cabe");
        // Horizontal: x=95 -> con radio 5 el borde toca posX, con radio 6 lo cruza
        assertTrue(IZQ.contieneBalon(balon(95, CENTRO_Y, 5)), "x+r == posX con radio 5");
        assertFalse(IZQ.contieneBalon(balon(95, CENTRO_Y, 6)), "x+r > posX con radio 6");
        assertTrue(DER.contieneBalon(balon(905, CENTRO_Y, 5)), "x-r == posX+ancho con radio 5");
        assertFalse(DER.contieneBalon(balon(905, CENTRO_Y, 6)), "x-r < posX+ancho con radio 6");
    }

    // Lado, entradas raras e inmutabilidad

    @Test
    public void laBanderaIzquierdoDefineDeQueLadoSeConsideraGol() {
        Arco izq = new Arco(100, 200, 50, 150, "A", true);
        Arco der = new Arco(100, 200, 50, 150, "B", false);
        Balon izquierda = balon(0, CENTRO_Y, R);
        Balon derecha = balon(300, CENTRO_Y, R);
        assertTrue(izq.contieneBalon(izquierda), "Flag true: cuenta a la izquierda");
        assertFalse(der.contieneBalon(izquierda), "Flag false: no cuenta a la izquierda");
        assertFalse(izq.contieneBalon(derecha), "Flag true: no cuenta a la derecha");
        assertTrue(der.contieneBalon(derecha), "Flag false: cuenta a la derecha");
    }

    @Test
    public void balonNuloLanzaNullPointerException() {
        assertThrows(NullPointerException.class, () -> IZQ.contieneBalon(null), "Izquierdo con null");
        assertThrows(NullPointerException.class, () -> DER.contieneBalon(null), "Derecho con null");
    }

    @Test
    public void posicionesNoNumericasNuncaCuentanComoGol() {
        for (Arco a : new Arco[]{IZQ, DER}) {
            assertFalse(a.contieneBalon(balon(Double.NaN, CENTRO_Y, R)), "x NaN");
            assertFalse(a.contieneBalon(balon(xDentro(a), Double.NaN, R)), "y NaN");
        }
    }

    @Test
    public void contieneBalonNoModificaAlBalonYEsRepetible() {
        Balon b = balon(50, CENTRO_Y, R);
        boolean primero = IZQ.contieneBalon(b);
        for (int i = 0; i < 5; i++) assertEquals(primero, IZQ.contieneBalon(b), "Resultado estable");
        assertEquals(50, b.getPosX(), 0, "posX intacto");
        assertEquals(CENTRO_Y, b.getPosY(), 0, "posY intacto");
        assertEquals(R, b.getRadio(), "Radio intacto");
    }

    // Geometrias especiales

    @Test
    public void arcoSinHuecoVerticalNuncaContieneBalon() {
        // Con alto <= 10 los margenes de 5 px por lado cierran el hueco
        for (int alto : new int[]{0, 5, 10}) {
            Arco izq = new Arco(100, 200, 50, alto, "X", true);
            Arco der = new Arco(100, 200, 50, alto, "Y", false);
            for (int y = 150; y <= 300; y++) {
                assertFalse(izq.contieneBalon(balon(0, y, 1)), "Izquierdo alto=" + alto + " y=" + y);
                assertFalse(der.contieneBalon(balon(500, y, 1)), "Derecho alto=" + alto + " y=" + y);
            }
        }
    }

    @Test
    public void funcionaConCoordenadasCeroNegativasYAnchoCero() {
        Arco derCero = new Arco(0, 0, 0, 100, "Z", false);   // limite: x-r >= 0
        assertTrue(derCero.contieneBalon(balon(10, 50, R)), "Derecho en origen, x-r == 0");
        assertFalse(derCero.contieneBalon(balon(9.9, 50, R)), "Derecho en origen, antes del limite");

        Arco izqNeg = new Arco(-50, -100, 50, 150, "W", true); // x+r <= -50, y en (-85, 35)
        assertTrue(izqNeg.contieneBalon(balon(-60, 0, R)), "Izquierdo negativo, limite exacto");
        assertFalse(izqNeg.contieneBalon(balon(-59.9, 0, R)), "Izquierdo negativo, pasado el limite");
        assertFalse(izqNeg.contieneBalon(balon(-60, 40, R)), "Izquierdo negativo, fuera del hueco");
    }

    // Barridos

    @Test
    public void barridoVerticalAceptaExactamenteElHuecoEsperado() {
        // Con R=10 los enteros validos son 216..334 -> 119 valores, en ambos arcos
        for (Arco a : new Arco[]{IZQ, DER}) {
            assertEquals(119, contarVerdaderos(a, 0, 650, true), "Cantidad de y validas");
            assertFalse(a.contieneBalon(balon(xDentro(a), 215, R)), "Primer valor invalido");
            assertTrue(a.contieneBalon(balon(xDentro(a), 216, R)), "Primer valor valido");
            assertTrue(a.contieneBalon(balon(xDentro(a), 334, R)), "Ultimo valor valido");
            assertFalse(a.contieneBalon(balon(xDentro(a), 335, R)), "Primer valor invalido por debajo");
        }
    }

    @Test
    public void barridoHorizontalAceptaExactamenteLaZonaEsperada() {
        // Izquierdo: x enteros en [-100, 300] validos hasta 90 -> 191 valores
        assertEquals(191, contarVerdaderos(IZQ, -100, 300, false), "Cantidad de x validas (izquierdo)");
        // Derecho: x enteros en [700, 1100] validos desde 910 -> 191 valores
        assertEquals(191, contarVerdaderos(DER, 700, 1100, false), "Cantidad de x validas (derecho)");
    }

    // Integracion con Cancha

    @Test
    public void losArcosDeLaCanchaSonConsistentesConContieneBalon() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            Arco izq = c.getArcoIzquierdo();
            Arco der = c.getArcoDerecho();
            String ctx = " (" + t[0] + ")";
            assertTrue(izq.isEsArcoIzquierdo(), "Bandera del arco izquierdo" + ctx);
            assertFalse(der.isEsArcoIzquierdo(), "Bandera del arco derecho" + ctx);
            assertTrue(izq.getPosX() < der.getPosX(), "El izquierdo esta a la izquierda del derecho" + ctx);
            assertTrue(izq.getAncho() > 0 && izq.getAlto() > 0, "Dimensiones positivas izquierdo" + ctx);
            assertTrue(der.getAncho() > 0 && der.getAlto() > 0, "Dimensiones positivas derecho" + ctx);
            assertTrue(izq.getAlto() <= t[1] && der.getAlto() <= t[1], "Los arcos caben en la cancha" + ctx);
            assertNotNull(izq.getEquipoPropietario(), "Equipo izquierdo" + ctx);
            assertNotNull(der.getEquipoPropietario(), "Equipo derecho" + ctx);

            double yIzq = izq.getPosY() + izq.getAlto() / 2.0;
            double yDer = der.getPosY() + der.getAlto() / 2.0;
            Balon enIzq = balon(izq.getPosX() - R - 1, yIzq, R);
            Balon enDer = balon(der.getPosX() + der.getAncho() + R + 1, yDer, R);
            assertTrue(izq.contieneBalon(enIzq), "El arco izquierdo contiene un balon detras de su linea" + ctx);
            assertTrue(der.contieneBalon(enDer), "El arco derecho contiene un balon detras de su linea" + ctx);
            // Cancha.esGol también comprueba el sentido del movimiento, no solo la posición del balón.
            enIzq.aplicarFuerza(-1, 0);
            enDer.aplicarFuerza(1, 0);
            assertSame(izq, c.esGol(enIzq), "Cancha.esGol coincide con el arco izquierdo" + ctx);
            assertSame(der, c.esGol(enDer), "Cancha.esGol coincide con el arco derecho" + ctx);
        }
    }
}