package Domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Jugador que controla el computador. Su estrategia le dice qué hacer y él lo hace en la cancha. */
public class JugadorMaquina extends Jugador {

    // Medio segundo entre una patada y otra, para que no golpee sin parar.
    private static final int DESCANSO_ENTRE_PATADAS = 30;
    private static final Set<Accion> PASOS = EnumSet.of(Accion.IZQUIERDA, Accion.DERECHA);

    private final String perfilMaquina;
    private final EstrategiaMaquina estrategia;
    private final Map<Accion, Consumer<Partido>> movimientos = new EnumMap<>(Accion.class);

    private Set<Accion> decision = EnumSet.noneOf(Accion.class);
    private int cuadrosParaDecidir, descansoPatada;

    /** Crea la máquina con uno de los perfiles del catálogo. */
    public JugadorMaquina(String equipo, Personaje personaje, String perfilMaquina) {
        this(equipo, personaje, perfilMaquina, CatalogoMaquinas.crear(perfilMaquina));
    }

    public JugadorMaquina(String equipo, Personaje personaje, String perfilMaquina, EstrategiaMaquina estrategia) {
        super(equipo, personaje);
        this.perfilMaquina = perfilMaquina;
        this.estrategia = estrategia;

        movimientos.put(Accion.IZQUIERDA, partido -> personaje.mover(-1, rivalEn(partido)));
        movimientos.put(Accion.DERECHA, partido -> personaje.mover(1, rivalEn(partido)));
        movimientos.put(Accion.SALTAR, partido -> personaje.saltar());
        movimientos.put(Accion.PATEAR, this::patear);
        movimientos.put(Accion.PODER, partido -> activarPoder());
    }

    /**
     * Juega un cuadro. Cada cierto tiempo toma una decisión nueva; mientras tanto sigue caminando
     * hacia donde iba, como si mantuviera la tecla presionada.
     */
    @Override
    public void jugarTurno(Partido partido) {
        if (descansoPatada > 0) descansoPatada--;

        if (cuadrosParaDecidir > 0) {
            cuadrosParaDecidir--;
            for (Accion paso : PASOS) {
                if (decision.contains(paso)) hacer(paso, partido);
            }
            return;
        }

        decision = estrategia.decidir(new SituacionPartido(this, partido));
        cuadrosParaDecidir = estrategia.cuadrosEntreDecisiones() - 1;
        for (Accion accion : decision) hacer(accion, partido);
    }

    /** Hace el movimiento que tenga ese nombre. Las teclas de una persona no la afectan. */
    @Override
    public void realizarAccion(String accion, Partido partido) {
        for (Accion posible : Accion.values()) {
            if (posible.name().equals(accion)) hacer(posible, partido);
        }
    }

    private void hacer(Accion accion, Partido partido) {
        movimientos.get(accion).accept(partido);
    }

    private void patear(Partido partido) {
        if (descansoPatada > 0) return;

        descansoPatada = DESCANSO_ENTRE_PATADAS;
        personaje.patear();
        partido.ejecutarPatada(this);
    }

    private Personaje rivalEn(Partido partido) {
        Jugador rival = partido.getJugador1() == this ? partido.getJugador2() : partido.getJugador1();
        return rival.getPersonaje();
    }

    /** También guarda lo que estaba haciendo, para que siga igual al abrir la partida. */
    @Override
    void guardar(EscritorPartida e) {
        super.guardar(e);
        e.entero(cuadrosParaDecidir, 8);
        e.entero(descansoPatada, 8);
        for (Accion accion : Accion.values()) e.logico(decision.contains(accion));
    }

    @Override
    void restaurar(LectorPartida l) {
        super.restaurar(l);
        cuadrosParaDecidir = l.entero(8);
        descansoPatada = l.entero(8);
        decision = EnumSet.noneOf(Accion.class);
        for (Accion accion : Accion.values()) {
            if (l.logico()) decision.add(accion);
        }
    }

    @Override
    public String getPerfilMaquina() {
        return perfilMaquina;
    }

    @Override
    public String getNombreVisible() {
        return super.getNombreVisible() + " (Máquina " + perfilMaquina + ")";
    }
}
