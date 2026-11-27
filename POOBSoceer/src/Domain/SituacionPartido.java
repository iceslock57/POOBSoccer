package Domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que una máquina ve del partido en un momento dado, contado desde su lado de la cancha.
 * Solo usa información que también está a la vista de una persona.
 */
public class SituacionPartido {

    private static final double MARGEN_PATADA = 12, ALTURA_DE_SALTO = 150, DISTANCIA_TIRO_ENCIMA = 200;
    private static final double DISTANCIA_CERCA = 170;
    private static final double CERCA_DEL_DESTINO = 6, VIDA_BAJA = .35;

    private final Jugador yo, rival;
    private final Partido partido;
    private final Balon balon;
    private final int direccion;

    public SituacionPartido(Jugador yo, Partido partido) {
        this.yo = yo;
        this.partido = partido;
        boolean soyElPrimero = partido.getJugador1() == yo;
        rival = soyElPrimero ? partido.getJugador2() : partido.getJugador1();
        direccion = soyElPrimero ? 1 : -1;
        balon = balonMasCercano();
    }

    /** Si hay balones falsos no sabe cuál es el real, así que sigue el que tenga más cerca. */
    private Balon balonMasCercano() {
        Balon elegido = partido.getBalon();
        for (Balon falso : partido.getBalonesFalsos()) {
            if (distanciaA(falso) < distanciaA(elegido)) elegido = falso;
        }
        return elegido;
    }

    private double distanciaA(Balon b) {
        return Math.hypot(b.getPosX() - miCentro(), b.getPosY() - (yo.getPersonaje().getPosY() + Personaje.ALTO / 2.0));
    }

    private double miCentro() {
        return yo.getPersonaje().getPosX() + Personaje.ANCHO / 2.0;
    }

    /** Es 1 si ataca hacia la derecha y -1 si ataca hacia la izquierda. */
    public int direccionDeAtaque() {
        return direccion;
    }

    /** Paso que la acerca a ese punto, o ninguno si ya está ahí. */
    public Set<Accion> caminarHacia(double x) {
        Set<Accion> pasos = EnumSet.noneOf(Accion.class);
        double falta = x - miCentro();
        if (falta > CERCA_DEL_DESTINO) pasos.add(Accion.DERECHA);
        if (falta < -CERCA_DEL_DESTINO) pasos.add(Accion.IZQUIERDA);
        return pasos;
    }

    /** Lugar donde estaría el balón si sigue igual durante esos cuadros. */
    public double balonXDentroDe(int cuadros) {
        double x = balon.getPosX() + balon.getVelX() * cuadros;
        return Math.max(0, Math.min(partido.getCancha().getAncho(), x));
    }

    /**
     * Lugar donde pararse para que el balón quede delante del pie. Se ubica del lado de su propio arco,
     * así la patada lo manda hacia el arco contrario y no al suyo.
     */
    public double lugarParaPatear(int cuadros) {
        return balonXDentroDe(cuadros) - direccion * (Personaje.ANCHO / 2.0);
    }

    /** El balón está delante y lo bastante cerca para pegarle con la patada; el pie no alcanza lo de atrás. */
    public boolean balonAlAlcance() {
        Personaje p = yo.getPersonaje();
        double alcance = balon.getRadio() + MARGEN_PATADA;
        double delante = (balon.getPosX() - miCentro()) * direccion;
        return delante >= -balon.getRadio() && delante <= Personaje.ANCHO / 2.0 + alcance
                && balon.getPosY() >= p.getPosY() - alcance
                && balon.getPosY() <= p.getPosY() + Personaje.ALTO + alcance;
    }

    /** El balón está en la zona donde se lo disputa con el rival. */
    public boolean balonCerca() {
        return distanciaA(balon) <= DISTANCIA_CERCA;
    }

    /** El balón pasa por encima de la cabeza, a una altura que alcanza con un salto. */
    public boolean balonPorEncima() {
        double altura = yo.getPersonaje().getPosY() - (balon.getPosY() + balon.getRadio());
        double alLado = Math.abs(balon.getPosX() - miCentro());
        return alLado <= Personaje.ANCHO && altura > 0 && altura <= ALTURA_DE_SALTO;
    }

    /** Un balón justo en el centro también cuenta, para que alguien vaya por él. */
    public boolean balonEnMiMitad() {
        double mitad = partido.getCancha().getAncho() / 2.0;
        return (balon.getPosX() - mitad) * direccion <= 0;
    }

    public boolean balonVaHaciaMiArco() {
        return balon.getVelX() * direccion < 0;
    }

    /** Punto justo delante de su arco. */
    public double puestoDeGuardia() {
        Cancha cancha = partido.getCancha();
        double bocaDelArco = direccion > 0
                ? cancha.getArcoIzquierdo().getPosX() + cancha.getArcoIzquierdo().getAncho()
                : cancha.getArcoDerecho().getPosX();
        return bocaDelArco + direccion * Personaje.ANCHO;
    }

    public boolean rivalAlAlcance() {
        return yo.getPersonaje().alcanzaConPatada(rival.getPersonaje(), direccion);
    }

    /** El rival no puede moverse ni patear en este momento. */
    public boolean rivalIndefenso() {
        Personaje p = rival.getPersonaje();
        return p.isAturdido() || p.isCongelado() || p.isInconsciente();
    }

    /** Un tiro de poder del rival viene hacia ella y ya está cerca. */
    public boolean tiroEnemigoEncima() {
        int numeroDelRival = direccion > 0 ? 2 : 1;
        return balon.isDisparoEspecial()
                && balon.getPropietarioDisparo() == numeroDelRival
                && Math.abs(balon.getPosX() - miCentro()) <= DISTANCIA_TIRO_ENCIMA;
    }

    public boolean poderListo() {
        return yo.getBarraPoder() >= 100 && !yo.tienePoderPreparado();
    }

    public boolean vidaBaja() {
        return yo.getPersonaje().getVidaActual() <= VIDA_BAJA * yo.getPersonaje().getVidaMaxima();
    }

    public boolean voyGanando() {
        return yo.getGolesAnotados() > rival.getGolesAnotados();
    }

    public boolean estaPorTerminar() {
        return partido.estaPorTerminar();
    }
}
