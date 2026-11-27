package Domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase principal de la capa de dominio (Fachada).
 * Administra el estado global del juego usando mapas para inicializar partidos
 * garantizando la extensibilidad sin cascadas de condicionales.
 */
public class POOBSoccer {

    /** Modalidades que se pueden elegir. */
    public static final String JUGADOR_VS_JUGADOR = "PvsP", JUGADOR_VS_MAQUINA = "PvsM";

    // Se escribe en lugar del tipo cuando no hay un partido para guardar.
    private static final String SIN_PARTIDO = "";

    // Cancha que se usa cuando no se elige ninguna.
    private static final int ANCHO_NORMAL = 1000, ALTO_NORMAL = 650;

    // Teclas de cada jugador, en orden: [Izquierda, Derecha, Saltar, Patear, Poder].
    // Jugador 1 usa S para patear y Q para preparar el poder.
    // Jugador 2 usa M para patear y N para preparar el poder; estos nombres
    // deben coincidir con los que envía presentacion.ControladorTeclado.
    private static final String[] TECLAS_J1 = {"A", "D", "W", "S", "Q"};
    private static final String[] TECLAS_J2 = {"LEFT", "RIGHT", "UP", "M", "N"};

    private Partido partidoActual;
    private boolean pausa;

    // Con qué opciones se creó el partido actual; se necesitan para poder guardarlo.
    private String tipoActual, modalidadActual;
    private int objetivoActual;

    // Progreso de las dos personas; se conserva entre un partido y otro mientras el juego esté abierto.
    private final PerfilJugador perfil1, perfil2;

    // Interfaces funcionales para el patrón Factory extensible
    private interface ModalidadFactory {
        Jugador[] crearJugadores(Personaje p1, Personaje p2, String perfilMaquina);
    }

    private interface PartidoFactory {
        Partido crearPartido(Jugador j1, Jugador j2, int objetivo, int ancho, int alto);
    }

    // Mapas de configuración
    private Map<String, ModalidadFactory> mapaModalidades;
    private Map<String, PartidoFactory> mapaTiposPartido;

    public POOBSoccer() {
        this(new PerfilJugador("jugador1", "Jugador 1"), new PerfilJugador("jugador2", "Jugador 2"));
    }

    private POOBSoccer(PerfilJugador perfil1, PerfilJugador perfil2) {
        this.perfil1 = perfil1;
        this.perfil2 = perfil2;
        this.partidoActual = null;
        this.pausa = false;

        this.mapaModalidades = new HashMap<>();
        this.mapaTiposPartido = new HashMap<>();

        configurarFabricas();
    }

    /**
     * Registra las estrategias de creación en los mapas.
     * Si a futuro se agrega una nueva modalidad, solo se añade una línea aquí.
     */
    private void configurarFabricas() {
        // Fábricas de Modalidades
        mapaModalidades.put(JUGADOR_VS_JUGADOR, (p1, p2, perfil) -> new Jugador[]{
            new JugadorHumano(p1.getPais(), p1, TECLAS_J1, perfil1),
            new JugadorHumano(p2.getPais(), p2, TECLAS_J2, perfil2)
        });
        mapaModalidades.put(JUGADOR_VS_MAQUINA, (p1, p2, perfil) -> new Jugador[]{
            new JugadorHumano(p1.getPais(), p1, TECLAS_J1, perfil1),
            new JugadorMaquina(p2.getPais(), p2, perfil)
        });

        // Fábricas de Tipos de Partido
        mapaTiposPartido.put("Tiempo", PartidoPorTiempo::new);
        mapaTiposPartido.put("Goles", PartidoPorGoles::new);
    }

    /**
     * Inicia un nuevo partido recuperando la configuración de los mapas (O(1)).
     */
    public void iniciarPartido(String tipo, String modalidad, Personaje p1, Personaje p2) {
        iniciarPartido(tipo, modalidad, p1, p2, 5);
    }

    public void iniciarPartido(String tipo, String modalidad, Personaje p1, Personaje p2, int objetivo) {
        iniciarPartido(tipo, modalidad, p1, p2, objetivo, ANCHO_NORMAL, ALTO_NORMAL);
    }

    public void iniciarPartido(String tipo, String modalidad, Personaje p1, Personaje p2, int objetivo, int ancho, int alto) {
        iniciarPartido(tipo, modalidad, p1, p2, objetivo, ancho, alto, null);
    }

    /**
     * Inicia el partido con todas las opciones. El perfil solo se usa cuando se juega contra la máquina.
     */
    public void iniciarPartido(String tipo, String modalidad, Personaje p1, Personaje p2, int objetivo,
                               int ancho, int alto, String perfilMaquina) {
        if (p1 == null || p2 == null) throw new ExcepcionJuego("Debe seleccionar ambos personajes");

        ModalidadFactory modalidadElegida = mapaModalidades.get(modalidad);
        if (modalidadElegida == null) throw new ExcepcionJuego("Modalidad no válida: " + modalidad);

        PartidoFactory tipoElegido = mapaTiposPartido.get(tipo);
        if (tipoElegido == null) throw new ExcepcionJuego("Tipo de partido no válido: " + tipo);

        Jugador[] jugadores = modalidadElegida.crearJugadores(p1, p2, perfilMaquina);
        partidoActual = tipoElegido.crearPartido(jugadores[0], jugadores[1], objetivo, ancho, alto);
        tipoActual = tipo;
        modalidadActual = modalidad;
        objetivoActual = objetivo;
        pausa = false;
        RegistroErrores.anotar("Empieza un partido por " + tipo + " (" + modalidad + "): "
                + p1.getPais() + " contra " + p2.getPais() + ", objetivo " + objetivo);
    }

    /** Indica si hay un partido que todavía no ha terminado. */
    public boolean hayPartidoEnCurso() {
        return partidoActual != null && !partidoActual.isTerminado();
    }

    /**
     * Guarda los puntos y las estadísticas de las dos personas y, si hay un partido en curso,
     * también el partido tal como está en este momento.
     */
    public void guardar(EscritorPartida e) {
        perfil1.guardar(e);
        perfil2.guardar(e);

        if (hayPartidoEnCurso()) guardarPartido(e);
        else e.texto(SIN_PARTIDO);
    }

    private void guardarPartido(EscritorPartida e) {
        String maquina = partidoActual.getJugador2().getPerfilMaquina();
        e.texto(tipoActual);
        e.texto(modalidadActual);
        e.texto(maquina == null ? "" : maquina);
        e.entero(objetivoActual, 16);
        e.entero(partidoActual.getCancha().getAncho(), 16);
        e.entero(partidoActual.getCancha().getAlto(), 16);
        guardarPersonaje(e, partidoActual.getJugador1().getPersonaje());
        guardarPersonaje(e, partidoActual.getJugador2().getPersonaje());
        partidoActual.guardar(e);
    }

    /** El país y las estadísticas con las que está jugando. */
    private static void guardarPersonaje(EscritorPartida e, Personaje personaje) {
        EstadisticasPersonaje niveles = new EstadisticasPersonaje();
        niveles.setNivel(PerfilJugador.Estadistica.VIDA, personaje.getSurvival());
        niveles.setNivel(PerfilJugador.Estadistica.SPEED, personaje.getVelocidad());
        niveles.setNivel(PerfilJugador.Estadistica.JUMP, personaje.getSalto());
        niveles.setNivel(PerfilJugador.Estadistica.KICK, personaje.getKick());
        e.texto(personaje.getPais());
        niveles.guardar(e);
    }

    /**
     * Recupera lo que se guardó. Si traía un partido lo deja en pausa y, si solo traía los puntos y las
     * estadísticas, queda sin partido. Primero lo arma todo por aparte; si algún dato no sirve,
     * lo que se estaba jugando queda intacto.
     */
    public void restaurar(LectorPartida l) {
        PerfilJugador leido1 = PerfilJugador.leer(l, perfil1.getIdentificador());
        PerfilJugador leido2 = PerfilJugador.leer(l, perfil2.getIdentificador());

        POOBSoccer leida = new POOBSoccer(perfil1, perfil2);
        String tipo = l.texto();
        if (!tipo.equals(SIN_PARTIDO)) leida.leerPartido(l, tipo);
        l.terminar();

        perfil1.copiarDe(leido1);
        perfil2.copiarDe(leido2);
        partidoActual = leida.partidoActual;
        tipoActual = leida.tipoActual;
        modalidadActual = leida.modalidadActual;
        objetivoActual = leida.objetivoActual;
        pausa = partidoActual != null;
        RegistroErrores.anotar(partidoActual == null
                ? "Se recuperaron los puntos y las estadísticas guardadas"
                : "Se recuperó un partido guardado; queda en pausa");
    }

    private void leerPartido(LectorPartida l, String tipo) {
        String modalidad = l.texto(), maquina = l.texto();
        int objetivo = l.entero(16), ancho = l.entero(16), alto = l.entero(16);
        Personaje p1 = leerPersonaje(l), p2 = leerPersonaje(l);
        iniciarPartido(tipo, modalidad, p1, p2, objetivo, ancho, alto, maquina.isEmpty() ? null : maquina);
        partidoActual.restaurar(l);
    }

    private static Personaje leerPersonaje(LectorPartida l) {
        String pais = l.texto();
        return CatalogoPersonajes.crearConEstadisticas(pais, EstadisticasPersonaje.leer(l));
    }

    public void pausarJuego() {
        if (this.partidoActual != null && !this.partidoActual.esFinDePartido()) {
            this.pausa = true;
            RegistroErrores.anotar("El partido se pausa");
        }
    }

    public void reanudarJuego() {
        if (this.pausa) {
            this.pausa = false;
            RegistroErrores.anotar("El partido continúa");
        }
    }

    public void actualizar() {
        if (this.partidoActual != null && !this.pausa && !this.partidoActual.esFinDePartido()) {
            this.partidoActual.actualizarEstado();
        }
    }

    // Getters con estructura estricta

    public Partido getPartidoActual() {
        return this.partidoActual;
    }

    /** Progreso del jugador 1 o del jugador 2. */
    public PerfilJugador getPerfil(int jugador) {
        return jugador == 1 ? perfil1 : perfil2;
    }

    /** Tipo del partido actual: "Tiempo" o "Goles". */
    public String getTipoPartido() {
        return tipoActual;
    }

    public boolean isPausa() {
        return this.pausa;
    }
}