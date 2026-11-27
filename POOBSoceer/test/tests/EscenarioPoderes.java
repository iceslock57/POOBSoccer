package tests;

import Domain.*;
import static org.junit.jupiter.api.Assertions.*;

/** Personajes y partidos ya preparados que comparten las pruebas de los poderes. */
final class EscenarioPoderes {

    static final int FPS = 60;
    static final int VIDA_MAX = 120;  // survival 3 -> 100 + 10 * 2

    private EscenarioPoderes() {
    }

    /** Rival quieto en el suelo y a mitad de una cancha de 1000 px, para observar efectos y empujes. */
    static Personaje rivalEnReposo() {
        Personaje p = new Personaje("Rival", "Rival", 3, 3, 3, 3, new PoderRayo());
        p.configurarCancha(1000, 564);
        p.resetearPosicion(500, 484);
        return p;
    }

    static void avanzar(Personaje p, int frames) {
        for (int i = 0; i < frames; i++) p.actualizarEstado();
    }

    static Jugador jugador(Personaje personaje, boolean primero) {
        String[] teclas = primero
                ? new String[]{"A", "D", "W", "S", "Q"}
                : new String[]{"LEFT", "RIGHT", "UP", "M", "N"};
        return new JugadorHumano(personaje.getNombre(), personaje, teclas);
    }

    static Personaje personajeDePrueba(String pais) {
        Poder poder = CatalogoPersonajes.crear(pais).getPoderEspecial();
        // Estas pruebas usan nivel 3 para comprobar los multiplicadores de daño ya conocidos (1.2).
        return new Personaje(pais, pais, 3, 3, 3, 3, poder);
    }

    static Partido partido(String paisJ1, String paisJ2) {
        Jugador j1 = jugador(personajeDePrueba(paisJ1), true);
        Jugador j2 = jugador(personajeDePrueba(paisJ2), false);
        return new PartidoPorGoles(j1, j2, 5);
    }

    /** Pone un disparo especial del jugador "dueno" sobre su rival y avanza el cuadro del impacto. */
    static void lanzarDisparoSobreElRival(Partido p, int dueno) {
        Personaje rival = (dueno == 1 ? p.getJugador2() : p.getJugador1()).getPersonaje();
        p.getBalon().resetearPosicion(rival.getPosX() + 20, rival.getPosY() + 20);
        p.getBalon().aplicarDisparoEspecial(0, 0, dueno);
        p.actualizarEstado();
    }

    /** Prepara el poder y comprueba que la siguiente patada al balón lo convierte en un disparo especial. */
    static void patearConPoderPreparado(Partido partido) {
        Jugador atacante = partido.getJugador1();
        atacante.cargarPoder(100);
        atacante.realizarAccion("Q", partido);
        assertTrue(atacante.tienePoderPreparado(), "Precondición: poder preparado");
        patearBalonEncima(partido);
    }

    /** Ejecuta la acción real de patada con el balón situado delante del zapato. */
    static void patearBalonEncima(Partido partido) {
        Personaje p = partido.getJugador1().getPersonaje();
        partido.getBalon().resetearPosicion(p.getPosX() + 20, p.getPosY() + 20);
        partido.getJugador1().realizarAccion("S", partido);
        partido.actualizarEstado();
    }
}
