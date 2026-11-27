package presentacion;

import Domain.POOBSoccer;
import javax.swing.*;
import java.awt.*;

/**
 * Pantalla del partido: barra de botones (pausa, terminar, menú) arriba y la cancha debajo.
 * Se crea una nueva por cada partido.
 */
class PantallaJuego extends JPanel {

    private final PanelCancha panelCancha;

    /**
     * @param dominio    el juego que se muestra y se controla
     * @param alIrAlMenu acción al presionar MENÚ
     */
    PantallaJuego(POOBSoccer dominio, Runnable alIrAlMenu) {
        super(new BorderLayout());
        panelCancha = new PanelCancha(dominio);
        panelCancha.setFocusable(true);
        add(crearBarra(dominio, alIrAlMenu), BorderLayout.NORTH);
        add(panelCancha, BorderLayout.CENTER);
    }

    private JPanel crearBarra(POOBSoccer dominio, Runnable alIrAlMenu) {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));

        JButton pausa = EstiloGUI.boton("PAUSAR / REANUDAR");
        pausa.setPreferredSize(new Dimension(220, 40));
        pausa.addActionListener(e -> {
            if (dominio.isPausa()) dominio.reanudarJuego();
            else dominio.pausarJuego();
            // Se devuelve el foco a la cancha para que las teclas sigan respondiendo.
            enfocar();
        });

        JButton terminar = EstiloGUI.boton("TERMINAR PARTIDO");
        terminar.setPreferredSize(new Dimension(190, 40));
        terminar.addActionListener(e -> {
            dominio.getPartidoActual().terminar();
            repintar();
        });

        JButton menu = EstiloGUI.boton("MENÚ");
        menu.setPreferredSize(new Dimension(120, 40));
        menu.addActionListener(e -> alIrAlMenu.run());

        barra.add(pausa);
        barra.add(terminar);
        barra.add(menu);
        return barra;
    }

    /** Vuelve a dibujar la cancha con el estado actual del partido. */
    void repintar() {
        panelCancha.repaint();
    }

    /** Pone el foco en la cancha para que el teclado responda. */
    void enfocar() {
        panelCancha.requestFocusInWindow();
    }
}
