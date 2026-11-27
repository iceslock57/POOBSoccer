package presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Pantalla para elegir el tipo de partido: por tiempo o por goles.
 * Avisa el tipo elegido ("Tiempo" o "Goles") a quien la crea.
 */
class PantallaTipos extends JPanel {

    /**
     * @param alElegirTipo acción que recibe el tipo elegido: "Tiempo" o "Goles"
     * @param alVolver     acción al presionar VOLVER
     */
    PantallaTipos(Consumer<String> alElegirTipo, Runnable alVolver) {
        super(new BorderLayout());
        JPanel p = EstiloGUI.fondo();
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel h = EstiloGUI.titulo("SELECCIONA EL TIPO DE PARTIDO", 28);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(h);
        box.add(Box.createVerticalStrut(35));

        JButton tiempo = EstiloGUI.boton("PARTIDO POR TIEMPO");
        tiempo.setAlignmentX(Component.CENTER_ALIGNMENT);
        tiempo.addActionListener(e -> alElegirTipo.accept("Tiempo"));
        box.add(tiempo);
        box.add(Box.createVerticalStrut(18));

        JButton goles = EstiloGUI.boton("PARTIDO POR GOLES");
        goles.setAlignmentX(Component.CENTER_ALIGNMENT);
        goles.addActionListener(e -> alElegirTipo.accept("Goles"));
        box.add(goles);
        box.add(Box.createVerticalStrut(25));

        JButton volver = EstiloGUI.boton("VOLVER");
        volver.setAlignmentX(Component.CENTER_ALIGNMENT);
        volver.addActionListener(e -> alVolver.run());
        box.add(volver);

        p.add(box);
        add(p, BorderLayout.CENTER);
    }
}
