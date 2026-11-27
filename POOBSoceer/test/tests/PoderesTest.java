package tests;

import Domain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static tests.EscenarioPoderes.*;

/** Pruebas de los poderes especiales y de su efecto real en cada país durante el partido. */
public class PoderesTest {

    private static final double DELTA = 1e-6;
    private static final double MULT = 1.2;   // multiplicador de kick que da el catálogo (kick 3)
    private static final double EMPUJE = 14;  // velocidad horizontal que deja aplicarEmpuje

    private static int danio(double base, double mult) {
        return (int) Math.round(base * mult);
    }

    private static int danioBase(String pais) {
        switch (pais) {
            case "Corea del Sur": return 40;
            case "Camerún": return 60;
            case "Nigeria": return 40;
            case "Estados Unidos": return 30;
            case "Rusia": return 35;
            case "Japón": return 25;
            default: throw new AssertionError("País sin daño esperado definido: " + pais);
        }
    }

    private static double velocidadEsperada(String pais) {
        switch (pais) {
            case "Corea del Sur": return 24;
            case "Camerún": return 12;
            case "Nigeria": return 18;
            case "Estados Unidos": return 14;
            case "Rusia": return 11;
            case "Japón": return 15;
            default: throw new AssertionError("País sin velocidad esperada definida: " + pais);
        }
    }

    /** Comprueba daño, estados y empuje que debe producir el poder del país tras el impacto. */
    private static void verificarEfecto(String pais, Partido partido, Personaje rival, double xAntes, int dueno) {
        int direccion = dueno == 1 ? 1 : -1;
        Personaje atacante = (dueno == 1 ? partido.getJugador1() : partido.getJugador2()).getPersonaje();
        boolean aturdido = false, congelado = false, confundido = false;
        boolean electrificado = false, inconsciente = false, empuja = false, quemado = false;
        switch (pais) {
            case "Corea del Sur":
                empuja = true;
                break;
            case "Camerún":
                electrificado = true;
                break;
            case "Rusia":
                congelado = true;
                break;
            case "Nigeria":
                inconsciente = true;
                break;
            case "Japón":
                quemado = true;
                break;
            case "Estados Unidos":
                confundido = true;
                break;
            default:
                throw new AssertionError("País sin efecto esperado definido: " + pais);
        }
        // Un cuadro más para que el empuje, si existe, se refleje en la posición.
        partido.actualizarEstado();
        int vidaEsperada = VIDA_MAX - danio(danioBase(pais), atacante.getMultiplicadorKick());
        assertEquals(vidaEsperada, rival.getVidaActual(), pais + ": daño");
        assertEquals(aturdido, rival.aturdido(), pais + ": aturdimiento");
        assertEquals(congelado, rival.congelado(), pais + ": congelamiento");
        assertEquals(confundido, rival.isConfundido(), pais + ": confusión");
        assertEquals(electrificado, rival.isElectrificado(), pais + ": electrocución");
        assertEquals(inconsciente, rival.isInconsciente(), pais + ": inconsciencia");
        assertEquals(quemado, rival.isQuemado(), pais + ": quemadura");
        double xEsperada = empuja ? xAntes + direccion * EMPUJE : xAntes;
        assertEquals(xEsperada, rival.getPosX(), DELTA, pais + ": empuje");
    }

    // Poderes por separado

    @Test
    public void velocidadDeDisparoDeCadaPoder() {
        assertEquals(24, new PoderAuraAzul().velocidadDisparo(), DELTA, "Aura azul");
        assertEquals(12, new PoderRayo().velocidadDisparo(), DELTA, "Rayo");
        assertEquals(11, new PoderHielo().velocidadDisparo(), DELTA, "Hielo");
        assertEquals(15, new PoderFuego().velocidadDisparo(), DELTA, "Fuego");
        assertEquals(18, new PoderTornado().velocidadDisparo(), DELTA, "Tornado");
        assertEquals(14, new PoderIlusion().velocidadDisparo(), DELTA, "Ilusión");
    }

    @Test
    public void poderAuraAzulHaceDanioYEmpujaSegunLaDireccion() {
        Personaje derecha = rivalEnReposo();
        new PoderAuraAzul().aplicarImpacto(derecha, MULT, 1);
        assertEquals(VIDA_MAX - 48, derecha.getVidaActual(), "Daño de 40 x 1.2");
        avanzar(derecha, 1);
        assertEquals(500 + EMPUJE, derecha.getPosX(), DELTA, "Empuje a la derecha");
        assertFalse(derecha.aturdido() || derecha.congelado() || derecha.isConfundido(), "Sin estados alterados");

        Personaje izquierda = rivalEnReposo();
        new PoderAuraAzul().aplicarImpacto(izquierda, MULT, -1);
        avanzar(izquierda, 1);
        assertEquals(500 - EMPUJE, izquierda.getPosX(), DELTA, "Empuje a la izquierda");
    }

    @Test
    public void poderRayoElectrocutaDuranteTresSegundosYReduceLaMovilidad() {
        Personaje rival = rivalEnReposo();
        Personaje obstaculo = rivalEnReposo();
        obstaculo.resetearPosicion(800, 484);
        new PoderRayo().aplicarImpacto(rival, MULT, 1);
        assertEquals(VIDA_MAX - 72, rival.getVidaActual(), "Daño de 60 x 1.2");
        assertTrue(rival.isElectrificado(), "Queda electrificado");
        assertFalse(rival.aturdido(), "El rayo no produce aturdimiento completo");
        assertFalse(rival.congelado(), "No queda congelado");

        double xAntes = rival.getPosX();
        rival.mover(1, obstaculo);
        assertEquals(xAntes + Personaje.PASO_BASE * 1.2 / 2, rival.getPosX(), DELTA, "Speed 3 equivale a 120 % de velocidad base y el rayo la reduce a la mitad");
        double yAntes = rival.getPosY();
        rival.saltar();
        avanzar(rival, 1);
        assertEquals(yAntes, rival.getPosY(), DELTA, "No puede saltar mientras está electrificado");

        avanzar(rival, 3 * FPS - 2);
        assertTrue(rival.isElectrificado(), "Un cuadro antes de los 3 segundos sigue electrificado");
        avanzar(rival, 1);
        assertFalse(rival.isElectrificado(), "A los 3 segundos termina la electrocución");
    }

    @Test
    public void poderHieloHaceDanioYCongelaDosSegundosYMedio() {
        Personaje rival = rivalEnReposo();
        new PoderHielo().aplicarImpacto(rival, MULT, 1);
        assertEquals(VIDA_MAX - 42, rival.getVidaActual(), "Daño de 35 x 1.2");
        assertTrue(rival.congelado(), "Queda congelado");
        assertFalse(rival.aturdido(), "No queda aturdido");
        assertFalse(rival.isConfundido(), "No queda confundido");
        avanzar(rival, 149);
        assertTrue(rival.congelado(), "Un cuadro antes de los 2,5 segundos sigue congelado");
        avanzar(rival, 1);
        assertFalse(rival.congelado(), "A los 2,5 segundos exactos se descongela");
        assertEquals(VIDA_MAX - 42, rival.getVidaActual(), "Descongelarse no devuelve la vida");
        assertEquals(500, rival.getPosX(), DELTA, "El hielo no empuja");

        Personaje sinMultiplicador = rivalEnReposo();
        new PoderHielo().aplicarEfecto(sinMultiplicador);
        assertEquals(VIDA_MAX - 35, sinMultiplicador.getVidaActual(), "aplicarEfecto sin multiplicador usa 1.0");
        assertTrue(sinMultiplicador.congelado(), "aplicarEfecto sin multiplicador también congela");
    }

    @Test
    public void poderTornadoHaceDanioYLanzaAlRivalInconscienteTresSegundos() {
        Personaje rival = rivalEnReposo();
        new PoderTornado().aplicarImpacto(rival, MULT, -1);
        assertEquals(VIDA_MAX - 48, rival.getVidaActual(), "Daño de 40 x 1.2");
        assertTrue(rival.isInconsciente(), "Queda inconsciente");
        assertFalse(rival.isConfundido(), "El tornado no aplica confusión");
        assertFalse(rival.aturdido() || rival.congelado(), "No queda aturdido por daño cero ni congelado");
        avanzar(rival, 1);
        assertEquals(500, rival.getPosX(), DELTA, "El tornado lo lanza verticalmente");
        assertTrue(rival.getPosY() < 500, "El rival es lanzado hacia arriba");
        avanzar(rival, 3 * FPS - 2);
        assertTrue(rival.isInconsciente(), "Un cuadro antes de los 3 segundos sigue inconsciente");
        avanzar(rival, 1);
        assertFalse(rival.isInconsciente(), "A los 3 segundos recupera el control");

        Personaje sinMultiplicador = rivalEnReposo();
        new PoderTornado().aplicarEfecto(sinMultiplicador);
        assertEquals(VIDA_MAX - 40, sinMultiplicador.getVidaActual(), "aplicarEfecto sin multiplicador usa 1.0");
        assertTrue(sinMultiplicador.isInconsciente(), "aplicarEfecto también deja inconsciente");
    }

    @Test
    public void elDanioEscalaConElMultiplicadorEnTodosLosPoderes() {
        Poder[] poderes = {new PoderAuraAzul(), new PoderRayo(), new PoderHielo(), new PoderTornado(), new PoderIlusion(), new PoderFuego()};
        int[] base = {40, 60, 35, 40, 30, 25};
        double[] multiplicadores = {1.0, 1.5};
        for (int i = 0; i < poderes.length; i++) {
            for (double mult : multiplicadores) {
                Personaje rival = rivalEnReposo();
                poderes[i].aplicarImpacto(rival, mult, 1);
                int esperado = VIDA_MAX - danio(base[i], mult);
                assertEquals(esperado, rival.getVidaActual(),
                        poderes[i].getClass().getSimpleName() + " con multiplicador " + mult);
            }
        }
    }

    @Test
    public void elEmpujeNoMueveAUnRivalAturdidoOCongelado() {
        Personaje aturdido = rivalEnReposo();
        aturdido.aturdir(3);
        new PoderAuraAzul().aplicarImpacto(aturdido, MULT, 1);
        avanzar(aturdido, 1);
        assertEquals(VIDA_MAX - 48, aturdido.getVidaActual(), "Aturdido: recibe el daño");
        assertEquals(500, aturdido.getPosX(), DELTA, "Aturdido: no se mueve");

        Personaje congelado = rivalEnReposo();
        congelado.congelar(3);
        new PoderTornado().aplicarImpacto(congelado, MULT, 1);
        avanzar(congelado, 1);
        assertEquals(VIDA_MAX - 48, congelado.getVidaActual(), "Congelado: recibe el daño del tornado");
        assertEquals(500, congelado.getPosX(), DELTA, "Congelado: no se mueve");
    }

    @Test
    public void laInterfazPorDefectoDelegaEnAplicarEfectoYUsaVelocidadNormal() {
        int[] llamadas = {0};
        Poder minimo = new Poder() {
            @Override
            public void aplicarEfecto(Personaje objetivo) {
                llamadas[0]++;
                objetivo.recibirDanio(10);
            }
        };
        Personaje rival = rivalEnReposo();
        assertEquals(12, minimo.velocidadDisparo(), DELTA, "Velocidad por defecto");
        minimo.aplicarEfecto(rival, 2.0);
        minimo.aplicarImpacto(rival, 2.0, 1);
        assertEquals(2, llamadas[0], "Las dos variantes delegan en aplicarEfecto(Personaje)");
        assertEquals(VIDA_MAX - 20, rival.getVidaActual(), "Cada llamada hizo 10 de daño");
    }

    // Efecto de cada país en el partido

    @Test
    public void disparoEspecialDeCadaPaisAplicaSuEfectoAlRival() {
        for (String pais : CatalogoPersonajes.paises()) {
            Partido partido = partido(pais, "Camerún");
            Personaje rival = partido.getJugador2().getPersonaje();
            double xAntes = rival.getPosX();
            lanzarDisparoSobreElRival(partido, 1);
            assertFalse(partido.getBalon().isDisparoEspecial(), pais + ": el disparo se consume al impactar");
            verificarEfecto(pais, partido, rival, xAntes, 1);
        }
    }

    @Test
    public void elEmpujeDependeDeQuienLanzaElDisparo() {
        for (String pais : new String[]{"Corea del Sur"}) {
            Partido comoJugador1 = partido(pais, "Camerún");
            Personaje rivalDerecha = comoJugador1.getJugador2().getPersonaje();
            double x1 = rivalDerecha.getPosX();
            lanzarDisparoSobreElRival(comoJugador1, 1);
            comoJugador1.actualizarEstado();
            assertEquals(x1 + EMPUJE, rivalDerecha.getPosX(), DELTA, pais + " como jugador 1 empuja a la derecha");

            Partido comoJugador2 = partido("Camerún", pais);
            Personaje rivalIzquierda = comoJugador2.getJugador1().getPersonaje();
            double x2 = rivalIzquierda.getPosX();
            lanzarDisparoSobreElRival(comoJugador2, 2);
            comoJugador2.actualizarEstado();
            assertEquals(x2 - EMPUJE, rivalIzquierda.getPosX(), DELTA, pais + " como jugador 2 empuja a la izquierda");
        }
    }

    @Test
    public void sinImpactoNingunPoderAfectaAlRival() {
        for (String pais : CatalogoPersonajes.paises()) {
            Partido partido = partido(pais, "Camerún");
            Personaje rival = partido.getJugador2().getPersonaje();
            partido.getBalon().aplicarDisparoEspecial(0, 0, 1);
            for (int i = 0; i < 5; i++) partido.actualizarEstado();
            assertTrue(partido.getBalon().isDisparoEspecial(), pais + ": el disparo sigue activo");
            assertEquals(VIDA_MAX, rival.getVidaActual(), pais + ": el rival no pierde vida");
            assertFalse(rival.aturdido() || rival.congelado() || rival.isConfundido()
                    || rival.isElectrificado() || rival.isInconsciente(), pais + ": sin estados");
        }
    }

    // Velocidad al patear

    @Test
    public void laPatadaConPoderPreparadoDisparaElBalonComoTiroEspecial() {
        for (String pais : CatalogoPersonajes.paises()) {
            Partido partido = partido(pais, "Camerún");
            patearConPoderPreparado(partido);
            Balon balon = partido.getBalon();

            assertTrue(balon.isDisparoEspecial(), pais + ": el balón sale como disparo especial");
            assertEquals(1, balon.getPropietarioDisparo(), pais + ": se registra quién lanzó el poder");
            assertEquals(velocidadEsperada(pais), balon.getVelX(), DELTA,
                    pais + ": se usa la velocidad definida por su habilidad");
            assertFalse(partido.getJugador1().tienePoderPreparado(),
                    pais + ": el poder se consume cuando la patada alcanza el balón");
        }
    }

    @Test
    public void sinPoderPreparadoLaPatadaEsNormalParaTodosLosPaises() {
        for (String pais : CatalogoPersonajes.paises()) {
            Partido partido = partido(pais, "Camerún");
            patearBalonEncima(partido);
            Balon balon = partido.getBalon();
            assertFalse(balon.isDisparoEspecial(), pais + ": patada normal");
            assertEquals(12, balon.getVelX(), DELTA, pais + ": velocidad normal");
        }
    }

    // El efecto depende del poder, no del país

    @Test
    public void elEfectoDependeDelPoderYNoDelNombreDelPais() {
        // Dice ser de Corea, pero con poder de hielo debe congelar y disparar como el hielo.
        Personaje falsoCorea = new Personaje("Falso", "Corea del Sur", 3, 3, 3, 3, new PoderHielo());
        Partido hielo = new PartidoPorGoles(jugador(falsoCorea, true), jugador(personajeDePrueba("Camerún"), false), 5);
        Personaje rivalHielo = hielo.getJugador2().getPersonaje();
        double x = rivalHielo.getPosX();
        lanzarDisparoSobreElRival(hielo, 1);
        hielo.actualizarEstado();
        assertTrue(rivalHielo.congelado(), "El poder de hielo congela aunque el país sea Corea");
        assertEquals(VIDA_MAX - 42, rivalHielo.getVidaActual(), "Daño del hielo");
        assertEquals(x, rivalHielo.getPosX(), DELTA, "Sin empuje de Corea");

        Personaje falsoCorea2 = new Personaje("Falso", "Corea del Sur", 3, 3, 3, 3, new PoderHielo());
        Partido patada = new PartidoPorGoles(jugador(falsoCorea2, true), jugador(personajeDePrueba("Camerún"), false), 5);
        patearConPoderPreparado(patada);
        assertEquals(11, patada.getBalon().getVelX(), DELTA, "No hereda la velocidad 24 de Corea");

        // Un país que antes no tenía ningún efecto ahora lo toma de su poder.
        Personaje brasil = new Personaje("Brasil", "Brasil", 3, 3, 3, 3, new PoderRayo());
        Partido rayo = new PartidoPorGoles(jugador(brasil, true), jugador(personajeDePrueba("Camerún"), false), 5);
        Personaje rivalRayo = rayo.getJugador2().getPersonaje();
        lanzarDisparoSobreElRival(rayo, 1);
        assertTrue(rivalRayo.isElectrificado(), "El rayo electrocuta aunque el país no esté en el catálogo");
        assertEquals(VIDA_MAX - 72, rivalRayo.getVidaActual(), "Daño del rayo");
    }

    @Test
    public void personajeSinPoderNoFallaAlImpactarNiAlPatear() {
        Personaje sinPoder = new Personaje("Sin", "Sin", 3, 3, 3, 3, null);
        Partido impacto = new PartidoPorGoles(jugador(sinPoder, true), jugador(personajeDePrueba("Camerún"), false), 5);
        Personaje rival = impacto.getJugador2().getPersonaje();
        assertDoesNotThrow(() -> lanzarDisparoSobreElRival(impacto, 1), "Impacto sin poder");
        assertEquals(VIDA_MAX, rival.getVidaActual(), "El rival no recibe daño");
        assertFalse(rival.aturdido() || rival.congelado() || rival.isConfundido()
                || rival.isElectrificado() || rival.isInconsciente(), "El rival no recibe estados");
        assertFalse(impacto.getBalon().isDisparoEspecial(), "El disparo igual se consume");

        Personaje otroSinPoder = new Personaje("Sin", "Sin", 3, 3, 3, 3, null);
        Partido patada = new PartidoPorGoles(jugador(otroSinPoder, true), jugador(personajeDePrueba("Camerún"), false), 5);
        assertDoesNotThrow(() -> patearBalonEncima(patada), "Patada sin poder");
        assertEquals(12, patada.getBalon().getVelX(), DELTA, "Velocidad normal");
    }


    @Test
    public void tocarAlRivalNoActivaElPoderSiElBalonNoHaSidoPateado() {
        for (String pais : CatalogoPersonajes.paises()) {
            Partido partido = partido(pais, "Camerún");
            Personaje atacante = partido.getJugador1().getPersonaje();
            Personaje rival = partido.getJugador2().getPersonaje();

            // Se ponen los personajes uno junto al otro y se deja el balón lejos.
            rival.resetearPosicion(atacante.getPosX() + Personaje.ANCHO, atacante.getPosY());
            partido.getBalon().resetearPosicion(500, 100);
            partido.getJugador1().cargarPoder(100);
            partido.getJugador1().realizarAccion("Q", partido);

            partido.actualizarEstado();

            assertTrue(partido.getJugador1().tienePoderPreparado(),
                    pais + ": tocar al rival no consume el poder");
            assertEquals(VIDA_MAX, rival.getVidaActual(),
                    pais + ": el rival no recibe el poder antes del impacto del balón");
            assertFalse(rival.aturdido() || rival.congelado() || rival.isConfundido()
                    || rival.isElectrificado() || rival.isInconsciente(),
                    pais + ": no se aplica ningún estado antes del impacto del balón");
            assertFalse(partido.getBalon().isDisparoEspecial(),
                    pais + ": todavía no se ha lanzado el balón especial");
        }
    }


    @Test
    public void laConfusionNoCambiaLosControlesDeMovimiento() {
        Personaje confundido = rivalEnReposo();
        Personaje obstaculo = rivalEnReposo();
        confundido.resetearPosicion(200, 400);
        obstaculo.resetearPosicion(800, 400);

        confundido.aplicarConfusion(3);
        confundido.mover(1, obstaculo);

        assertEquals(200 + Personaje.PASO_BASE * 1.2, confundido.getPosX(), DELTA,
                "La confusión está en no saber cuál es el balón real, no en los controles");
    }
}
