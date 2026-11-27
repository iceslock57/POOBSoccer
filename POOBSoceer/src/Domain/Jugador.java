package Domain;

/**
 * Mantiene el estado competitivo del participante durante un partido y lo conecta
 * con su PerfilJugador para que los puntos ganados queden asociados a su progreso.
 */
public abstract class Jugador {

    protected String equipo;
    protected Personaje personaje;
    protected int golesAnotados, barraPoder;
    protected long puntos;
    protected String poderActivo;

    protected final int MAX_PODER = 100;

    private int framesCarga;
    private final PerfilJugador perfil;

    /** Constructor compatible con las pruebas y partidos que no utilizan perfiles guardados. */
    public Jugador(String equipo, Personaje personaje) {
        this(equipo, personaje, null);
    }

    /** Crea el jugador del partido utilizando el perfil permanente que seleccionó la persona. */
    public Jugador(String equipo, Personaje personaje, PerfilJugador perfil) {
        this.equipo = equipo;
        this.personaje = personaje;
        this.perfil = perfil;
        golesAnotados = 0;
        barraPoder = 0;
        puntos = perfil == null ? 0 : perfil.getPuntos();
        poderActivo = "Ninguno";
    }

    public abstract void realizarAccion(String accion, Partido partido);

    /** El partido lo llama en cada cuadro. Una persona juega con el teclado, así que aquí no hace nada. */
    public void jugarTurno(Partido partido) {
    }

    /** Nombre con el que aparece en la pantalla del partido. */
    public String getNombreVisible() {
        return personaje.getPais();
    }

    public void cargarPoder(int cantidad) {
        barraPoder = Math.min(MAX_PODER, Math.max(0, barraPoder + cantidad));
    }

    public void actualizarCarga() {
        avanzarCarga();
    }

    public void avanzarCarga() {
        framesCarga++;

        if (framesCarga >= 6) {
            cargarPoder(1);
            framesCarga = 0;
        }
    }

    public boolean activarPoder() {
        if (barraPoder < 100 || !"Ninguno".equals(poderActivo)) return false;

        barraPoder = 0;
        poderActivo = "Preparado";

        return true;
    }

    public boolean tienePoderPreparado() {
        return "Preparado".equals(poderActivo);
    }

    public void consumirPoder() {
        poderActivo = "Ninguno";
    }

    public void anotarGol() {
        golesAnotados++;
    }

    /** Guarda lo que lleva en el partido. Lo del perfil se guarda aparte. */
    void guardar(EscritorPartida e) {
        e.entero(golesAnotados, 16);
        e.entero(barraPoder, 8);
        e.entero(framesCarga, 8);
        e.logico(tienePoderPreparado());
        personaje.guardar(e);
    }

    void restaurar(LectorPartida l) {
        golesAnotados = l.entero(16);
        barraPoder = Math.min(MAX_PODER, l.entero(8));
        framesCarga = l.entero(8);
        poderActivo = l.logico() ? "Preparado" : "Ninguno";
        personaje.restaurar(l);
    }

    /** Perfil de la máquina que lo controla, o null si lo controla una persona. */
    public String getPerfilMaquina() {
        return null;
    }

    public String getEquipo() {
        return equipo;
    }

    public Personaje getPersonaje() {
        return personaje;
    }

    public int getGolesAnotados() {
        return golesAnotados;
    }

    public void agregarPuntos(long cantidad) {
        long puntosGanados = Math.max(0L, cantidad);
        if (puntos <= Long.MAX_VALUE - puntosGanados) puntos += puntosGanados;
        // Cuando el jugador tiene perfil, el premio también se incorpora a su progreso permanente.
        if (perfil != null) perfil.agregarPuntos(puntosGanados);
    }

    /** Devuelve el saldo de puntos del perfil o, si no existe, el acumulado de esta partida. */
    public long getPuntos() {
        return perfil == null ? puntos : perfil.getPuntos();
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public int getBarraPoder() {
        return barraPoder;
    }

    public String getPoderActivo() {
        return poderActivo;
    }
}
