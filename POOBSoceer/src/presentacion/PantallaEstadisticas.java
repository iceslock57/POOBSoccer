package presentacion;

import Domain.CatalogoPersonajes;
import Domain.EstadisticasPersonaje;
import Domain.PerfilJugador;
import Domain.Personaje;
import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Pantalla para mejorar un personaje con los puntos ganados en los partidos.
 * Las mejoras son solo del jugador y del país que se están mostrando.
 */
class PantallaEstadisticas extends JPanel {

    // Nombre con el que se muestra cada estadística.
    private static final Map<PerfilJugador.Estadistica, String> NOMBRES = new EnumMap<>(PerfilJugador.Estadistica.class);

    static {
        NOMBRES.put(PerfilJugador.Estadistica.SPEED, "Velocidad");
        NOMBRES.put(PerfilJugador.Estadistica.JUMP, "Salto");
        NOMBRES.put(PerfilJugador.Estadistica.KICK, "Patada");
        NOMBRES.put(PerfilJugador.Estadistica.VIDA, "Vida");
    }

    private final JLabel titulo = EstiloGUI.titulo("", 24);
    private final JLabel puntos = EstiloGUI.titulo("", 20);
    private final JLabel mensaje = EstiloGUI.titulo(" ", 16);
    private final Map<PerfilJugador.Estadistica, JProgressBar> barras = new EnumMap<>(PerfilJugador.Estadistica.class);
    private final Map<PerfilJugador.Estadistica, JLabel> efectos = new EnumMap<>(PerfilJugador.Estadistica.class);
    private final Map<PerfilJugador.Estadistica, JButton> botones = new EnumMap<>(PerfilJugador.Estadistica.class);

    private PerfilJugador perfil;
    private String pais;

    /** @param alVolver acción al presionar VOLVER */
    PantallaEstadisticas(Runnable alVolver) {
        super(new BorderLayout());
        JPanel p = EstiloGUI.fondo();
        JPanel box = crearRecuadro();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        agregarCentrado(box, titulo, 8);
        agregarCentrado(box, puntos, 20);
        agregarCentrado(box, crearTabla(), 14);
        agregarCentrado(box, mensaje, 14);
        JButton volver = EstiloGUI.boton("VOLVER");
        volver.addActionListener(e -> alVolver.run());
        agregarCentrado(box, volver, 0);

        p.add(box);
        add(p, BorderLayout.CENTER);
    }

    /** Recuadro oscuro detrás del contenido, para que el texto se lea sobre el fondo. */
    private JPanel crearRecuadro() {
        JPanel recuadro = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            }
        };
        recuadro.setOpaque(false);
        recuadro.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return recuadro;
    }

    private void agregarCentrado(JPanel box, JComponent componente, int espacio) {
        componente.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(componente);
        box.add(Box.createVerticalStrut(espacio));
    }

    /** Una fila por estadística: nombre, nivel, lo que se gana con ese nivel y el botón para mejorar. */
    private JPanel crearTabla() {
        JPanel tabla = new JPanel(new GridBagLayout());
        tabla.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 10, 7, 10);
        c.fill = GridBagConstraints.BOTH;

        for (PerfilJugador.Estadistica e : NOMBRES.keySet()) {
            c.gridy++;
            c.gridx = 0;
            JLabel nombre = EstiloGUI.titulo(NOMBRES.get(e), 18);
            nombre.setHorizontalAlignment(SwingConstants.LEFT);
            tabla.add(nombre, c);

            c.gridx = 1;
            JProgressBar barra = new JProgressBar(0, EstadisticasPersonaje.NIVEL_MAXIMO);
            barra.setPreferredSize(new Dimension(180, 26));
            barra.setMinimumSize(new Dimension(110, 26));
            barra.setStringPainted(true);
            barra.setForeground(new Color(40, 150, 75));
            barras.put(e, barra);
            tabla.add(barra, c);

            c.gridx = 2;
            JLabel efecto = EstiloGUI.titulo("", 15);
            efecto.setPreferredSize(new Dimension(95, 26));
            efectos.put(e, efecto);
            tabla.add(efecto, c);

            c.gridx = 3;
            JButton boton = EstiloGUI.boton("");
            boton.setFont(new Font("Arial", Font.BOLD, 15));
            boton.setPreferredSize(new Dimension(200, 40));
            boton.addActionListener(evento -> mejorar(e));
            botones.put(e, boton);
            tabla.add(boton, c);
        }
        return tabla;
    }

    /** Muestra las estadísticas que tiene ese jugador para ese país. */
    void mostrar(String nombreJugador, PerfilJugador perfil, String pais) {
        this.perfil = perfil;
        this.pais = pais;
        titulo.setText("ESTADÍSTICAS DE " + pais.toUpperCase() + " - " + nombreJugador);
        mensaje.setText(" ");
        refrescar();
    }

    private void mejorar(PerfilJugador.Estadistica estadistica) {
        boolean comprada = perfil.mejorar(pais, estadistica);
        mensaje.setText(comprada
                ? NOMBRES.get(estadistica) + " de " + pais + " subió al nivel " + perfil.getNivel(pais, estadistica)
                : "No alcanzan los puntos para esta mejora");
        refrescar();
    }

    private void refrescar() {
        puntos.setText("Puntos disponibles: " + perfil.getPuntos());
        for (PerfilJugador.Estadistica e : NOMBRES.keySet()) {
            int nivel = perfil.getNivel(pais, e);
            barras.get(e).setValue(nivel);
            barras.get(e).setString(nivel + " / " + EstadisticasPersonaje.NIVEL_MAXIMO);
            efectos.get(e).setText(efectoDe(e, nivel));

            boolean alMaximo = nivel == EstadisticasPersonaje.NIVEL_MAXIMO;
            botones.get(e).setText(alMaximo ? "MÁXIMO" : "MEJORAR (" + perfil.getCostoSiguienteMejora(pais) + ")");
            botones.get(e).setEnabled(perfil.puedeMejorar(pais, e));
        }
    }

    /** La vida se muestra con su valor real y las demás con el porcentaje que ganan. */
    private String efectoDe(PerfilJugador.Estadistica estadistica, int nivel) {
        if (estadistica == PerfilJugador.Estadistica.VIDA) {
            Personaje personaje = CatalogoPersonajes.crear(pais, perfil);
            return personaje.getVidaMaxima() + " HP";
        }
        return "+" + (nivel - 1) * 10 + " %";
    }
}
