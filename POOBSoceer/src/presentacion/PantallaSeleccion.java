package presentacion;

import Domain.CatalogoPersonajes;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Pantalla para elegir el país de cada jugador, el tamaño de la cancha y el objetivo del partido
 * (minutos o goles). Guarda lo que el usuario elige y lo entrega con sus getters.
 */
class PantallaSeleccion extends JPanel {

    private static final String[] TAMANOS = {"Pequeña (800 x 550)", "Mediana (1000 x 650)", "Grande (1200 x 750)"};
    // ANCHOS[i] y ALTOS[i] corresponden a TAMANOS[i].
    private static final int[] ANCHOS = {800, 1000, 1200};
    private static final int[] ALTOS = {550, 650, 750};

    // Imagen de presentación de cada país. Los países que no estén aquí se muestran solo con su nombre.
    // Para agregar otro: PAIS -> "recursos/fondo_<pais>.jpg".
    private static final Map<String, String> IMAGENES_PAISES = Map.of(
            "Corea del Sur", "recursos/fondo_corea.jpg",
            "Camerún", "recursos/fondo_camerun.jpg",
            "Rusia", "recursos/fondo_rusia.jpg",
            "Japón", "recursos/fondo_japon.png",
            "Estados Unidos", "recursos/fondo_estadosunidos.png",
            "Nigeria", "recursos/fondo_nigeria.jpg");
    
    private static final int ANCHO_TARJETA = 220, ALTO_TARJETA = 123;
    // Las imágenes se escalan una sola vez y se guardan aquí, para no repetirlo en cada cambio.
    private final Map<String, ImageIcon> imagenesEscaladas = new HashMap<>();

    // La lista sale del catálogo, así que agregar un personaje nuevo no obliga a tocar esta clase.
    private final List<String> paises = CatalogoPersonajes.paises();
    private int indice1 = 0, indice2 = 1;
    private JLabel paisJ1, paisJ2;
    private final JLabel tituloJ1 = new JLabel("JUGADOR 1"), tituloJ2 = new JLabel("JUGADOR 2");
    private final JButton estadisticasJ1 = EstiloGUI.boton("ESTADÍSTICAS"), estadisticasJ2 = EstiloGUI.boton("ESTADÍSTICAS");
    private final JLabel modoLabel = new JLabel();
    private final JComboBox<String> comboCancha = new JComboBox<>(TAMANOS);
    private final JSpinner spinnerObjetivo = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));

    /**
     * @param alJugar  acción al presionar JUGAR (quien la recibe lee la configuración con los getters)
     * @param alVolver acción al presionar VOLVER
     * @param alVerEstadisticas acción que recibe el número del jugador que quiere ver sus estadísticas
     */
    PantallaSeleccion(Runnable alJugar, Runnable alVolver, IntConsumer alVerEstadisticas) {
        super(new BorderLayout());
        estadisticasJ1.addActionListener(e -> alVerEstadisticas.accept(1));
        estadisticasJ2.addActionListener(e -> alVerEstadisticas.accept(2));
        JPanel p = EstiloGUI.fondo();
        p.setLayout(new BorderLayout(15, 15));
        p.add(EstiloGUI.titulo("ELIGE LOS PAÍSES Y CONFIGURA LA CANCHA", 26), BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 30, 0));
        centro.setOpaque(false);
        centro.add(crearSelectorJugador(1));
        centro.add(crearSelectorJugador(2));
        p.add(centro, BorderLayout.CENTER);

        JPanel sur = new JPanel();
        sur.setOpaque(false);
        sur.setLayout(new BoxLayout(sur, BoxLayout.Y_AXIS));
        sur.add(crearOpciones());
        sur.add(crearBotones(alJugar, alVolver));
        p.add(sur, BorderLayout.SOUTH);

        add(p, BorderLayout.CENTER);
        refrescarPaises();
    }

    // Construcción de la pantalla

    private JPanel crearOpciones() {
        JPanel opciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        opciones.setOpaque(false);
        JLabel canchaLabel = new JLabel("Tamaño de cancha:");
        canchaLabel.setForeground(Color.WHITE);
        canchaLabel.setFont(new Font("Arial", Font.BOLD, 16));
        comboCancha.setSelectedIndex(1);
        // Esta etiqueta cambia según el tipo de partido (duración o goles), ver prepararPara().
        modoLabel.setForeground(Color.WHITE);
        modoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        opciones.add(canchaLabel);
        opciones.add(comboCancha);
        opciones.add(modoLabel);
        opciones.add(spinnerObjetivo);
        return opciones;
    }

    private JPanel crearBotones(Runnable alJugar, Runnable alVolver) {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        botones.setOpaque(false);
        JButton atras = EstiloGUI.boton("VOLVER");
        atras.addActionListener(e -> alVolver.run());
        botones.add(atras);
        JButton iniciar = EstiloGUI.boton("JUGAR");
        iniciar.addActionListener(e -> alJugar.run());
        botones.add(iniciar);
        return botones;
    }

    private JPanel crearSelectorJugador(int jugador) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(255, 255, 255, 225));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 4),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.insets = new Insets(8, 8, 18, 8);
        p.add(jugador == 1 ? tituloJ1 : tituloJ2, c);
        c.gridy++;
        c.gridwidth = 1;

        JButton izq = new JButton("<<");
        izq.setFont(new Font("Arial", Font.BOLD, 16));
        izq.setPreferredSize(new Dimension(54, 42));
        c.gridx = 0;
        p.add(izq, c);

        JLabel pais = new JLabel("", SwingConstants.CENTER);
        pais.setFont(new Font("Arial", Font.BOLD, 18));
        pais.setOpaque(true);
        pais.setBackground(Color.WHITE);
        pais.setPreferredSize(new Dimension(ANCHO_TARJETA, ALTO_TARJETA));
        pais.setMinimumSize(new Dimension(ANCHO_TARJETA, ALTO_TARJETA));
        c.gridx = 1;
        p.add(pais, c);

        JButton der = new JButton(">>");
        der.setFont(new Font("Arial", Font.BOLD, 16));
        der.setPreferredSize(new Dimension(54, 42));
        c.gridx = 2;
        p.add(der, c);

        JButton estadisticas = jugador == 1 ? estadisticasJ1 : estadisticasJ2;
        estadisticas.setFont(new Font("Arial", Font.BOLD, 15));
        estadisticas.setPreferredSize(new Dimension(170, 38));
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        p.add(estadisticas, c);

        // "<<" retrocede y ">>" avanza por la lista completa de países.
        izq.addActionListener(e -> cambiar(jugador, -1));
        der.addActionListener(e -> cambiar(jugador, 1));
        if (jugador == 1) paisJ1 = pais;
        else paisJ2 = pais;
        return p;
    }

    // Selección de países

    /** Mueve la selección del jugador; floorMod hace que la lista dé la vuelta en ambos sentidos. */
    private void cambiar(int jugador, int paso) {
        int n = paises.size();
        if (jugador == 1) indice1 = Math.floorMod(indice1 + paso, n);
        else indice2 = Math.floorMod(indice2 + paso, n);
        refrescarPaises();
    }

    private void refrescarPaises() {
        mostrarPais(paisJ1, paises.get(indice1));
        mostrarPais(paisJ2, paises.get(indice2));
    }

    /** Muestra la imagen del país si existe; si no, solo su nombre. */
    private void mostrarPais(JLabel tarjeta, String pais) {
        ImageIcon imagen = imagenDe(pais);
        if (imagen != null) {
            // La imagen ya trae escrito el nombre del país, así que no se repite el texto.
            tarjeta.setIcon(imagen);
            tarjeta.setText(null);
            tarjeta.setToolTipText(pais);
        } else {
            tarjeta.setIcon(null);
            tarjeta.setText(pais);
            tarjeta.setToolTipText(null);
        }
    }

    /** Imagen del país escalada al tamaño de la tarjeta, o null si el país no tiene imagen o el archivo no está. */
    private ImageIcon imagenDe(String pais) {
        String ruta = IMAGENES_PAISES.get(pais);
        if (ruta == null) return null;
        return imagenesEscaladas.computeIfAbsent(pais, p -> {
            Image original = EstiloGUI.cargarImagen(ruta);
            if (original == null) return null;
            return new ImageIcon(original.getScaledInstance(ANCHO_TARJETA, ALTO_TARJETA, Image.SCALE_SMOOTH));
        });
    }

    // Lo que usa la ventana principal

    /** Ajusta la pantalla al tipo de partido elegido: "Tiempo" o "Goles". */
    void prepararPara(String tipoPartido) {
        refrescarPaises();
        boolean porTiempo = tipoPartido.equals("Tiempo");
        modoLabel.setText(porTiempo ? "Duración (minutos):" : "Goles para ganar:");
        // Valores por defecto: 2 minutos o 5 goles, según el tipo de partido.
        spinnerObjetivo.setModel(new SpinnerNumberModel(porTiempo ? 2 : 5, 1, 10, 1));
    }

    /**
     * Muestra contra quién se juega: otra persona si el perfil es null, o la máquina con ese perfil.
     * La máquina no gana puntos, así que no tiene estadísticas para mejorar.
     */
    void mostrarRival(String perfilMaquina) {
        tituloJ2.setText(perfilMaquina == null ? "JUGADOR 2" : "MÁQUINA " + perfilMaquina.toUpperCase());
        estadisticasJ2.setVisible(perfilMaquina == null);
    }

    String getPais1() {
        return paises.get(indice1);
    }

    String getPais2() {
        return paises.get(indice2);
    }

    /** Minutos de duración o goles para ganar, según el tipo de partido. */
    int getObjetivo() {
        return ((Number) spinnerObjetivo.getValue()).intValue();
    }

    int getAnchoCancha() {
        return ANCHOS[comboCancha.getSelectedIndex()];
    }

    int getAltoCancha() {
        return ALTOS[comboCancha.getSelectedIndex()];
    }
}
