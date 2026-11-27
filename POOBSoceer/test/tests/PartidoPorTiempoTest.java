package tests;

import Domain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de PartidoPorTiempo (cronometro, gol de oro, fin de partido). */
public class PartidoPorTiempoTest {

    private static final int FPS = 60;
    private static final int[][] TAMANOS = {{800, 550}, {1000, 650}, {1200, 750}};

    // Apoyo

    /** Construye objetos de prueba. */
    private static final class Fabrica {
        static Jugador jugador1() {
            return new JugadorHumano("Corea",
                    new Personaje("Corea", "Corea del Sur", 3, 3, 3, new PoderAuraAzul()),
                    new String[]{"A", "D", "W", "S", "Q"});
        }
        static Jugador jugador2() {
            return new JugadorHumano("Camerun",
                    new Personaje("Camerun", "Camerún", 3, 3, 3, new PoderRayo()),
                    new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
        }
        static PartidoPorTiempo porDefecto() { return new PartidoPorTiempo(jugador1(), jugador2()); }
        static PartidoPorTiempo conMinutos(int min) { return new PartidoPorTiempo(jugador1(), jugador2(), min); }
        static PartidoPorTiempo conCancha(int min, int ancho, int alto) {
            return new PartidoPorTiempo(jugador1(), jugador2(), min, ancho, alto);
        }
    }

    /** Avanza el partido y fuerza goles. */
    private static final class Simulador {
        static void avanzar(Partido p, int frames) {
            for (int i = 0; i < frames; i++) p.actualizarEstado();
        }
        static void avanzarSegundos(Partido p, int segundos) { avanzar(p, segundos * FPS); }
        static void golEnArcoIzquierdo(Partido p) { gol(p, true); }
        static void golEnArcoDerecho(Partido p) { gol(p, false); }

        private static void gol(Partido p, boolean izquierdo) {
            Cancha c = p.getCancha();
            Arco arco = izquierdo ? c.getArcoIzquierdo() : c.getArcoDerecho();
            double y = arco.getPosY() + arco.getAlto() / 2.0;
            double x = izquierdo ? arco.getPosX() + arco.getAncho() + 8 : arco.getPosX() - 8;
            Balon b = p.getBalon();
            b.resetearPosicion(x, y);
            b.aplicarFuerza(izquierdo ? -12 : 12, 0);
            p.actualizarEstado();
        }
        static PartidoPorTiempo enGolDeOro() {
            PartidoPorTiempo p = Fabrica.conMinutos(1);
            avanzarSegundos(p, 60);
            return p;
        }
    }

    private static void assertMarcador(Partido p, int g1, int g2, String msg) {
        int[] m = p.getMarcador();
        assertEquals(2, m.length, msg + ": longitud del marcador");
        assertEquals(g1, m[0], msg + ": goles jugador 1");
        assertEquals(g2, m[1], msg + ": goles jugador 2");
    }

    // Construccion

    @Test
    public void constructorPorDefecto() {
        PartidoPorTiempo p = Fabrica.porDefecto();
        assertEquals(120, p.getTiempoRestante(), "2 minutos por defecto");
        assertFalse(p.isEsGolDeOro(), "No inicia en gol de oro");
        assertFalse(p.isTerminado(), "No inicia terminado");
        assertFalse(p.esFinDePartido(), "esFinDePartido inicial");
        assertNull(p.getGanador(), "Sin ganador al inicio");
        assertMarcador(p, 0, 0, "Marcador inicial");
        assertEquals(1000, p.getCancha().getAncho(), "Ancho por defecto");
        assertEquals(650, p.getCancha().getAlto(), "Alto por defecto");
    }

    @Test
    public void constructorConMinutos() {
        for (int m : new int[]{1, 2, 5, 10})
            assertEquals(m * 60, Fabrica.conMinutos(m).getTiempoRestante(), "Segundos para " + m + " min");
    }

    @Test
    public void constructorConDimensiones() {
        for (int[] t : TAMANOS) {
            PartidoPorTiempo p = Fabrica.conCancha(3, t[0], t[1]);
            assertEquals(180, p.getTiempoRestante(), "Tiempo en " + t[0]);
            assertEquals(t[0], p.getCancha().getAncho(), "Ancho " + t[0]);
            assertEquals(t[1], p.getCancha().getAlto(), "Alto " + t[1]);
        }
    }

    @Test
    public void duracionInvalida() {
        for (int m : new int[]{0, -1, Integer.MIN_VALUE}) {
            final int minutos = m;
            assertThrows(IllegalArgumentException.class, () -> Fabrica.conMinutos(minutos), "3 args, min=" + m);
            assertThrows(IllegalArgumentException.class, () -> Fabrica.conCancha(minutos, 800, 550), "5 args, min=" + m);
        }
    }

    @Test
    public void jugadoresNulos() {
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorTiempo(null, Fabrica.jugador2()), "j1 nulo");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorTiempo(Fabrica.jugador1(), null), "j2 nulo");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorTiempo(null, null, 1), "ambos nulos (3 args)");
        assertThrows(IllegalArgumentException.class, () -> new PartidoPorTiempo(null, Fabrica.jugador2(), 1, 800, 550), "j1 nulo (5 args)");
    }

    // Cronometro

    @Test
    public void noDescuentaAntesDe60Frames() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.avanzar(p, 59);
        assertEquals(60, p.getTiempoRestante(), "A los 59 frames no baja");
        Simulador.avanzar(p, 1);
        assertEquals(59, p.getTiempoRestante(), "Al frame 60 baja 1 segundo");
    }

    @Test
    public void descuentaUnSegundoPorCada60Frames() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        for (int s = 1; s <= 5; s++) {
            Simulador.avanzarSegundos(p, 1);
            assertEquals(60 - s, p.getTiempoRestante(), "Tras " + s + " segundos");
        }
    }

    @Test
    public void framesParciales() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.avanzar(p, 130);
        assertEquals(58, p.getTiempoRestante(), "130 frames = 2 segundos completos");
    }

    // Empate y gol de oro

    @Test
    public void empateAlAgotarTiempoActivaGolDeOro() {
        PartidoPorTiempo p = Simulador.enGolDeOro();
        assertEquals(0, p.getTiempoRestante(), "Tiempo agotado");
        assertTrue(p.isEsGolDeOro(), "Debe activar gol de oro");
        assertFalse(p.isTerminado(), "No termina empatado");
        assertFalse(p.esFinDePartido(), "esFinDePartido en empate");
        assertNull(p.getGanador(), "Sin ganador en empate");
    }

    @Test
    public void noActivaGolDeOroUnFrameAntes() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.avanzar(p, 3599);
        assertEquals(1, p.getTiempoRestante(), "Queda 1 segundo");
        assertFalse(p.isEsGolDeOro(), "Aun no es gol de oro");
        assertFalse(p.isTerminado(), "Aun no termina");
    }

    @Test
    public void golDeOroCongelaElTiempo() {
        PartidoPorTiempo p = Simulador.enGolDeOro();
        Simulador.avanzar(p, 600);
        assertEquals(0, p.getTiempoRestante(), "El tiempo no pasa de 0");
        assertTrue(p.isEsGolDeOro(), "Sigue en gol de oro");
        assertFalse(p.isTerminado(), "Sigue sin terminar");
    }

    @Test
    public void empateConGolesEntraEnGolDeOro() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoIzquierdo(p);
        Simulador.golEnArcoDerecho(p);
        assertMarcador(p, 1, 1, "Marcador 1-1");
        Simulador.avanzar(p, 3598);
        assertEquals(0, p.getTiempoRestante(), "Tiempo agotado");
        assertTrue(p.isEsGolDeOro(), "1-1 debe ir a gol de oro");
        assertFalse(p.isTerminado(), "1-1 no termina");
    }

    @Test
    public void golDeOroTerminaConGolIzquierdo() {
        for (int[] t : TAMANOS) {
            PartidoPorTiempo p = Fabrica.conCancha(1, t[0], t[1]);
            Simulador.avanzarSegundos(p, 60);
            Simulador.golEnArcoIzquierdo(p);
            String ctx = " (" + t[0] + ")";
            assertMarcador(p, 0, 1, "Gol al arco izquierdo" + ctx);
            assertTrue(p.isTerminado(), "Gol de oro termina" + ctx);
            assertTrue(p.esFinDePartido(), "esFinDePartido" + ctx);
            assertSame(p.getJugador2(), p.getGanador(), "Gana jugador 2" + ctx);
            assertTrue(p.isEsGolDeOro(), "Conserva bandera de oro" + ctx);
        }
    }

    @Test
    public void golDeOroTerminaConGolDerecho() {
        for (int[] t : TAMANOS) {
            PartidoPorTiempo p = Fabrica.conCancha(1, t[0], t[1]);
            Simulador.avanzarSegundos(p, 60);
            Simulador.golEnArcoDerecho(p);
            String ctx = " (" + t[0] + ")";
            assertMarcador(p, 1, 0, "Gol al arco derecho" + ctx);
            assertTrue(p.isTerminado(), "Gol de oro termina" + ctx);
            assertSame(p.getJugador1(), p.getGanador(), "Gana jugador 1" + ctx);
        }
    }

    // Goles en tiempo regular

    @Test
    public void golEnTiempoRegularNoTermina() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoIzquierdo(p);
        assertMarcador(p, 0, 1, "Marcador 0-1");
        assertFalse(p.isTerminado(), "El gol no termina antes de tiempo");
        assertFalse(p.esFinDePartido(), "esFinDePartido con tiempo restante");
        assertFalse(p.isEsGolDeOro(), "No es gol de oro");
        assertNull(p.getGanador(), "Ganador null mientras corre");
    }

    @Test
    public void marcadorAcumulaGoles() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoIzquierdo(p);
        Simulador.golEnArcoIzquierdo(p);
        Simulador.golEnArcoDerecho(p);
        assertMarcador(p, 1, 2, "Marcador acumulado");
        assertFalse(p.isTerminado(), "Sigue en juego");
    }

    @Test
    public void ventajaJugador2AlAcabarElTiempo() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoIzquierdo(p);
        Simulador.avanzar(p, 3598);
        assertEquals(1, p.getTiempoRestante(), "Un frame antes del final");
        assertFalse(p.isTerminado(), "Aun no termina");
        Simulador.avanzar(p, 1);
        assertEquals(0, p.getTiempoRestante(), "Tiempo agotado");
        assertTrue(p.isTerminado(), "Termina exactamente al agotarse");
        assertFalse(p.isEsGolDeOro(), "Con ventaja no hay gol de oro");
        assertTrue(p.esFinDePartido(), "esFinDePartido");
        assertSame(p.getJugador2(), p.getGanador(), "Gana jugador 2");
    }

    @Test
    public void ventajaJugador1AlAcabarElTiempo() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoDerecho(p);
        Simulador.avanzar(p, 3599);
        assertTrue(p.isTerminado(), "Termina al agotarse el tiempo");
        assertSame(p.getJugador1(), p.getGanador(), "Gana jugador 1");
        assertMarcador(p, 1, 0, "Marcador final");
    }

    @Test
    public void golEnElUltimoFrameEvitaGolDeOro() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.avanzar(p, 3599);
        Simulador.golEnArcoIzquierdo(p);
        assertEquals(0, p.getTiempoRestante(), "Tiempo agotado en ese frame");
        assertFalse(p.isEsGolDeOro(), "El gol desempata antes de activar oro");
        assertTrue(p.isTerminado(), "Termina");
        assertSame(p.getJugador2(), p.getGanador(), "Gana jugador 2");
    }

    // Cierre del partido

    @Test
    public void estadoCongeladoTrasTerminar() {
        PartidoPorTiempo p = Simulador.enGolDeOro();
        Simulador.golEnArcoDerecho(p);
        assertTrue(p.isTerminado(), "Precondicion: terminado");
        int tiempo = p.getTiempoRestante();
        int[] marcador = p.getMarcador().clone();
        Simulador.avanzar(p, 200);
        Simulador.golEnArcoIzquierdo(p);
        assertEquals(tiempo, p.getTiempoRestante(), "El tiempo no cambia");
        assertMarcador(p, marcador[0], marcador[1], "El marcador no cambia");
        assertSame(p.getJugador1(), p.getGanador(), "El ganador no cambia");
    }

    @Test
    public void tiempoNuncaEsNegativo() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.avanzar(p, 3600 + 1200);
        assertEquals(0, p.getTiempoRestante(), "Tiempo se queda en 0");
        assertMarcador(p, 0, 0, "El marcador sigue teniendo dos posiciones");
    }

    @Test
    public void terminarManualConVentaja() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoDerecho(p);
        p.terminar();
        assertTrue(p.isTerminado(), "terminar() finaliza");
        assertSame(p.getJugador1(), p.getGanador(), "Gana quien lidera");
        int tiempo = p.getTiempoRestante();
        Simulador.avanzar(p, 120);
        assertEquals(tiempo, p.getTiempoRestante(), "Tras terminar no corre el reloj");
    }

    @Test
    public void terminarManualEmpatadoNoDaGanador() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        p.terminar();
        assertTrue(p.isTerminado(), "terminar() finaliza");
        assertNull(p.getGanador(), "Empate sin ganador");
    }

    @Test
    public void terminarEsIdempotente() {
        PartidoPorTiempo p = Fabrica.conMinutos(1);
        Simulador.golEnArcoIzquierdo(p);
        p.terminar();
        p.terminar();
        assertTrue(p.isTerminado(), "Sigue terminado");
        assertSame(p.getJugador2(), p.getGanador(), "Ganador estable");
        assertMarcador(p, 0, 1, "Marcador estable");
    }

    @Test
    public void terminarDuranteGolDeOro() {
        PartidoPorTiempo p = Simulador.enGolDeOro();
        p.terminar();
        assertTrue(p.isTerminado(), "Terminado manualmente");
        assertNull(p.getGanador(), "Empatado, sin ganador");
    }
}
