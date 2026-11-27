package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regresion de los requisitos de la Version 1: fisica de la cancha, goles y poderes. */
public class RegresionVersionUnoTest {

    private static final int[][] TAMANOS = {{800, 550}, {1000, 650}, {1200, 750}};
    private static final int RADIO = 10;

    private Jugador j1;
    private Jugador j2;
    private PartidoPorGoles partido;

    @BeforeEach
    public void preparar() {
        Personaje corea = new Personaje("Corea", "Corea del Sur", 3, 3, 3, new PoderAuraAzul());
        Personaje camerun = new Personaje("Camerun", "Camerún", 3, 3, 3, new PoderRayo());
        j1 = new JugadorHumano("Corea", corea, new String[]{"A", "D", "W", "S", "Q"});
        j2 = new JugadorHumano("Camerun", camerun, new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
        partido = new PartidoPorGoles(j1, j2, 5);
    }

    // Apoyo

    private static double centroY(Arco arco) {
        return arco.getPosY() + arco.getAlto() / 2.0;
    }

    private static Balon balonEnGolIzquierdo(Cancha c) {
        Arco izq = c.getArcoIzquierdo();
        Balon b = new Balon(izq.getPosX() + izq.getAncho() + 8, centroY(izq), RADIO);
        b.aplicarFuerza(-12, 0);
        b.mover();
        return b;
    }

    private static Balon balonEnGolDerecho(Cancha c) {
        Arco der = c.getArcoDerecho();
        Balon b = new Balon(der.getPosX() - 8, centroY(der), RADIO);
        b.aplicarFuerza(12, 0);
        b.mover();
        return b;
    }

    // Techo

    @Test
    public void balonNoAtraviesaElTecho() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            Balon b = new Balon(t[0] / 2.0, 12, RADIO);
            b.aplicarFuerza(0, -8);
            b.mover();
            assertTrue(b.getPosY() < b.getRadio(), "Precondicion: el balon debe haber salido del techo (" + t[0] + ")");
            b.rebotar(c);
            assertTrue(b.getPosY() >= b.getRadio(), "El balon atraviesa el techo (" + t[0] + ")");
        }
    }

    @Test
    public void balonRebotaHaciaAbajoEnElTecho() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            Balon b = new Balon(t[0] / 2.0, 12, RADIO);
            b.aplicarFuerza(0, -8);
            b.mover();
            b.rebotar(c);
            assertTrue(b.getVelY() > 0, "No rebota hacia abajo en el techo (" + t[0] + ")");
        }
    }

    @Test
    public void balonEnCasoLimiteDelTechoNoSeHunde() {
        Cancha c = new Cancha(1000, 650);
        Balon b = new Balon(500, 18, RADIO);
        b.aplicarFuerza(0, -8);
        b.mover();
        b.rebotar(c);
        assertTrue(b.getPosY() >= b.getRadio(), "Caso limite: el balon queda dentro de la cancha");
    }

    // Suelo

    @Test
    public void balonNoAtraviesaElSuelo() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            Balon b = new Balon(t[0] / 2.0, c.getSuelo() - 5, RADIO);
            b.aplicarFuerza(0, 8);
            b.mover();
            b.rebotar(c);
            assertTrue(b.getPosY() + b.getRadio() <= c.getSuelo(), "El balon atraviesa el suelo (" + t[0] + ")");
        }
    }

    // Goles

    @Test
    public void detectaGolIzquierdo() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            assertSame(c.getArcoIzquierdo(), c.esGol(balonEnGolIzquierdo(c)), "No detecta gol izquierdo en " + t[0]);
        }
    }

    @Test
    public void detectaGolDerecho() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            assertSame(c.getArcoDerecho(), c.esGol(balonEnGolDerecho(c)), "No detecta gol derecho en " + t[0]);
        }
    }

    @Test
    public void balonEnElCentroNoEsGol() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            Balon b = new Balon(t[0] / 2.0, t[1] / 2.0, RADIO);
            assertNull(c.esGol(b), "El centro de la cancha no es gol (" + t[0] + ")");
        }
    }

    @Test
    public void balonSinLlegarAlArcoNoEsGol() {
        for (int[] t : TAMANOS) {
            Cancha c = new Cancha(t[0], t[1]);
            Arco izq = c.getArcoIzquierdo();
            Arco der = c.getArcoDerecho();
            Balon lejosIzq = new Balon(izq.getPosX() + izq.getAncho() + 150, centroY(izq), RADIO);
            Balon lejosDer = new Balon(der.getPosX() - 150, centroY(der), RADIO);
            assertNull(c.esGol(lejosIzq), "Lejos del arco izquierdo no es gol (" + t[0] + ")");
            assertNull(c.esGol(lejosDer), "Lejos del arco derecho no es gol (" + t[0] + ")");
        }
    }

    // Poderes

    @Test
    public void nActivaPoderDelJugador2() {
        j2.cargarPoder(100);
        j2.realizarAccion("N", partido);
        assertTrue(j2.tienePoderPreparado(), "N del jugador 2 no activa el poder");
    }

    @Test
    public void qActivaPoderDelJugador1() {
        j1.cargarPoder(100);
        j1.realizarAccion("Q", partido);
        assertTrue(j1.tienePoderPreparado(), "Q del jugador 1 no activa el poder");
    }

    @Test
    public void poderNoSeActivaSinCarga() {
        j1.realizarAccion("Q", partido);
        j2.realizarAccion("N", partido);
        assertFalse(j1.tienePoderPreparado(), "Jugador 1 activa poder sin carga");
        assertFalse(j2.tienePoderPreparado(), "Jugador 2 activa poder sin carga");
    }

    @Test
    public void teclaDelOtroJugadorNoActivaElPoder() {
        j1.cargarPoder(100);
        j2.cargarPoder(100);
        j1.realizarAccion("N", partido);
        j2.realizarAccion("Q", partido);
        assertFalse(j1.tienePoderPreparado(), "N no debe activar el poder del jugador 1");
        assertFalse(j2.tienePoderPreparado(), "Q no debe activar el poder del jugador 2");
    }

    @Test
    public void poderNoEstaPreparadoAlInicio() {
        assertFalse(j1.tienePoderPreparado(), "Jugador 1 no debe iniciar con poder");
        assertFalse(j2.tienePoderPreparado(), "Jugador 2 no debe iniciar con poder");
    }
}