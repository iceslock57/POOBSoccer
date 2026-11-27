package tests;

import Domain.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static tests.EscenarioPoderes.*;

/** Pruebas de cómo viaja cada tiro de poder y de cuánto dura su efecto. */
public class TirosDePoderTest {

    private static final double DELTA = 1e-6;
    private static final double MULT = 1.2;   // multiplicador de kick con nivel 3

    // Duraciones y recorridos del anexo

    @Test
    public void cadaPoderDuraLoQueDiceElAnexo() {
        assertEquals(1.5, new PoderAuraAzul().duracionEfecto(), DELTA, "Aura azul");
        assertEquals(3, new PoderRayo().duracionEfecto(), DELTA, "Rayo");
        assertEquals(3, new PoderTornado().duracionEfecto(), DELTA, "Tornado");
        assertEquals(2.5, new PoderIlusion().duracionEfecto(), DELTA, "Ilusión");
    }

    // Poderes propios de Rusia y Japón

    @Test
    public void elHieloVuelaEnArcoYLuegoPatinaSinRebotar() {
        Partido partido = partido("Rusia", "Camerún");
        Personaje rival = partido.getJugador2().getPersonaje();
        patearConPoderPreparado(partido);
        Balon balon = partido.getBalon();
        double salida = balon.getPosY(), masAlto = salida;
        int suelo = partido.getCancha().getSuelo();
        int cuadros = 0;
        while (balon.getPosY() + balon.getRadio() < suelo && cuadros++ < 100) {
            rival.resetearPosicion(900, 0);
            partido.actualizarEstado();
            masAlto = Math.min(masAlto, balon.getPosY());
        }
        assertTrue(masAlto < salida - 60, "Primero sube");
        assertTrue(balon.isDisparoEspecial(), "Sigue llevando el poder al caer");
        for (int i = 0; i < 4; i++) {
            rival.resetearPosicion(900, 0);
            partido.actualizarEstado();
            assertEquals(suelo - balon.getRadio(), balon.getPosY(), DELTA, "Patina pegado al suelo");
            assertEquals(17, balon.getVelX(), DELTA, "Y va más rápido que en el aire");
        }
    }

    @Test
    public void elHieloDelJugadorDosPatinaHaciaLaIzquierda() {
        Balon balon = new Balon(500, 534, 16);
        new PoderHielo().lanzar(balon, -1, 2);
        assertTrue(balon.getVelX() < 0 && balon.getVelY() < 0, "Sale hacia arriba y a la izquierda");
        new PoderHielo().avanzarDisparo(balon, 550);
        assertEquals(-17, balon.getVelX(), DELTA, "Conserva su dirección en el suelo");
    }

    @Test
    public void elFuegoAvanzaSubiendoYBajando() {
        Balon balon = new Balon(300, 300, 16);
        PoderFuego fuego = new PoderFuego();
        fuego.lanzar(balon, 1, 1);
        assertTrue(balon.getVelY() < 0, "Siempre empieza subiendo");
        int cambios = 0;
        for (int i = 0; i < 50; i++) {
            double antes = balon.getVelY();
            balon.mover();
            fuego.avanzarDisparo(balon, 550);
            assertEquals(15, balon.getVelX(), DELTA, "Avanza siempre igual");
            if (antes * balon.getVelY() < 0) cambios++;
        }
        assertTrue(cambios >= 3, "Cambia de sentido varias veces: " + cambios);
    }

    @Test
    public void laQuemaduraQuitaVidaCadaSegundoYDejaMoverse() {
        Personaje rival = rivalEnReposo();
        new PoderFuego().aplicarImpacto(rival, 1.0, 1);
        assertEquals(VIDA_MAX - 25, rival.getVidaActual(), "Daño del golpe");
        double x = rival.getPosX();
        rival.mover(1, null);
        assertTrue(rival.getPosX() > x, "Puede caminar mientras se quema");
        avanzar(rival, FPS - 1);
        assertEquals(VIDA_MAX - 25, rival.getVidaActual(), "Todavía no pasa un segundo");
        avanzar(rival, 1);
        assertEquals(VIDA_MAX - 33, rival.getVidaActual(), "Al primer segundo");
        avanzar(rival, 2 * FPS - 1);
        assertTrue(rival.isQuemado(), "Un cuadro antes de los tres segundos sigue en llamas");
        avanzar(rival, 1);
        assertFalse(rival.isQuemado(), "A los tres segundos se apaga");
        assertEquals(VIDA_MAX - 49, rival.getVidaActual(), "Tres golpes de 8");
        avanzar(rival, 2 * FPS);
        assertEquals(VIDA_MAX - 49, rival.getVidaActual(), "No sigue perdiendo vida");
    }

    @Test
    public void elDanioDeLaQuemaduraTambienEscalaConElKick() {
        Personaje rival = rivalEnReposo();
        new PoderFuego().aplicarImpacto(rival, 1.5, 1);
        avanzar(rival, 3 * FPS);
        assertEquals(VIDA_MAX - 38 - 3 * 12, rival.getVidaActual(), "Golpe de 37,5 y tres quemaduras de 12");
    }

    @Test
    public void otroEfectoApagaElFuego() {
        Personaje rival = rivalEnReposo();
        new PoderFuego().aplicarEfecto(rival);
        rival.congelar(1);
        assertFalse(rival.isQuemado(), "El hielo reemplaza la quemadura");
        int vida = rival.getVidaActual();
        avanzar(rival, 4 * FPS);
        assertEquals(vida, rival.getVidaActual(), "Ya no pierde vida");
    }

    @Test
    public void laQuemaduraNoAlargaElAturdimiento() {
        Personaje rival = rivalEnReposo();
        new PoderFuego().aplicarEfecto(rival);
        rival.recibirDanio(VIDA_MAX);
        assertTrue(rival.aturdido(), "Precondición: se quedó sin vida");
        avanzar(rival, 3 * FPS);
        assertFalse(rival.aturdido(), "El aturdimiento dura los mismos tres segundos");
        assertEquals(VIDA_MAX, rival.getVidaActual(), "Recupera toda la vida");
    }

    @Test
    public void elEmpujeDuraSegundoYMedioYNoDejaCaminar() {
        Personaje rival = rivalEnReposo();
        new PoderAuraAzul().aplicarImpacto(rival, 1.0, 1);
        avanzar(rival, 89);
        assertTrue(rival.isEmpujado(), "Un cuadro antes del segundo y medio sigue empujado");
        double x = rival.getPosX();
        rival.mover(-1, null);
        assertEquals(x, rival.getPosX(), DELTA, "No puede caminar en contra del empuje");
        avanzar(rival, 1);
        assertFalse(rival.isEmpujado(), "Al segundo y medio recupera el control");
        assertTrue(rival.getPosX() > 900, "Quedó junto a su propio arco");
        double quieto = rival.getPosX();
        avanzar(rival, 5);
        assertEquals(quieto, rival.getPosX(), DELTA, "No se sigue deslizando");
    }

    @Test
    public void laConfusionDuraDosSegundosYMedio() {
        Personaje rival = rivalEnReposo();
        new PoderIlusion().aplicarImpacto(rival, MULT, 1);
        assertEquals(VIDA_MAX - 36, rival.getVidaActual(), "Daño de 30 x 1.2");
        avanzar(rival, 149);
        assertTrue(rival.isConfundido(), "Un cuadro antes sigue confundido");
        avanzar(rival, 1);
        assertFalse(rival.isConfundido(), "A los 2,5 segundos termina");
    }

    @Test
    public void inconscienteNoSeMueveNiSaltaNiPatea() {
        Personaje rival = rivalEnReposo();
        new PoderTornado().aplicarImpacto(rival, 1.0, 1);
        avanzar(rival, 120);
        double x = rival.getPosX(), y = rival.getPosY();
        rival.mover(1, null);
        rival.saltar();
        rival.patear();
        avanzar(rival, 1);
        assertEquals(x, rival.getPosX(), DELTA, "No camina");
        assertEquals(y, rival.getPosY(), DELTA, "No salta");
        assertFalse(rival.isPateando(), "No patea");
    }

    @Test
    public void losTirosRectosNoCambianDeAltura() {
        for (String pais : new String[]{"Corea del Sur", "Camerún"}) {
            Partido partido = partido(pais, "Camerún");
            patearConPoderPreparado(partido);
            double y = partido.getBalon().getPosY();
            for (int i = 0; i < 6; i++) partido.actualizarEstado();
            assertEquals(y, partido.getBalon().getPosY(), DELTA, pais + ": recto y horizontal");
        }
    }

    @Test
    public void elTornadoAvanzaPegadoAlSueloRebotando() {
        Partido partido = partido("Nigeria", "Camerún");
        partido.getJugador2().getPersonaje().resetearPosicion(900, 100);
        patearConPoderPreparado(partido);
        Balon balon = partido.getBalon();
        for (int i = 0; i < 10; i++) partido.actualizarEstado();
        int rebotes = 0;
        for (int i = 0; i < 20; i++) {
            double antes = balon.getVelY();
            partido.getJugador2().getPersonaje().resetearPosicion(900, 100);
            partido.actualizarEstado();
            assertTrue(balon.getPosY() > partido.getCancha().getSuelo() - 45, "No se levanta del suelo");
            if (antes > 0 && balon.getVelY() < 0) rebotes++;
        }
        assertTrue(rebotes >= 2, "Rebota varias veces en poco tiempo: " + rebotes);
    }

    // Balones de la ilusión

    @Test
    public void laIlusionDejaOchoBalonesEnLaCancha() {
        Partido partido = partido("Estados Unidos", "Camerún");
        patearConPoderPreparado(partido);
        List<Balon> falsos = partido.getBalonesFalsos();
        assertEquals(7, falsos.size(), "Siete falsos acompañan al real");
        for (Balon falso : falsos) {
            assertEquals(partido.getBalon().getRadio(), falso.getRadio(), "Mismo tamaño");
            assertEquals(partido.getBalon().getVelX(), falso.getVelX(), DELTA, "Misma velocidad de avance");
        }
    }

    @Test
    public void elBalonRealDeLaIlusionNoSaleSiempreIgual() {
        double primera = salidaVerticalDelReal(1);
        boolean cambio = false;
        for (int semilla = 2; semilla < 20 && !cambio; semilla++) cambio = salidaVerticalDelReal(semilla) != primera;
        assertTrue(cambio, "La posición del real se reparte al azar");
    }

    private static double salidaVerticalDelReal(int semilla) {
        Balon balon = new Balon(300, 300, 16);
        new PoderIlusion(new Random(semilla)).lanzar(balon, 1, 1);
        return balon.getVelY();
    }

    @Test
    public void losBalonesFalsosNoHacenGolNiDanio() {
        Partido partido = partido("Estados Unidos", "Camerún");
        patearConPoderPreparado(partido);
        Personaje rival = partido.getJugador2().getPersonaje();
        for (int i = 0; i < 148; i++) {
            // El real se mantiene lejos para que solo los falsos puedan tocar algo.
            partido.getBalon().resetearPosicion(500, 60);
            partido.actualizarEstado();
        }
        assertEquals(0, partido.getMarcador()[0] + partido.getMarcador()[1], "Ningún falso cuenta como gol");
        assertEquals(VIDA_MAX, rival.getVidaActual(), "Ningún falso hace daño");
        assertFalse(rival.isConfundido(), "Ningún falso aplica el efecto");
    }

    @Test
    public void losBalonesFalsosDesaparecenAlTerminarElEfecto() {
        Partido partido = partido("Estados Unidos", "Camerún");
        patearConPoderPreparado(partido);
        Personaje rival = partido.getJugador2().getPersonaje();
        lanzarDisparoSobreElRival(partido, 1);
        assertTrue(rival.isConfundido(), "Precondición: el real golpeó al rival");
        for (int i = 0; i < 149; i++) {
            partido.getBalon().resetearPosicion(500, 60);
            partido.actualizarEstado();
        }
        assertEquals(7, partido.getBalonesFalsos().size(), "Siguen mientras dura la confusión");
        partido.getBalon().resetearPosicion(500, 60);
        partido.actualizarEstado();
        assertFalse(rival.isConfundido(), "Terminó la confusión");
        assertTrue(partido.getBalonesFalsos().isEmpty(), "Y con ella los balones falsos");
    }

    @Test
    public void sinGolpearAlRivalLosFalsosTambienDesaparecen() {
        Partido partido = partido("Estados Unidos", "Camerún");
        patearConPoderPreparado(partido);
        for (int i = 0; i < 150; i++) {
            partido.getBalon().resetearPosicion(500, 60);
            partido.actualizarEstado();
        }
        assertTrue(partido.getBalonesFalsos().isEmpty(), "Duran dos segundos y medio");
    }

    @Test
    public void unTiroDePoderQueEntraAlArcoEsGolSinDanio() {
        Partido partido = partido("Camerún", "Camerún");
        Arco arco = partido.getCancha().getArcoDerecho();
        partido.getBalon().resetearPosicion(arco.getPosX() - 20, arco.getPosY() + arco.getAlto() / 2.0);
        partido.getBalon().aplicarDisparoEspecial(12, 0, 1);
        partido.actualizarEstado();
        assertEquals(1, partido.getMarcador()[0], "El gol cuenta");
        Personaje rival = partido.getJugador2().getPersonaje();
        assertEquals(VIDA_MAX, rival.getVidaActual(), "Sin daño");
        assertFalse(rival.isElectrificado(), "Sin efecto");
    }

    @Test
    public void alChocarConUnLadoElTiroPierdeSuPoder() {
        Partido partido = partido("Corea del Sur", "Camerún");
        partido.getBalon().resetearPosicion(970, 60);
        partido.getBalon().aplicarDisparoEspecial(24, 0, 1);
        partido.actualizarEstado();
        assertFalse(partido.getBalon().isDisparoEspecial(), "Vuelve a ser un balón normal");
    }
}