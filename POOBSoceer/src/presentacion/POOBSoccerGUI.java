package presentacion;

import Domain.*;
import persistencia.ArchivoPartida;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Ventana del juego. El recorrido es: inicio, modo de juego, máquina (solo si se juega contra ella),
 * tipo de partido, selección de países y partido. Desde la selección también se llega a las estadísticas
 * y con el menú Archivo se guardan y se abren las partidas.
 */
public class POOBSoccerGUI extends JFrame {

    private final POOBSoccer dominio = new POOBSoccer();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private PantallaJuego pantallaJuego;
    private Timer loop;
    private String tipoPartido = "Tiempo";
    // Perfil de la máquina elegida, o null si juegan dos personas.
    private String perfilMaquina;
    private int anchoCancha = 1000, altoCancha = 650;
    private PantallaSeleccion seleccion;
    private final PantallaEstadisticas estadisticas;
    private final ControladorTeclado teclado;
    private final Dialogos dialogos;

    public POOBSoccerGUI() {
        this(new DialogosSwing());
    }

    /** Recibe los diálogos desde afuera para poder probar la ventana sin que se queden esperando al usuario. */
    POOBSoccerGUI(Dialogos dialogos) {
        super("DOPOSoccer");
        this.dialogos = dialogos;
        setJMenuBar(new MenuArchivo(this::guardarPartida, this::abrirPartida));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        root.add(new PantallaInicio(() -> cards.show(root, "modalidad"), this::dispose), "inicio");
        root.add(new PantallaModalidad(() -> elegirRival(null), () -> cards.show(root, "maquina"),
                () -> cards.show(root, "inicio")), "modalidad");
        root.add(new PantallaMaquina(this::elegirRival, () -> cards.show(root, "modalidad")), "maquina");
        root.add(new PantallaTipos(this::elegirTipo, this::volverDeTipos), "tipos");
        seleccion = new PantallaSeleccion(this::iniciarPartida, () -> cards.show(root, "tipos"), this::verEstadisticas);
        root.add(seleccion, "seleccion");
        estadisticas = new PantallaEstadisticas(() -> cards.show(root, "seleccion"));
        root.add(estadisticas, "estadisticas");
        setContentPane(root);
        // Tamaño inicial suficiente para que las tarjetas de los países se vean completas.
        setSize(1000, 650);
        setLocationRelativeTo(null);
        teclado = new ControladorTeclado(dominio, root, this::repintarJuego);
        // Si la ventana pierde el foco (por ejemplo con Alt+Tab) no llega el "soltar" de las teclas;
        // se olvidan para que ningún jugador siga moviéndose solo.
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                teclado.reiniciar();
            }
        });
        // Bucle del juego: ~60 cuadros por segundo.
        loop = new Timer(16, e -> {
            Partido actual = dominio.getPartidoActual();
            if (actual != null && !dominio.isPausa() && !actual.isTerminado()) {
                teclado.procesarTeclasContinuas();
                dominio.actualizar();
            }
            repintarJuego();
        });
    }

    private void repintarJuego() {
        if (pantallaJuego != null) pantallaJuego.repintar();
    }

    // Pantallas

    /** Guarda contra quién se juega: null es otra persona y un perfil es la máquina. */
    private void elegirRival(String perfil) {
        perfilMaquina = perfil;
        seleccion.mostrarRival(perfil);
        cards.show(root, "tipos");
    }

    private void volverDeTipos() {
        cards.show(root, perfilMaquina == null ? "modalidad" : "maquina");
    }

    /** Se llama desde PantallaTipos con "Tiempo" o "Goles". */
    private void elegirTipo(String tipo) {
        tipoPartido = tipo;
        seleccion.prepararPara(tipo);
        cards.show(root, "seleccion");
    }

    /** Abre las estadísticas del país que ese jugador tiene elegido en este momento. */
    private void verEstadisticas(int jugador) {
        String pais = jugador == 1 ? seleccion.getPais1() : seleccion.getPais2();
        PerfilJugador perfil = dominio.getPerfil(jugador);
        estadisticas.mostrar("JUGADOR " + jugador, perfil, pais);
        cards.show(root, "estadisticas");
    }

    // Inicio del juego

    /** Se llama desde PantallaSeleccion al presionar JUGAR. */
    private void iniciarPartida() {
        anchoCancha = seleccion.getAnchoCancha();
        altoCancha = seleccion.getAltoCancha();
        int objetivo = seleccion.getObjetivo();
        // El catálogo decide qué poder le toca a cada país y el perfil, sus estadísticas. La máquina juega sin mejoras.
        Personaje p1 = CatalogoPersonajes.crear(seleccion.getPais1(), dominio.getPerfil(1));
        Personaje p2 = CatalogoPersonajes.crear(seleccion.getPais2(), perfilMaquina == null ? dominio.getPerfil(2) : null);
        try {
            String modalidad = perfilMaquina == null ? POOBSoccer.JUGADOR_VS_JUGADOR : POOBSoccer.JUGADOR_VS_MAQUINA;
            dominio.iniciarPartido(tipoPartido, modalidad, p1, p2, objetivo, anchoCancha, altoCancha, perfilMaquina);
            mostrarJuego();
        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    // Archivos

    /**
     * Guarda el partido si hay uno en curso; si no, solo los puntos y las estadísticas.
     * El juego se detiene mientras se elige el archivo y luego sigue como estaba.
     */
    private void guardarPartida(ArchivoPartida formato) {
        boolean estabaEnPausa = dominio.isPausa();
        dominio.pausarJuego();
        try {
            File archivo = dialogos.paraGuardar(this, formato);
            if (archivo != null) {
                formato.guardar(dominio, archivo);
                String guardado = dominio.hayPartidoEnCurso() ? "La partida se guardó en " : "Los puntos y las estadísticas se guardaron en ";
                dialogos.informar(this, guardado + archivo.getName());
            }
        } catch (ExcepcionJuego ex) {
            mostrarError(ex);
        }
        if (!estabaEnPausa) dominio.reanudarJuego();
    }

    /** Si el archivo no sirve, se sigue con lo que se estaba jugando. */
    private void abrirPartida(ArchivoPartida formato) {
        boolean estabaEnPausa = dominio.isPausa();
        dominio.pausarJuego();
        File archivo = dialogos.paraAbrir(this, formato);
        if (archivo != null) {
            try {
                formato.abrir(dominio, archivo);
                if (dominio.hayPartidoEnCurso()) mostrarPartidaAbierta();
                else mostrarProgresoAbierto(archivo);
                return;
            } catch (ExcepcionJuego ex) {
                mostrarError(ex);
            }
        }
        if (!estabaEnPausa) dominio.reanudarJuego();
    }

    /** Deja la ventana como si ese partido se hubiera configurado a mano. Queda en pausa hasta que lo reanuden. */
    private void mostrarPartidaAbierta() {
        Partido partido = dominio.getPartidoActual();
        perfilMaquina = partido.getJugador2().getPerfilMaquina();
        tipoPartido = dominio.getTipoPartido();
        anchoCancha = partido.getCancha().getAncho();
        altoCancha = partido.getCancha().getAlto();
        seleccion.mostrarRival(perfilMaquina);
        seleccion.prepararPara(tipoPartido);
        mostrarJuego();
    }

    /** El archivo no traía partido: se retira la pantalla del que hubiera y se vuelve al inicio. */
    private void mostrarProgresoAbierto(File archivo) {
        if (pantallaJuego != null) root.remove(pantallaJuego);
        pantallaJuego = null;
        volverAlMenu();
        dialogos.informar(this, "Se recuperaron los puntos y las estadísticas de " + archivo.getName());
    }

    /** El usuario ve un mensaje claro y el detalle queda en el registro para los programadores. */
    private void mostrarError(RuntimeException error) {
        RegistroErrores.registrar(error.getMessage(), error);
        dialogos.error(this, error.getMessage());
    }

    private void mostrarJuego() {
        loop.stop();
        teclado.reiniciar();
        // Si ya había una partida anterior, se retira su pantalla antes de agregar la nueva.
        if (pantallaJuego != null) root.remove(pantallaJuego);
        pantallaJuego = new PantallaJuego(dominio, this::volverAlMenu);
        root.add(pantallaJuego, "juego");
        cards.show(root, "juego");
        setSize(anchoCancha, altoCancha + 95 + getJMenuBar().getPreferredSize().height);
        setLocationRelativeTo(null);
        pantallaJuego.enfocar();
        loop.start();
    }

    /** Se llama desde PantallaJuego al presionar MENÚ. */
    private void volverAlMenu() {
        loop.stop();
        cards.show(root, "inicio");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new POOBSoccerGUI().setVisible(true));
    }
}