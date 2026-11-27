package presentacion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Pantalla de inicio con los botones PLAY y SALIR.
 * Si existe la portada (recursos/portada_poobsoccer.png) se usa de fondo y los botones van sobre el césped;
 * si no, se muestra el fondo normal con el título en texto.
 * No sabe a qué pantalla se va después: eso lo decide quien la crea, por medio de las acciones que recibe.
 */
class PantallaInicio extends JPanel {

    /**
     * @param alJugar acción al presionar PLAY
     * @param alSalir acción al presionar SALIR
     */
    PantallaInicio(Runnable alJugar, Runnable alSalir) {
        super(new BorderLayout());

        JButton play = EstiloGUI.boton("PLAY");
        play.addActionListener(e -> alJugar.run());
        JButton salir = EstiloGUI.boton("SALIR");
        salir.addActionListener(e -> alSalir.run());

        if (EstiloGUI.hayPortada()) {
            add(conPortada(play, salir), BorderLayout.CENTER);
        } else {
            add(sinPortada(play, salir), BorderLayout.CENTER);
        }
    }

    /** La portada ya trae el título; los botones van uno al lado del otro sobre el césped. */
    private JPanel conPortada(JButton play, JButton salir) {
        JPanel p = EstiloGUI.portada();
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        botones.setOpaque(false);
        botones.add(play);
        botones.add(salir);
        p.add(botones, BorderLayout.SOUTH);

        // La franja de césped cambia de alto con la ventana; se recalcula el margen inferior
        // para que los botones queden centrados sobre ella.
        p.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int cesped = EstiloGUI.altoCespedPortada(p.getWidth(), p.getHeight());
                int margen = Math.max(8, (cesped - play.getPreferredSize().height) / 2);
                p.setBorder(BorderFactory.createEmptyBorder(0, 0, margen, 0));
                p.revalidate();
            }
        });
        return p;
    }

    /** Versión anterior: fondo normal con el título en texto y los botones en columna. */
    private JPanel sinPortada(JButton play, JButton salir) {
        JPanel p = EstiloGUI.fondo();
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel logo = EstiloGUI.titulo("POOBSoccer", 42);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(logo);
        box.add(Box.createVerticalStrut(45));

        play.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(play);
        box.add(Box.createVerticalStrut(15));
        salir.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(salir);

        p.add(box);
        return p;
    }
}
