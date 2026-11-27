package Domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Motor de partido: actualiza física, colisiones, goles y recompensas. */
public abstract class Partido {

    private static final int ANCHO = Personaje.ANCHO, ALTO = Personaje.ALTO;
    private static final int CUADROS_POR_SEGUNDO = 60;
    private static final double VELOCIDAD_PATADA = 12, ELEVACION_PATADA = -6.5;

    protected Cancha cancha;
    protected Balon balon;
    protected Jugador jugador1, jugador2;
    protected boolean terminado;
    protected int[] marcador;
    private boolean premiosAsignados;

    // Balones que acompañan al real cuando se usa la ilusión: se ven iguales, pero no hacen gol ni daño.
    private final List<Balon> balonesFalsos = new ArrayList<>();
    private int framesBalonesFalsos;
    private Poder poderDeLosFalsos;
    private Personaje confundidoPorLosFalsos;

    public Partido(Jugador j1, Jugador j2) {
        this(j1, j2, 1000, 650);
    }

    public Partido(Jugador j1, Jugador j2, int ancho, int alto) {
        if (j1 == null || j2 == null) throw new IllegalArgumentException("Los dos jugadores son obligatorios");
        jugador1 = j1;
        jugador2 = j2;
        marcador = new int[]{0, 0};
        cancha = new Cancha(ancho, alto);
        balon = new Balon(ancho / 2, cancha.getSuelo() - 100, 16);
        // Cada personaje necesita conocer los límites de la cancha para moverse dentro de ella.
        jugador1.getPersonaje().configurarCancha(ancho, cancha.getSuelo());
        jugador2.getPersonaje().configurarCancha(ancho, cancha.getSuelo());
        reiniciarPosiciones();
    }

    /**
     * Registra la patada del personaje. La animación se inicia desde JugadorHumano;
     * el balón se impulsa en gestionarContacto cuando realmente alcanza al jugador.
     * Si el rival está delante y muy cerca, también recibe el golpe físico de la patada.
     */
    public void ejecutarPatada(Jugador atacante) {
        if (atacante == null) return;

        // Quien está aturdido o inconsciente no llega a patear, así que tampoco puede golpear.
        Personaje a = atacante.getPersonaje();
        Personaje d = rivalDe(atacante).getPersonaje();

        if (a.isPateando() && a.alcanzaConPatada(d, direccionDe(atacante)) && !d.aturdido()) {
            d.recibirDanio(10 * a.getMultiplicadorKick());
        }
    }

    /**
     * Actualiza el partido en cada cuadro: física, disparos, colisiones y goles.
     * La habilidad especial no se aplica al tocar al otro personaje; viaja dentro
     * del balón y produce su efecto cuando ese balón impacta al rival.
     */
    public void actualizarEstado() {
        if (terminado) return;

        // Primero avanzan los personajes y se acumula poco a poco la energía de sus poderes.
        jugador1.getPersonaje().actualizarEstado(jugador2.getPersonaje());
        jugador2.getPersonaje().actualizarEstado(jugador1.getPersonaje());
        jugador1.avanzarCarga();
        jugador2.avanzarCarga();

        // Las máquinas deciden su movimiento; las personas ya lo hicieron con el teclado.
        jugador1.jugarTurno(this);
        jugador2.jugarTurno(this);

        // Después se mueve el balón y se corrigen sus rebotes contra la cancha.
        moverBalon(balon);
        actualizarBalonesFalsos();

        // Un disparo especial puede aplicar el poder si alcanza al personaje contrario.
        detectarImpactoDisparoEspecial();

        // Las colisiones ordinarias determinan si el balón rebota o sale pateado.
        gestionarContacto(jugador1);
        gestionarContacto(jugador2);

        verificarGol();
        if (esFinDePartido()) {
            terminado = true;
            asignarPuntos();
        }
    }

    /**
     * Mueve un balón y deja que el poder ajuste su recorrido si es un tiro especial.
     * Al chocar con un lado de la cancha el tiro pierde su poder y el balón vuelve a ser normal.
     */
    private void moverBalon(Balon b) {
        double velAntes = b.getVelX();
        b.aplicarGravedad();
        b.mover();
        b.rebotar(cancha);

        if (!b.isDisparoEspecial()) return;

        if (velAntes * b.getVelX() < 0) {
            b.consumirDisparoEspecial();
            return;
        }
        Poder poder = getPoderDelDisparo(b);
        if (poder != null) poder.avanzarDisparo(b, cancha.getSuelo());
    }

    /** Mueve los balones falsos y los retira cuando se acaba su tiempo o termina la confusión del rival. */
    private void actualizarBalonesFalsos() {
        if (balonesFalsos.isEmpty()) return;

        boolean terminoLaConfusion = confundidoPorLosFalsos != null && !confundidoPorLosFalsos.isConfundido();
        if (--framesBalonesFalsos <= 0 || terminoLaConfusion) {
            quitarBalonesFalsos();
            return;
        }

        for (Balon falso : balonesFalsos) {
            moverBalon(falso);
            falso.rebotarEnLosLados(cancha.getAncho());
            tocarConBalonFalso(falso, jugador1);
            tocarConBalonFalso(falso, jugador2);
        }
    }

    private void quitarBalonesFalsos() {
        balonesFalsos.clear();
        framesBalonesFalsos = 0;
        confundidoPorLosFalsos = null;
    }

    /** Un balón falso se comporta como uno normal al tocar a alguien, para no delatarse. */
    private void tocarConBalonFalso(Balon falso, Jugador jugador) {
        if (esTiroPropio(falso, jugador)) return;

        Personaje personaje = jugador.getPersonaje();
        if (personaje.alcanzaConElPie(falso, direccionDe(jugador))) impulsarConPatada(falso, jugador);
        else if (tocaA(falso, personaje)) rebotarEnElCuerpo(falso, jugador);
    }

    /**
     * Revisa si el balón toca a uno de los jugadores.
     * Si el pie lo alcanza mientras patea, sale un disparo: normal cuando no preparó su poder
     * y especial cuando sí lo preparó. Si solo toca el cuerpo, rebota hacia el lado por donde llegó.
     */
    private void gestionarContacto(Jugador jugador) {
        Personaje personaje = jugador.getPersonaje();

        // Un tiro de poder no puede volver a golpear al mismo jugador que lo lanzó.
        if (esTiroPropio(balon, jugador)) return;

        if (personaje.alcanzaConElPie(balon, direccionDe(jugador))) {
            boolean poderPreparado = jugador.tienePoderPreparado();

            if (poderPreparado && personaje.getPoderEspecial() != null) lanzarPoder(jugador);
            else impulsarConPatada(balon, jugador);

            if (poderPreparado) jugador.consumirPoder();
            personaje.consumirPatada();
        } else if (!balon.isDisparoEspecial() && tocaA(balon, personaje)) {
            rebotarEnElCuerpo(balon, jugador);
        }
    }

    /** Cada poder decide cómo sale su tiro y si lo acompañan balones falsos. */
    private void lanzarPoder(Jugador jugador) {
        Poder poder = jugador.getPersonaje().getPoderEspecial();

        quitarBalonesFalsos();
        balonesFalsos.addAll(poder.lanzar(balon, direccionDe(jugador), numeroDe(jugador)));
        framesBalonesFalsos = duracionEnCuadros(poder);
        poderDeLosFalsos = poder;
    }

    private void impulsarConPatada(Balon b, Jugador jugador) {
        b.aplicarFuerza(direccionDe(jugador) * VELOCIDAD_PATADA, ELEVACION_PATADA);
    }

    /**
     * Si el balón llega sin una patada, rebota en la cabeza o sale por el costado que tocó:
     * un balón que está detrás del jugador se va hacia atrás, no hacia adelante.
     */
    private void rebotarEnElCuerpo(Balon b, Jugador jugador) {
        Personaje personaje = jugador.getPersonaje();
        if (b.caeSobre(personaje)) b.rebotarEnCabeza(personaje, direccionDe(jugador));
        else b.rebotarEnElCostado(personaje, direccionDe(jugador));
    }

    /**
     * Revisa si el tiro de poder ya llegó al rival. El poder de cada personaje
     * define el daño y el efecto; esta clase solo coordina el impacto.
     */
    private void detectarImpactoDisparoEspecial() {
        if (!balon.isDisparoEspecial()) return;

        Jugador atacante = duenoDelDisparo(balon);
        Personaje rival = rivalDe(atacante).getPersonaje();
        if (!tocaA(balon, rival)) return;

        Poder poder = atacante.getPersonaje().getPoderEspecial();
        if (poder != null) {
            poder.aplicarImpacto(rival, atacante.getPersonaje().getMultiplicadorKick(), direccionDe(atacante));
            mantenerBalonesFalsosMientrasDure(poder, rival);
        }

        // Al golpear, el balón deja de ser especial para que no afecte varias veces.
        balon.consumirDisparoEspecial();
    }

    /** Si el rival quedó confundido, los balones falsos permanecen hasta que se le pase. */
    private void mantenerBalonesFalsosMientrasDure(Poder poder, Personaje rival) {
        if (balonesFalsos.isEmpty() || !rival.isConfundido()) return;

        confundidoPorLosFalsos = rival;
        framesBalonesFalsos = duracionEnCuadros(poder) + 1;
    }

    private static int duracionEnCuadros(Poder poder) {
        return (int) Math.round(poder.duracionEfecto() * CUADROS_POR_SEGUNDO);
    }

    private static boolean tocaA(Balon b, Personaje personaje) {
        int radio = b.getRadio();
        return b.getPosX() + radio > personaje.getPosX()
                && b.getPosX() - radio < personaje.getPosX() + ANCHO
                && b.getPosY() + radio > personaje.getPosY()
                && b.getPosY() - radio < personaje.getPosY() + ALTO;
    }

    private boolean esTiroPropio(Balon b, Jugador jugador) {
        return b.isDisparoEspecial() && b.getPropietarioDisparo() == numeroDe(jugador);
    }

    private Jugador duenoDelDisparo(Balon b) {
        return b.getPropietarioDisparo() == 1 ? jugador1 : jugador2;
    }

    private Jugador rivalDe(Jugador jugador) {
        return jugador == jugador1 ? jugador2 : jugador1;
    }

    private int numeroDe(Jugador jugador) {
        return jugador == jugador1 ? 1 : 2;
    }

    private int direccionDe(Jugador jugador) {
        return jugador == jugador1 ? 1 : -1;
    }

    /** Poder que lleva un balón en este momento, o null si va sin poder. */
    public Poder getPoderDelDisparo(Balon b) {
        return b.isDisparoEspecial() ? duenoDelDisparo(b).getPersonaje().getPoderEspecial() : null;
    }

    /**
     * Poder con el que debe verse un balón. Mientras haya balones falsos todos se ven con el mismo,
     * para que el real no se delate aunque ya haya golpeado a alguien.
     */
    public Poder getPoderVisible(Balon b) {
        return balonesFalsos.isEmpty() ? getPoderDelDisparo(b) : poderDeLosFalsos;
    }

    /** Balones falsos que hay en la cancha; solo sirven para mostrarlos. */
    public List<Balon> getBalonesFalsos() {
        return Collections.unmodifiableList(balonesFalsos);
    }

    protected void verificarGol() {
        Arco arco = cancha.esGol(balon);
        if (arco == null) return;
        // Quien recibe el gol gana carga de poder para intentar remontar.
        if (arco == cancha.getArcoIzquierdo()) {
            marcador[1]++;
            jugador2.anotarGol();
            jugador1.cargarPoder(25);
        } else {
            marcador[0]++;
            jugador1.anotarGol();
            jugador2.cargarPoder(25);
        }
        RegistroErrores.anotar("Gol. El marcador va " + marcador[0] + " a " + marcador[1]);
        reiniciarPosiciones();
    }

    protected void reiniciarPosiciones() {
        quitarBalonesFalsos();
        int suelo = cancha.getSuelo();
        jugador1.getPersonaje().resetearPosicion(cancha.getAncho() * .22, suelo - ALTO);
        jugador2.getPersonaje().resetearPosicion(cancha.getAncho() * .74, suelo - ALTO);
        balon.resetearPosicion(cancha.getAncho() / 2, cancha.getAlto() * .32);
    }

    private void asignarPuntos() {
        // Los premios se entregan una sola vez, aunque el partido se termine por varias vías.
        if (premiosAsignados) return;
        if (marcador[0] > marcador[1]) {
            jugador1.agregarPuntos(100);
            jugador2.agregarPuntos(25);
        } else if (marcador[1] > marcador[0]) {
            jugador2.agregarPuntos(100);
            jugador1.agregarPuntos(25);
        } else {
            jugador1.agregarPuntos(25);
            jugador2.agregarPuntos(25);
        }
        premiosAsignados = true;
        RegistroErrores.anotar("Termina el partido " + marcador[0] + " a " + marcador[1]
                + ". Puntos: " + jugador1.getPuntos() + " y " + jugador2.getPuntos());
    }

    public void terminar() {
        if (!terminado) {
            terminado = true;
            asignarPuntos();
        }
    }

    protected void finalizarSiCorresponde() {
        if (esFinDePartido() && !terminado) {
            terminado = true;
            asignarPuntos();
        }
    }

    /** Guarda todo lo que pasa en la cancha en este momento. */
    void guardar(EscritorPartida e) {
        e.entero(marcador[0], 16);
        e.entero(marcador[1], 16);
        jugador1.guardar(e);
        jugador2.guardar(e);
        balon.guardar(e);

        e.entero(balonesFalsos.size(), 4);
        for (Balon falso : balonesFalsos) falso.guardar(e);
        e.entero(framesBalonesFalsos, 16);
        e.logico(poderDeLosFalsos == jugador2.getPersonaje().getPoderEspecial());
        e.entero(confundidoPorLosFalsos == null ? 0 : confundidoPorLosFalsos == jugador1.getPersonaje() ? 1 : 2, 2);
    }

    void restaurar(LectorPartida l) {
        marcador[0] = l.entero(16);
        marcador[1] = l.entero(16);
        jugador1.restaurar(l);
        jugador2.restaurar(l);
        balon.restaurar(l);

        quitarBalonesFalsos();
        int cantidad = l.entero(4);
        for (int i = 0; i < cantidad; i++) {
            Balon falso = new Balon(0, 0, balon.getRadio());
            falso.restaurar(l);
            balonesFalsos.add(falso);
        }
        framesBalonesFalsos = l.entero(16);
        poderDeLosFalsos = (l.logico() ? jugador2 : jugador1).getPersonaje().getPoderEspecial();
        int confundido = l.entero(2);
        if (confundido == 1) confundidoPorLosFalsos = jugador1.getPersonaje();
        if (confundido == 2) confundidoPorLosFalsos = jugador2.getPersonaje();
    }

    public Jugador getGanador() {
        if (!terminado) return null;
        if (marcador[0] > marcador[1]) return jugador1;
        if (marcador[1] > marcador[0]) return jugador2;
        return null;
    }

    public abstract boolean esFinDePartido();

    /** Indica si se está acabando el tiempo. Un partido sin límite de tiempo siempre responde que no. */
    public boolean estaPorTerminar() {
        return false;
    }

    public int[] getMarcador() { return marcador; }
    public boolean isTerminado() { return terminado; }
    public Balon getBalon() { return balon; }
    public Jugador getJugador1() { return jugador1; }
    public Jugador getJugador2() { return jugador2; }
    public Cancha getCancha() { return cancha; }
}