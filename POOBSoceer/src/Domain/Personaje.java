package Domain;

/** Personaje con movimiento, salto, daño y efectos temporales. */
public class Personaje {

    private static final double GRAVEDAD = .48;
    private static final int CUADROS_POR_SEGUNDO = 60;
    /** Caja de choque del personaje en píxeles de cancha. La interfaz dibuja al personaje dentro de esta caja. */
    public static final int ANCHO = 52, ALTO = 80;
    // Lo que avanza en cada cuadro y la fuerza del salto con la estadística en 1; cada punto extra suma un 10 %.
    private static final double IMPULSO_SALTO_BASE = 7.7;
    /** Píxeles que avanza en cada cuadro un personaje con Speed 1. Subir este valor hace más rápidos a todos. */
    public static final double PASO_BASE = 2.7;
    /** Cuadros que dura una patada (12 cuadros = 0,2 s a 60 fps). */
    public static final int DURACION_PATADA = 12;

    // Hasta dónde llega el pie por delante del cuerpo y a qué altura del cuerpo empieza.
    private static final double ALCANCE_PIE = 24, ALTURA_PIE = 44, PARTE_BAJA_RIVAL = 36;
    // Al patear, la punta del pie sube; por eso alcanza balones un poco más altos que el tobillo.
    private static final double ALTURA_PUNTA_PIE = 28;
    // El empuje arranca fuerte y se va frenando hasta un arrastre constante.
    private static final double EMPUJE_INICIAL = 14, EMPUJE_MINIMO = 6, FRENADO = .82;
    private static final double SEGUNDOS_EMPUJE = 1.5;
    private static final double LANZAMIENTO_AL_AIRE = 11;

    private final String nombre, pais;

    private int velocidad, salto, kick, survival;
    private Poder poderEspecial;

    private double posX, posY, velX, velY;

    private int framesPateando, framesAturdido, framesEfecto, framesCongelado, framesInconsciente, framesEmpuje;
    private int framesQuemado;
    private double danioQuemaduraPorSegundo;
    private int direccionEmpuje;
    private int vidaActual;

    private boolean electrificado, confundido;
    // Se vuelve verdadero cuando el pie ya golpeó el balón durante la patada actual.
    private boolean pieUsado;

    private int suelo = 500, limiteX = 1000;

    public Personaje(String n, String p, int v, int s, int k, Poder poder) {
        this(n, p, v, s, k, 1, poder);
    }

    public Personaje(String n, String p, int v, int s, int k, int surv, Poder poder) {
        nombre = n;
        pais = p;
        velocidad = limitar(v);
        salto = limitar(s);
        kick = limitar(k);
        survival = limitar(surv);
        poderEspecial = poder;
        vidaActual = getVidaMaxima();
    }

    /** Cada punto por encima de uno suma un diez por ciento al valor base. */
    private double multiplicadorPorNivel(int nivel) {
        return 1 + .10 * (nivel - 1);
    }

    private int limitar(int x) {
        return Math.max(1, Math.min(10, x));
    }

    private static int enCuadros(double segundos) {
        return (int) Math.round(Math.max(0, segundos) * CUADROS_POR_SEGUNDO);
    }

    public void configurarCancha(int ancho, int suelo) {
        limiteX = ancho;
        this.suelo = suelo - ALTO;

        if (posY > this.suelo) posY = this.suelo;
    }

    public int getVidaMaxima() {
        return 100 + 10 * (survival - 1);
    }

    public double getMultiplicadorKick() {
        return multiplicadorPorNivel(kick);
    }

    /** Camina hacia un lado. La electrocución lo deja a mitad de velocidad y el empuje no le permite caminar. */
    public void mover(int dir, Personaje rival) {
        if (inmovilizado() || isEmpujado()) return;

        double ritmo = multiplicadorPorNivel(velocidad) * (electrificado ? .5 : 1);
        ubicarEnX(posX + dir * PASO_BASE * ritmo, rival);
    }

    /** Lleva al personaje a una posición sin salirse de la cancha ni atravesar al rival. */
    private void ubicarEnX(double x, Personaje rival) {
        posX = dentroDeLaCancha(mantenerSeparacionHorizontal(dentroDeLaCancha(x), rival));
    }

    private double dentroDeLaCancha(double x) {
        return Math.max(0, Math.min(limiteX - ANCHO, x));
    }

    private double mantenerSeparacionHorizontal(double x, Personaje rival) {
        if (rival == null) return x;
        return posX <= rival.posX
                ? Math.min(x, rival.posX - ANCHO)
                : Math.max(x, rival.posX + ANCHO);
    }

    public void saltar() {
        if (inmovilizado() || electrificado) return;
        if (posY >= suelo - .1) velY = -IMPULSO_SALTO_BASE * multiplicadorPorNivel(salto);
    }

    /** Empieza una patada nueva; el pie vuelve a poder golpear el balón. */
    public void patear() {
        if (inmovilizado()) return;
        framesPateando = DURACION_PATADA;
        pieUsado = false;
    }

    /**
     * El pie ya le pegó al balón en esta patada. El movimiento sigue hasta terminar,
     * pero el mismo pie no vuelve a golpear hasta la siguiente patada.
     */
    public void consumirPatada() {
        pieUsado = true;
    }

    public void actualizarEstado() {
        actualizarEstado(null);
    }

    /** Avanza un cuadro: mueve al personaje y descuenta el tiempo de sus efectos. */
    public void actualizarEstado(Personaje rival) {
        double destino = dentroDeLaCancha(posX + velX);
        ubicarEnX(destino, rival);
        boolean chocoConElRival = Math.abs(posX - destino) > .001;

        velX = chocoConElRival ? 0 : velX * FRENADO;
        if (Math.abs(velX) < .15) velX = 0;
        avanzarEmpuje();

        velY += GRAVEDAD;
        posY += velY;

        if (posY >= suelo) {
            posY = suelo;
            velY = 0;
        }

        if (framesPateando > 0) framesPateando--;
        // Va antes del aturdimiento para no quitarle vida justo cuando la recupera.
        avanzarQuemadura();

        if (framesAturdido > 0) {
            framesAturdido--;

            if (framesAturdido == 0) {
                vidaActual = getVidaMaxima();
            }
        }

        if (framesEfecto > 0) {
            framesEfecto--;

            if (framesEfecto == 0) {
                electrificado = false;
                confundido = false;
            }
        }

        if (framesCongelado > 0) framesCongelado--;
        if (framesInconsciente > 0) framesInconsciente--;
    }

    /** El fuego quita vida una vez por segundo. Si ya quedó sin vida no le sigue pegando, para no alargar el aturdimiento. */
    private void avanzarQuemadura() {
        if (framesQuemado == 0) return;

        framesQuemado--;
        boolean pasoUnSegundo = framesQuemado % CUADROS_POR_SEGUNDO == 0;
        if (pasoUnSegundo && !aturdido()) recibirDanio(danioQuemaduraPorSegundo);
    }

    /** Mientras dura el empuje, el personaje sigue siendo arrastrado aunque vaya perdiendo impulso. */
    private void avanzarEmpuje() {
        if (framesEmpuje == 0) return;

        framesEmpuje--;
        velX = framesEmpuje == 0 ? 0 : direccionEmpuje * Math.max(EMPUJE_MINIMO, Math.abs(velX));
    }

    public void recibirDanio(double d) {
        vidaActual = Math.max(0, vidaActual - (int) Math.round(Math.max(0, d)));

        if (vidaActual == 0) aturdir(3);
    }

    public void aturdir(int s) {
        framesAturdido = Math.max(framesAturdido, enCuadros(s));
        framesPateando = 0;
        framesEmpuje = 0;
        velX = 0;
    }

    public void congelar(double s) {
        limpiarEfectosEspeciales();
        framesCongelado = enCuadros(s);
        framesPateando = 0;
        velX = 0;
    }

    /** No puede saltar y camina a la mitad de su velocidad. */
    public void aplicarElectrocucion(double s) {
        limpiarEfectosEspeciales();
        electrificado = true;
        framesEfecto = enCuadros(s);
        velX *= .5;
    }

    /** Sale volando y queda sin poder moverse, saltar ni patear. */
    public void aplicarInconsciencia(double s) {
        limpiarEfectosEspeciales();
        framesInconsciente = enCuadros(s);
        framesPateando = 0;
        velX = 0;
        velY = -LANZAMIENTO_AL_AIRE;
    }

    /** Queda confundido; mientras dure, en la cancha siguen los balones falsos. */
    public void aplicarConfusion(double s) {
        limpiarEfectosEspeciales();
        confundido = true;
        framesEfecto = enCuadros(s);
    }

    /** Puede moverse con normalidad, pero va perdiendo vida cada segundo. */
    public void aplicarQuemadura(double s, double danioPorSegundo) {
        limpiarEfectosEspeciales();
        framesQuemado = enCuadros(s);
        danioQuemaduraPorSegundo = danioPorSegundo;
    }

    public void aplicarEmpuje(int direccion) {
        aplicarEmpuje(direccion, SEGUNDOS_EMPUJE);
    }

    /** Lo arrastra en una dirección durante el tiempo indicado. Un personaje aturdido no se mueve. */
    public void aplicarEmpuje(int direccion, double s) {
        limpiarEfectosEspeciales();
        if (inmovilizado()) return;

        direccionEmpuje = direccion;
        framesEmpuje = enCuadros(s);
        velX = direccion * EMPUJE_INICIAL;
        velY = 0;
    }

    /** Los efectos no se acumulan: cada uno nuevo borra el que hubiera antes. */
    private void limpiarEfectosEspeciales() {
        if (framesEmpuje > 0) velX = 0;
        framesEfecto = framesCongelado = framesInconsciente = framesEmpuje = framesQuemado = 0;
        electrificado = confundido = false;
    }

    public void resetearPosicion(double x, double y) {
        posX = x;
        posY = y;
        limpiarEfectosEspeciales();
        velX = 0;
        velY = 0;
        framesPateando = framesAturdido = 0;
        vidaActual = getVidaMaxima();
    }

    /** Guarda cómo está en este momento: dónde va, cuánta vida le queda y qué efectos sufre. */
    void guardar(EscritorPartida e) {
        e.decimal(posX);
        e.decimal(posY);
        e.decimal(velX);
        e.decimal(velY);
        e.entero(vidaActual, 16);
        e.entero(framesPateando, 16);
        e.entero(framesAturdido, 16);
        e.entero(framesEfecto, 16);
        e.entero(framesCongelado, 16);
        e.entero(framesInconsciente, 16);
        e.entero(framesEmpuje, 16);
        e.entero(framesQuemado, 16);
        // La dirección es -1, 0 o 1; se suma uno para que no sea negativa.
        e.entero(direccionEmpuje + 1, 2);
        e.decimal(danioQuemaduraPorSegundo);
        e.logico(electrificado);
        e.logico(confundido);
        e.logico(pieUsado);
    }

    void restaurar(LectorPartida l) {
        posX = l.decimal();
        posY = l.decimal();
        velX = l.decimal();
        velY = l.decimal();
        vidaActual = Math.min(getVidaMaxima(), l.entero(16));
        framesPateando = l.entero(16);
        framesAturdido = l.entero(16);
        framesEfecto = l.entero(16);
        framesCongelado = l.entero(16);
        framesInconsciente = l.entero(16);
        framesEmpuje = l.entero(16);
        framesQuemado = l.entero(16);
        direccionEmpuje = l.entero(2) - 1;
        danioQuemaduraPorSegundo = l.decimal();
        electrificado = l.logico();
        confundido = l.logico();
        pieUsado = l.logico();
    }

    private boolean inmovilizado() {
        return framesAturdido > 0 || framesCongelado > 0 || framesInconsciente > 0;
    }

    public boolean aturdido() {
        return framesAturdido > 0;
    }

    public boolean isAturdido() {
        return aturdido();
    }

    public boolean congelado() {
        return framesCongelado > 0;
    }

    public boolean isCongelado() {
        return congelado();
    }

    public boolean isPateando() {
        return framesPateando > 0;
    }

    /** Cuadros que le quedan a la patada actual (0 si no está pateando). Lo usa la interfaz para animarla. */
    public int getFramesPateando() {
        return framesPateando;
    }

    public boolean isElectrificado() {
        return electrificado;
    }

    public boolean isConfundido() {
        return confundido;
    }

    public boolean isInconsciente() {
        return framesInconsciente > 0;
    }

    public boolean isQuemado() {
        return framesQuemado > 0;
    }

    public boolean isEmpujado() {
        return framesEmpuje > 0;
    }

    /** Indica si el pie llega a tocar al rival; direccion es 1 si patea a la derecha y -1 si patea a la izquierda. */
    public boolean alcanzaConPatada(Personaje rival, int direccion) {
        if (rival == null) return false;

        double distancia = direccion > 0
                ? rival.posX - (posX + ANCHO)
                : posX - (rival.posX + ANCHO);
        boolean mismaAltura = posY + ALTURA_PIE < rival.posY + ALTO
                && posY + ALTO > rival.posY + PARTE_BAJA_RIVAL;
        return distancia >= 0 && distancia <= ALCANCE_PIE && mismaAltura;
    }

    /**
     * Indica si el pie, mientras patea, alcanza el balón. El pie cubre la mitad delantera del cuerpo
     * y sale un poco por delante, desde la altura a la que sube su punta hasta el suelo.
     * Un balón que está detrás o a la altura de la cabeza no lo toca el pie.
     */
    public boolean alcanzaConElPie(Balon balon, int direccion) {
        if (!isPateando() || pieUsado || balon == null) return false;

        double centro = posX + ANCHO / 2.0;
        double desde = direccion > 0 ? centro : posX - ALCANCE_PIE;
        double hasta = direccion > 0 ? posX + ANCHO + ALCANCE_PIE : centro;
        int radio = balon.getRadio();

        return balon.getPosX() + radio > desde && balon.getPosX() - radio < hasta
                && balon.getPosY() + radio > posY + ALTURA_PUNTA_PIE && balon.getPosY() - radio < posY + ALTO;
    }

    public int getVidaActual() {
        return vidaActual;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPais() {
        return pais;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getVelY() {
        return velY;
    }

    public int getFuerza() {
        return kick;
    }

    public int getVelocidad() {
        return velocidad;
    }

    public int getSalto() {
        return salto;
    }

    public int getKick() {
        return kick;
    }

    public int getSurvival() {
        return survival;
    }

    public Poder getPoderEspecial() {
        return poderEspecial;
    }
}
