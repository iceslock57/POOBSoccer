package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de PartidoPorGoles y de la logica heredada de Partido que ejecuta
 * (goles, reinicio, contacto, disparo especial, patada y cierre del partido).
 * Limite: 20 pruebas, cada una agrupa varios escenarios relacionados.
 */
public class PartidoPorGolesTest {

    private static final int[][] TAMANOS = {{800, 550}, {1000, 650}, {1200, 750}};
    private static final int[] METAS = {1, 3, 5, 7};
    private static final int META = 3;
    private static final double DELTA = 1e-6;

    private Jugador j1;
    private Jugador j2;
    private PartidoPorGoles partido;

    @BeforeEach
    public void preparar() {
        j1 = crearJugador1();
        j2 = crearJugador2();
        partido = new PartidoPorGoles(j1, j2, META);
    }

    // Apoyo

    private static Jugador crearJugador1() {
        return new JugadorHumano("Corea",
                new Personaje("Corea", "Corea del Sur", 3, 3, 3, new PoderAuraAzul()),
                new String[]{"A", "D", "W", "S", "Q"});
    }

    private static Jugador crearJugador2() {
        return new JugadorHumano("Camerun",
                new Personaje("Camerun", "Camerún", 3, 3, 3, new PoderRayo()),
                new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
    }

    private static PartidoPorGoles crear(int meta, int[] t) {
        return new PartidoPorGoles(crearJugador1(), crearJugador2(), meta, t[0], t[1]);
    }

    private static void avanzar(Partido p, int frames) {
        for (int i = 0; i < frames; i++) p.actualizarEstado();
    }

    private static int golesTotales(Partido p) {
        return p.getMarcador()[0] + p.getMarcador()[1];
    }

    private static void colocarBalonEnGol(Partido p, boolean arcoIzquierdo) {
        Cancha c = p.getCancha();
        Arco arco = arcoIzquierdo ? c.getArcoIzquierdo() : c.getArcoDerecho();
        double y = arco.getPosY() + arco.getAlto() / 2.0;
        double x = arcoIzquierdo ? arco.getPosX() + arco.getAncho() + 8 : arco.getPosX() - 8;
        p.getBalon().resetearPosicion(x, y);
        p.getBalon().aplicarFuerza(arcoIzquierdo ? -12 : 12, 0);
    }

    /** Fuerza un gol y exige que se registre exactamente uno. */
    private static void anotar(Partido p, boolean arcoIzquierdo) {
        int antes = golesTotales(p);
        colocarBalonEnGol(p, arcoIzquierdo);
        for (int i = 0; i < 10 && golesTotales(p) == antes; i++) p.actualizarEstado();
        assertEquals(antes + 1, golesTotales(p), "El gol forzado no fue detectado");
    }

    private static void ubicar(Jugador j, double x, double y) {
        j.getPersonaje().resetearPosicion(x, y);
    }

    private static void colocarBalonSobre(Partido p, Personaje objetivo) {
        p.getBalon().resetearPosicion(objetivo.getPosX() + 20, objetivo.getPosY() + 20);
    }

    private static void verificarPosicionesIniciales(Partido p, int ancho, int alto, String ctx) {
        int suelo = p.getCancha().getSuelo();
        Personaje a = p.getJugador1().getPersonaje();
        Personaje b = p.getJugador2().getPersonaje();
        assertEquals(ancho * .22, a.getPosX(), DELTA, "X jugador 1 " + ctx);
        assertEquals(suelo - Personaje.ALTO, a.getPosY(), DELTA, "Y jugador 1 " + ctx);
        assertEquals(ancho * .74, b.getPosX(), DELTA, "X jugador 2 " + ctx);
        assertEquals(suelo - Personaje.ALTO, b.getPosY(), DELTA, "Y jugador 2 " + ctx);
        assertEquals(ancho / 2.0, p.getBalon().getPosX(), DELTA, "X balon " + ctx);
        assertEquals(alto * .32, p.getBalon().getPosY(), DELTA, "Y balon " + ctx);
    }

    // Construccion

    @Test
    public void constructoresInicializanEstadoYPosiciones() {
        assertEquals(META, partido.getGolesObjetivo(), "Meta");
        assertEquals(1000, partido.getCancha().getAncho(), "Ancho por defecto");
        assertEquals(650, partido.getCancha().getAlto(), "Alto por defecto");
        assertArrayEquals(new int[]{0, 0}, partido.getMarcador(), "Marcador inicial");
        assertFalse(partido.isTerminado(), "No inicia terminado");
        assertFalse(partido.esFinDePartido(), "esFinDePartido inicial");
        assertNull(partido.getGanador(), "Sin ganador al inicio");
        assertSame(j1, partido.getJugador1(), "Jugador 1");
        assertSame(j2, partido.getJugador2(), "Jugador 2");
        assertNotNull(partido.getBalon(), "Balon");
        verificarPosicionesIniciales(partido, 1000, 650, "(defecto)");
        for (int[] t : TAMANOS) {
            PartidoPorGoles p = crear(5, t);
            assertEquals(5, p.getGolesObjetivo(), "Meta (" + t[0] + ")");
            assertEquals(t[0], p.getCancha().getAncho(), "Ancho " + t[0]);
            assertEquals(t[1], p.getCancha().getAlto(), "Alto " + t[1]);
            assertFalse(p.isTerminado(), "No inicia terminado (" + t[0] + ")");
            verificarPosicionesIniciales(p, t[0], t[1], "(" + t[0] + ")");
        }
    }

    @Test
    public void constructoresValidanLaMeta() {
        for (int meta : METAS) {
            assertEquals(meta, new PartidoPorGoles(crearJugador1(), crearJugador2(), meta).getGolesObjetivo(), "Meta " + meta);
            assertEquals(meta, new PartidoPorGoles(crearJugador1(), crearJugador2(), meta, 800, 550).getGolesObjetivo(), "Meta 5 args " + meta);
        }
        for (int meta : new int[]{0, -1, Integer.MIN_VALUE}) {
            final int m = meta;
            assertThrows(IllegalArgumentException.class,
                    () -> new PartidoPorGoles(crearJugador1(), crearJugador2(), m), "3 args, meta=" + m);
            assertThrows(IllegalArgumentException.class,
                    () -> new PartidoPorGoles(crearJugador1(), crearJugador2(), m, 800, 550), "5 args, meta=" + m);
        }
    }

    @Test
    public void constructoresRechazanJugadoresNulos() {
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(null, crearJugador2(), 3), "j1 nulo");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(crearJugador1(), null, 3), "j2 nulo");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(null, null, 3), "ambos nulos");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(null, crearJugador2(), 3, 800, 550), "j1 nulo (5 args)");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(crearJugador1(), null, 3, 800, 550), "j2 nulo (5 args)");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(null, null, 0), "nulos y meta invalida (3 args)");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorGoles(null, null, 0, 800, 550), "nulos y meta invalida (5 args)");
    }

    // esFinDePartido

    @Test
    public void esFinDePartidoRespetaLosValoresLimite() {
        assertFalse(partido.esFinDePartido(), "0-0");
        for (int meta : METAS) {
            PartidoPorGoles p = new PartidoPorGoles(crearJugador1(), crearJugador2(), meta);
            int[] m = p.getMarcador();
            m[0] = meta - 1; m[1] = 0;
            assertFalse(p.esFinDePartido(), "J1 con meta-1 (meta " + meta + ")");
            m[0] = 0; m[1] = meta - 1;
            assertFalse(p.esFinDePartido(), "J2 con meta-1 (meta " + meta + ")");
            m[0] = meta - 1;
            assertFalse(p.esFinDePartido(), "Ambos con meta-1 (meta " + meta + ")");
            m[0] = meta; m[1] = 0;
            assertTrue(p.esFinDePartido(), "J1 en la meta (meta " + meta + ")");
            m[0] = 0; m[1] = meta;
            assertTrue(p.esFinDePartido(), "J2 en la meta (meta " + meta + ")");
            m[0] = meta; m[1] = meta;
            assertTrue(p.esFinDePartido(), "Ambos en la meta (meta " + meta + ")");
            m[0] = meta + 2; m[1] = 0;
            assertTrue(p.esFinDePartido(), "J1 supera la meta (meta " + meta + ")");
            m[0] = 0; m[1] = meta + 2;
            assertTrue(p.esFinDePartido(), "J2 supera la meta (meta " + meta + ")");
            assertFalse(p.isTerminado(), "Consultar no debe terminar el partido (meta " + meta + ")");
        }
    }

    // Goles

    @Test
    public void golEnArcoIzquierdoSumaAlJugador2YReiniciaPosiciones() {
        for (int[] t : TAMANOS) {
            PartidoPorGoles p = crear(5, t);
            ubicar(p.getJugador1(), 10, 10);
            ubicar(p.getJugador2(), 20, 20);
            anotar(p, true);
            assertArrayEquals(new int[]{0, 1}, p.getMarcador(), "Marcador (" + t[0] + ")");
            verificarPosicionesIniciales(p, t[0], t[1], "tras gol (" + t[0] + ")");
        }
    }

    @Test
    public void golEnArcoDerechoSumaAlJugador1YReiniciaPosiciones() {
        for (int[] t : TAMANOS) {
            PartidoPorGoles p = crear(5, t);
            ubicar(p.getJugador1(), 10, 10);
            ubicar(p.getJugador2(), 20, 20);
            anotar(p, false);
            assertArrayEquals(new int[]{1, 0}, p.getMarcador(), "Marcador (" + t[0] + ")");
            verificarPosicionesIniciales(p, t[0], t[1], "tras gol (" + t[0] + ")");
        }
    }

    @Test
    public void marcadorAcumulaGolesDeAmbosLadosSinTerminar() {
        PartidoPorGoles p = new PartidoPorGoles(j1, j2, 7);
        anotar(p, true);
        anotar(p, true);
        anotar(p, false);
        assertArrayEquals(new int[]{1, 2}, p.getMarcador(), "Marcador acumulado");
        assertFalse(p.isTerminado(), "Sigue en juego");
        assertNull(p.getGanador(), "Sin ganador mientras corre");
    }

    @Test
    public void sinGolElBalonLibreNoCambiaElMarcador() {
        avanzar(partido, 60);
        assertArrayEquals(new int[]{0, 0}, partido.getMarcador(), "Sin goles con el balon libre");
        assertFalse(partido.isTerminado(), "Sigue en juego");
        partido.getMarcador()[0] = META - 1;
        assertNull(partido.getGanador(), "Con ventaja pero sin terminar no hay ganador");
    }

    @Test
    public void golesRecibidosCarganElPoderDelJugador() {
        PartidoPorGoles p = new PartidoPorGoles(j1, j2, 7);
        for (int i = 0; i < 3; i++) anotar(p, true);
        j1.realizarAccion("Q", p);
        assertFalse(j1.tienePoderPreparado(), "3 goles recibidos (75) no bastan");
        anotar(p, true);
        j1.realizarAccion("Q", p);
        assertTrue(j1.tienePoderPreparado(), "4 goles recibidos (100) activan el poder del jugador 1");
        for (int i = 0; i < 4; i++) anotar(p, false);
        j2.realizarAccion("N", p);
        assertTrue(j2.tienePoderPreparado(), "4 goles recibidos activan el poder del jugador 2");
        assertFalse(p.isTerminado(), "4-4 con meta 7 sigue");
    }

    // Fin y ganador

    @Test
    public void jugador1GanaAlAlcanzarLaMeta() {
        for (int i = 0; i < META - 1; i++) {
            anotar(partido, false);
            assertFalse(partido.isTerminado(), "No termina con " + (i + 1) + " goles");
            assertNull(partido.getGanador(), "Sin ganador con " + (i + 1) + " goles");
        }
        anotar(partido, false);
        assertTrue(partido.isTerminado(), "Termina en la meta");
        assertTrue(partido.esFinDePartido(), "esFinDePartido");
        assertSame(j1, partido.getGanador(), "Gana jugador 1");
        assertArrayEquals(new int[]{META, 0}, partido.getMarcador(), "Marcador final");
    }

    @Test
    public void jugador2GanaRemontandoYGanaElPrimeroEnLlegar() {
        anotar(partido, false);
        anotar(partido, true);
        anotar(partido, false);
        anotar(partido, true);
        assertFalse(partido.isTerminado(), "2-2 con meta 3 sigue");
        anotar(partido, true);
        assertTrue(partido.isTerminado(), "3 para el jugador 2 termina");
        assertSame(j2, partido.getGanador(), "Gana jugador 2");
        assertArrayEquals(new int[]{2, 3}, partido.getMarcador(), "Marcador final");
    }

    @Test
    public void metaUnoYReferenciaGenericaTerminanConUnGol() {
        Partido generico = new PartidoPorGoles(crearJugador1(), crearJugador2(), 1);
        assertFalse(generico.esFinDePartido(), "Inicio");
        anotar(generico, true);
        assertTrue(generico.isTerminado(), "Termina con 1 gol");
        assertTrue(generico.esFinDePartido(), "Fin por goles via referencia a Partido");
        assertSame(generico.getJugador2(), generico.getGanador(), "Gana jugador 2");
    }

    // Cierre y estado congelado

    @Test
    public void actualizarEstadoDetectaMetaPreCargada() {
        partido.getMarcador()[0] = META;
        partido.actualizarEstado();
        assertTrue(partido.isTerminado(), "J1 termina en el siguiente frame");
        assertSame(j1, partido.getGanador(), "Gana jugador 1");

        PartidoPorGoles p2 = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        p2.getMarcador()[1] = META;
        p2.actualizarEstado();
        assertTrue(p2.isTerminado(), "J2 termina en el siguiente frame");
        assertSame(p2.getJugador2(), p2.getGanador(), "Gana jugador 2");

        PartidoPorGoles p3 = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        p3.getMarcador()[0] = META;
        p3.getMarcador()[1] = META;
        p3.actualizarEstado();
        assertTrue(p3.isTerminado(), "Ambos en la meta termina");
        assertNull(p3.getGanador(), "Empate sin ganador");
    }

    @Test
    public void trasTerminarElPartidoQuedaCongelado() {
        for (int i = 0; i < META; i++) anotar(partido, false);
        assertTrue(partido.isTerminado(), "Precondicion: terminado");
        double x = partido.getBalon().getPosX();
        double y = partido.getBalon().getPosY();
        avanzar(partido, 30);
        assertEquals(x, partido.getBalon().getPosX(), DELTA, "X del balon");
        assertEquals(y, partido.getBalon().getPosY(), DELTA, "Y del balon");
        int[] antes = partido.getMarcador().clone();
        colocarBalonEnGol(partido, true);
        avanzar(partido, 20);
        assertArrayEquals(antes, partido.getMarcador(), "El marcador no cambia");
        assertSame(j1, partido.getGanador(), "El ganador no cambia");
    }

    @Test
    public void terminarManualEsCoherenteEIdempotente() {
        partido.terminar();
        assertTrue(partido.isTerminado(), "terminar() finaliza");
        assertNull(partido.getGanador(), "Empate sin ganador");

        PartidoPorGoles p = new PartidoPorGoles(crearJugador1(), crearJugador2(), 5);
        anotar(p, true);
        p.terminar();
        p.terminar();
        assertTrue(p.isTerminado(), "Sigue terminado");
        assertSame(p.getJugador2(), p.getGanador(), "Gana quien lidera, estable");
        assertArrayEquals(new int[]{0, 1}, p.getMarcador(), "Marcador estable");

        PartidoPorGoles q = new PartidoPorGoles(crearJugador1(), crearJugador2(), 1);
        anotar(q, false);
        q.terminar();
        assertSame(q.getJugador1(), q.getGanador(), "terminar() tras fin natural no altera el ganador");
    }

    // Patada y contacto

    @Test
    public void patadaConRivalAlFrenteEnAlcanceNoFalla() {
        double y = partido.getCancha().getSuelo() - Personaje.ALTO;
        ubicar(j1, 300, y);
        ubicar(j2, 350, y);
        assertDoesNotThrow(() -> partido.ejecutarPatada(j1), "J1 con rival al frente");
        ubicar(j1, 250, y);
        ubicar(j2, 300, y);
        assertDoesNotThrow(() -> partido.ejecutarPatada(j2), "J2 con rival al frente");
    }

    @Test
    public void patadaNulaOFueraDeAlcanceNoFalla() {
        double y = partido.getCancha().getSuelo() - Personaje.ALTO;
        assertDoesNotThrow(() -> partido.ejecutarPatada(null), "Atacante nulo");
        ubicar(j1, 300, y);
        ubicar(j2, 250, y);
        assertDoesNotThrow(() -> partido.ejecutarPatada(j1), "Rival a la espalda");
        ubicar(j2, 700, y);
        assertDoesNotThrow(() -> partido.ejecutarPatada(j1), "Rival lejos");
        ubicar(j2, 350, y - 200);
        assertDoesNotThrow(() -> partido.ejecutarPatada(j1), "Rival a otra altura");
        ubicar(j2, 350, y);
        j2.getPersonaje().aturdir(3);
        assertDoesNotThrow(() -> partido.ejecutarPatada(j1), "Rival aturdido");
        assertFalse(partido.isTerminado(), "La patada no termina el partido");
    }

    @Test
    public void contactoSinPatearSaleHaciaElLadoQueToca() {
        Personaje uno = j1.getPersonaje();
        partido.getBalon().resetearPosicion(uno.getPosX() + 40, uno.getPosY() + 40);
        partido.actualizarEstado();
        assertTrue(partido.getBalon().getVelX() > 0, "Delante de J1: sale hacia la derecha");
        assertFalse(partido.getBalon().isDisparoEspecial(), "No es disparo especial");

        PartidoPorGoles p = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        Personaje dos = p.getJugador2().getPersonaje();
        p.getBalon().resetearPosicion(dos.getPosX() + 12, dos.getPosY() + 40);
        p.actualizarEstado();
        assertTrue(p.getBalon().getVelX() < 0, "Delante de J2: sale hacia la izquierda");

        PartidoPorGoles atras = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        Personaje espalda = atras.getJugador1().getPersonaje();
        atras.getBalon().resetearPosicion(espalda.getPosX() + 8, espalda.getPosY() + 40);
        atras.actualizarEstado();
        assertTrue(atras.getBalon().getVelX() < 0, "Detrás de J1: sale hacia atrás, no hacia adelante");
        assertTrue(atras.getBalon().getPosX() < espalda.getPosX(), "Y queda fuera del cuerpo");

        PartidoPorGoles libre = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        double velX = libre.getBalon().getVelX();
        libre.actualizarEstado();
        assertEquals(velX, libre.getBalon().getVelX(), DELTA, "Sin contacto no hay rebote");
    }

    // Disparo especial

    @Test
    public void disparoEspecialDeCamerunElectrocutaAlRivalYSeConsume() {
        Personaje rival = j1.getPersonaje();
        colocarBalonSobre(partido, rival);
        partido.getBalon().aplicarDisparoEspecial(0, 0, 2);
        assertTrue(partido.getBalon().isDisparoEspecial(), "Precondicion: disparo activo");
        partido.actualizarEstado();
        assertTrue(rival.isElectrificado(), "El rayo de Camerun electrocuta al rival");
        assertFalse(rival.aturdido(), "Un tiro de poder no aturde por sí mismo");
        assertFalse(partido.getBalon().isDisparoEspecial(), "El disparo se consume al impactar");
    }

    @Test
    public void disparoEspecialDeCoreaSeConsumeYSoloImpactaAlRival() {
        colocarBalonSobre(partido, j2.getPersonaje());
        partido.getBalon().aplicarDisparoEspecial(0, 0, 1);
        assertDoesNotThrow(() -> partido.actualizarEstado(), "Impacto de Corea del Sur");
        assertFalse(partido.getBalon().isDisparoEspecial(), "Se consume al impactar");

        PartidoPorGoles propio = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        colocarBalonSobre(propio, propio.getJugador1().getPersonaje());
        propio.getBalon().aplicarDisparoEspecial(0, 0, 1);
        propio.actualizarEstado();
        assertTrue(propio.getBalon().isDisparoEspecial(), "No impacta a su propio lanzador");
        assertFalse(propio.getJugador1().getPersonaje().aturdido(), "El lanzador no queda aturdido");

        PartidoPorGoles vacio = new PartidoPorGoles(crearJugador1(), crearJugador2(), META);
        vacio.getBalon().aplicarDisparoEspecial(0, 0, 1);
        vacio.actualizarEstado();
        assertTrue(vacio.getBalon().isDisparoEspecial(), "Sin impacto el disparo sigue activo");
    }
}