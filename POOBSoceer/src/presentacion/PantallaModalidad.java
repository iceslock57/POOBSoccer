package presentacion;

import javax.swing.*;
import java.awt.*;

/** Pantalla para elegir contra quién se juega: otra persona o la máquina. */
class PantallaModalidad extends JPanel {

    /**
     * @param contraJugador acción al elegir jugador vs jugador
     * @param contraMaquina acción al elegir jugador vs máquina
     * @param alVolver      acción al presionar VOLVER
     */
    PantallaModalidad(Runnable contraJugador, Runnable contraMaquina, Runnable alVolver) {
        super(new BorderLayout());
        JPanel p = EstiloGUI.fondo();
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel h = EstiloGUI.titulo("SELECCIONA EL MODO DE JUEGO", 28);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(h);
        box.add(Box.createVerticalStrut(35));
        agregarBoton(box, "JUGADOR VS JUGADOR", contraJugador);
        box.add(Box.createVerticalStrut(18));
        agregarBoton(box, "JUGADOR VS MÁQUINA", contraMaquina);
        box.add(Box.createVerticalStrut(25));
        agregarBoton(box, "VOLVER", alVolver);

        p.add(box);
        add(p, BorderLayout.CENTER);
    }

    private void agregarBoton(JPanel box, String texto, Runnable accion) {
        JButton boton = EstiloGUI.boton(texto);
        boton.setAlignmentX(Component.CENTER_ALIGNMENT);
        boton.addActionListener(e -> accion.run());
        box.add(boton);
    }
}
