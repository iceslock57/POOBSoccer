package presentacion;

import Domain.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lee el teclado y le avisa al dominio qué acción hizo cada jugador.
 * No mueve a nadie por sí mismo: el movimiento y la física los resuelve el dominio (Jugador.realizarAccion).
 *
 * Hay dos tipos de teclas:
 * - Continuas (moverse): mientras estén presionadas, el jugador se mueve en cada cuadro del juego.
 *   Así el movimiento es fluido y no se detiene al presionar otra tecla al mismo tiempo.
 * - De una vez (saltar, patear, poder): actúan en el momento en que se presionan.
 *
 * Controles: J1 = A/D mover, W saltar, S patear, Q poder.
 *            J2 = flechas izquierda/derecha mover, flecha arriba saltar, M patear, N poder.
 * Los nombres enviados deben coincidir con el orden de teclas de Domain.POOBSoccer.
 */
class ControladorTeclado {

    private final POOBSoccer dominio;
    private final JComponent raiz;
    private final Runnable alActuar;
    // Teclas continuas que están presionadas en este momento.
    private final Set<Integer> teclasPresionadas = new HashSet<>();
    // Qué hace cada tecla continua en cada cuadro.
    private final Map<Integer, Runnable> accionesContinuas = new HashMap<>();

    /**
     * @param dominio  el juego al que se le envían las acciones
     * @param raiz     componente de la ventana donde se registran las teclas
     * @param alActuar qué hacer después de cada acción (por ejemplo, repintar la cancha)
     */
    ControladorTeclado(POOBSoccer dominio, JComponent raiz, Runnable alActuar) {
        this.dominio = dominio;
        this.raiz = raiz;
        this.alActuar = alActuar;
        instalarControles();
    }

    /** Olvida las teclas presionadas; se usa al empezar un partido o cuando la ventana pierde el foco. */
    void reiniciar() {
        teclasPresionadas.clear();
    }

    /** Se llama en cada cuadro del bucle del juego: mueve a los jugadores según las teclas que sigan presionadas. */
    void procesarTeclasContinuas() {
        for (int tecla : teclasPresionadas) {
            accionesContinuas.get(tecla).run();
        }
    }

    // Internos

    private void instalarControles() {
        // Los key bindings usan WHEN_IN_FOCUSED_WINDOW, así que responden aunque el foco no esté en la cancha.
        continua(KeyEvent.VK_A, "p1left", () -> ejecutar(1, "A"));
        continua(KeyEvent.VK_D, "p1right", () -> ejecutar(1, "D"));
        alPresionar(KeyEvent.VK_W, "p1jump", () -> accion(1, "W"));
        alPresionar(KeyEvent.VK_S, "p1kick", () -> accion(1, "S"));
        alPresionar(KeyEvent.VK_Q, "p1power", () -> accion(1, "Q"));

        continua(KeyEvent.VK_LEFT, "p2left", () -> ejecutar(2, "LEFT"));
        continua(KeyEvent.VK_RIGHT, "p2right", () -> ejecutar(2, "RIGHT"));
        alPresionar(KeyEvent.VK_UP, "p2jump", () -> accion(2, "UP"));
        alPresionar(KeyEvent.VK_M, "p2kick", () -> accion(2, "M"));
        alPresionar(KeyEvent.VK_N, "p2power", () -> accion(2, "N"));
    }

    /** Tecla que actúa una sola vez, en el momento en que se presiona. */
    private void alPresionar(int key, String name, Runnable action) {
        registrar(key, false, name, () -> {
            // Las teclas solo actúan mientras hay una partida en curso y sin pausa.
            Partido actual = dominio.getPartidoActual();
            if (actual != null && !dominio.isPausa() && !actual.isTerminado()) action.run();
        });
    }

    /** Tecla que actúa en cada cuadro mientras esté presionada (ver procesarTeclasContinuas). */
    private void continua(int key, String name, Runnable action) {
        accionesContinuas.put(key, action);
        registrar(key, false, name + "Presionada", () -> teclasPresionadas.add(key));
        // Soltar siempre se registra, incluso en pausa; si no, la tecla quedaría "pegada" al reanudar.
        registrar(key, true, name + "Soltada", () -> teclasPresionadas.remove(key));
    }

    private void registrar(int key, boolean alSoltar, String name, Runnable action) {
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0, alSoltar), name);
        raiz.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /** Envía la acción al jugador y repinta (para teclas de una vez). */
    private void accion(int n, String tecla) {
        ejecutar(n, tecla);
        alActuar.run();
    }

    /** Envía la acción al jugador sin repintar (el bucle del juego ya repinta en cada cuadro). */
    private void ejecutar(int n, String tecla) {
        Partido p = dominio.getPartidoActual();
        if (p == null) return;
        Jugador j = n == 1 ? p.getJugador1() : p.getJugador2();
        j.realizarAccion(tecla, p);
    }
}
