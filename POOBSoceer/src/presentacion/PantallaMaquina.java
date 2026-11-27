package presentacion;

import Domain.CatalogoMaquinas;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Pantalla para elegir el perfil de la máquina. La lista sale del catálogo, así que los perfiles nuevos aparecen solos. */
class PantallaMaquina extends JPanel {

    /**
     * @param alElegirPerfil acción que recibe el nombre del perfil elegido
     * @param alVolver       acción al presionar VOLVER
     */
    PantallaMaquina(Consumer<String> alElegirPerfil, Runnable alVolver) {
        super(new BorderLayout());
        JPanel p = EstiloGUI.fondo();
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel h = EstiloGUI.titulo("SELECCIONA LA MÁQUINA", 28);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(h);
        box.add(Box.createVerticalStrut(25));

        List<String> perfiles = CatalogoMaquinas.perfiles();
        JPanel opciones = new JPanel(new GridLayout(0, 2, 25, 18));
        opciones.setOpaque(false);
        for (String perfil : perfiles) opciones.add(crearOpcion(perfil, alElegirPerfil));
        box.add(opciones);
        box.add(Box.createVerticalStrut(25));

        JButton volver = EstiloGUI.boton("VOLVER");
        volver.setAlignmentX(Component.CENTER_ALIGNMENT);
        volver.addActionListener(e -> alVolver.run());
        box.add(volver);

        p.add(box);
        add(p, BorderLayout.CENTER);
    }

    /** Botón del perfil con una frase debajo que explica cómo juega. */
    private JPanel crearOpcion(String perfil, Consumer<String> alElegirPerfil) {
        JPanel opcion = new JPanel(new BorderLayout(0, 6));
        opcion.setOpaque(false);
        JButton boton = EstiloGUI.boton(perfil.toUpperCase());
        boton.addActionListener(e -> alElegirPerfil.accept(perfil));
        opcion.add(boton, BorderLayout.CENTER);
        opcion.add(EstiloGUI.titulo(CatalogoMaquinas.crear(perfil).descripcion(), 14), BorderLayout.SOUTH);
        return opcion;
    }
}
