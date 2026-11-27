package Domain;

/**
 * Representa el balón del juego.
 * Maneja sus propias físicas básicas de movimiento, gravedad y rebotes.
 */
public class Balon {
    
    private double posX;
    private double posY;
    private double velX;
    private double velY;
    private int radio;
    
    // Constante para la fuerza de gravedad
    private final double GRAVEDAD = 0.42;
    // El rebote en una cabeza siempre se nota, pero tampoco sale disparado.
    private static final double REBOTE_CABEZA = .8, REBOTE_MINIMO = 6, REBOTE_MAXIMO = 13;
    private static final double DESVIO_CABEZA = 3.5, AVANCE_MINIMO = 1.5;
    /** Grueso del palo de arriba de cada arco, igual al trazo con que se dibuja. */
    public static final double GROSOR_TRAVESANO = 7;
    // Velocidad con la que sale el balón cuando lo toca el cuerpo de un jugador.
    private static final double EMPUJE_CUERPO = 3;
    private boolean disparoEspecial;
    private int propietarioDisparo;
    // Lugar desde donde salió el último tiro de poder.
    private double origenDisparo;

    /**
     * Constructor del Balon.
     * 
     * @param x Posición inicial en X.
     * @param y Posición inicial en Y.
     * @param radio Radio del balón para cálculo de colisiones.
     */
    public Balon(double x, double y, int radio) {
        this.posX = x;
        this.posY = y;
        this.radio = radio;
        this.velX = 0;
        this.velY = 0;
    }

    /**
     * Aplica la fuerza de gravedad aumentando la velocidad vertical hacia abajo.
     * El balón está afectado por la gravedad en todo momento.
     */
    public void aplicarGravedad() {
        // Los tiros especiales del anexo son rectos; la gravedad no curva su trayectoria.
        if (!disparoEspecial) this.velY += GRAVEDAD;
    }

    /**
     * Actualiza la posición del balón basándose en su velocidad actual.
     */
    public void mover() {
        this.posX += this.velX;
        this.posY += this.velY;
    }

    /**
     * Maneja el rebote del balón cuando colisiona con los bordes de la cancha.
     * El balón rebota en el suelo y en los bordes.
     * 
     * @param limiteX Ancho de la cancha.
     * @param limiteY Alto de la cancha (suelo).
     */
    public void rebotar(int limiteX, int limiteY) {
        // El techo SIEMPRE es sólido: nunca se puede marcar por encima de la cancha.
        // Rebote en el suelo y techo
        if (this.posY + this.radio >= limiteY) {
            this.posY = limiteY - this.radio;
            this.velY = -this.velY * 0.8; // Pierde un poco de energía al rebotar
        } else if (this.posY - this.radio <= 0) {
            this.posY = this.radio;
            this.velY = -this.velY * 0.8;
        }
        
        // Solo se deja pasar por la abertura vertical real de los arcos.
        boolean alturaDePorteria = this.posY >= limiteY - 150 && this.posY <= limiteY - 25;
        if (this.posX + this.radio >= limiteX && !alturaDePorteria) {
            this.posX = limiteX - this.radio;
            this.velX = -this.velX * 0.8;
        } else if (this.posX - this.radio <= 0 && !alturaDePorteria) {
            this.posX = this.radio;
            this.velX = -this.velX * 0.8;
        }
    }

    /**
     * Rebotes robustos con la geometría real de la cancha.
     * El techo es siempre sólido, incluso durante un disparo especial.
     */
    public void rebotar(Cancha cancha) {
        int ancho = cancha.getAncho();
        int suelo = cancha.getSuelo();
        Arco arco = cancha.getArcoIzquierdo();

        // Techo y suelo se resuelven por separado para no omitir un rebote.
        if (posY - radio <= 0) {
            posY = radio;
            if (velY < 0) velY = Math.abs(velY) * 0.8;
        }
        if (posY + radio >= suelo) {
            posY = suelo - radio;
            if (velY > 0) velY = -Math.abs(velY) * 0.8;
        }

        // Los travesaños se revisan antes que los lados, porque pueden acomodar el balón.
        rebotarEnTravesano(cancha.getArcoIzquierdo());
        rebotarEnTravesano(cancha.getArcoDerecho());

        // Los laterales solo se abren dentro de la boca de las porterías.
        double arriba = arco.getPosY();
        double abajo = arriba + arco.getAlto();
        boolean dentroAbertura = posY - radio >= arriba
                && posY + radio <= abajo;

        if (posX - radio <= 0 && !dentroAbertura) {
            posX = radio;
            if (velX < 0) velX = Math.abs(velX) * 0.8;
        }
        if (posX + radio >= ancho && !dentroAbertura) {
            posX = ancho - radio;
            if (velX > 0) velX = -Math.abs(velX) * 0.8;
        }
    }

    /**
     * El palo de arriba del arco es sólido. Si el balón le cae encima, rebota hacia arriba;
     * si lo golpea desde abajo, dentro del arco, rebota hacia abajo.
     */
    private void rebotarEnTravesano(Arco arco) {
        double izquierda = arco.getPosX(), derecha = izquierda + arco.getAncho();
        double arriba = arco.getPosY() - GROSOR_TRAVESANO / 2, abajo = arco.getPosY() + GROSOR_TRAVESANO / 2;

        boolean tocaElPalo = posX + radio > izquierda && posX - radio < derecha
                && posY + radio > arriba && posY - radio < abajo;
        if (!tocaElPalo) return;

        // Se mira dónde estaba el balón un cuadro antes para saber por qué lado llegó.
        boolean veniaDeArriba = posY - velY <= arco.getPosY();
        if (veniaDeArriba) {
            posY = arriba - radio;
            if (velY > 0) velY = -velY * 0.8;
        } else {
            posY = abajo + radio;
            if (velY < 0) velY = -velY * 0.8;
        }
    }

    /**
     * Devuelve el balón a una posición específica sin velocidad.
     * Usado después de cada gol para que el balón caiga desde el centro.
     * 
     * @param x Nueva posición X.
     * @param y Nueva posición Y.
     */
    public void resetearPosicion(double x, double y) {
        this.posX = x;
        this.posY = y;
        this.velX = 0;
        this.velY = 0;
        this.disparoEspecial = false;
        this.propietarioDisparo = 0;
    }
    /**
     * Sobrescribe la velocidad actual del balón por una nueva fuerza de impacto.
     */
    public void aplicarFuerza(double fX, double fY) {
        this.velX = fX; this.velY = fY; this.disparoEspecial = false; this.propietarioDisparo = 0;
    }
    public void aplicarDisparoEspecial(double fX, double fY, int propietario) {
        this.velX=fX; this.velY=fY; this.disparoEspecial=true; this.propietarioDisparo=propietario;
        this.origenDisparo = posX;
    }
    /** Cambia la velocidad sin quitarle al balón la marca de tiro de poder. */
    public void cambiarVelocidad(double nuevaVelX, double nuevaVelY) {
        this.velX = nuevaVelX;
        this.velY = nuevaVelY;
    }

    /** El balón viene de arriba y está tocando la cabeza, no un lado del cuerpo. */
    public boolean caeSobre(Personaje personaje) {
        double alturaAnterior = posY - Math.max(0, velY);
        return alturaAnterior <= personaje.getPosY();
    }

    /**
     * Rebota en la cabeza de un personaje. Sube más si la cabeza venía saltando y se desvía hacia
     * el lado donde pegó; si pegó justo en el centro, avanza en la dirección indicada.
     */
    public void rebotarEnCabeza(Personaje personaje, int direccion) {
        double mitad = Personaje.ANCHO / 2.0;
        double lado = Math.max(-1, Math.min(1, (posX - personaje.getPosX() - mitad) / mitad));
        double velCabeza = Math.min(0, personaje.getVelY());
        double golpe = Math.max(0, velY - velCabeza);
        double subida = golpe * REBOTE_CABEZA - velCabeza;

        posY = personaje.getPosY() - radio;
        velY = -Math.max(REBOTE_MINIMO, Math.min(REBOTE_MAXIMO, subida));
        velX = velX * .7 + lado * DESVIO_CABEZA;
        if (Math.abs(velX) < AVANCE_MINIMO) velX = direccion * AVANCE_MINIMO;
    }

    /**
     * Rebota en un costado del cuerpo. Sale por el lado donde está el balón, fuera del cuerpo para que
     * no quede atrapado, y con al menos la mitad de la velocidad que traía, o un empujón suave si venía quieto.
     * Si queda justo en el centro del cuerpo, sale hacia donde mira el jugador.
     */
    public void rebotarEnElCostado(Personaje personaje, int direccion) {
        double centro = personaje.getPosX() + Personaje.ANCHO / 2.0;
        int lado = posX < centro ? -1 : posX > centro ? 1 : direccion;

        posX = lado < 0 ? personaje.getPosX() - radio : personaje.getPosX() + Personaje.ANCHO + radio;
        velX = lado * Math.max(EMPUJE_CUERPO, Math.abs(velX) * .5);
        velY = Math.min(velY, -2.5);
        disparoEspecial = false;
        propietarioDisparo = 0;
    }

    /** Evita que el balón salga por los lados, incluso por la abertura de los arcos. */
    public void rebotarEnLosLados(int limiteX) {
        if (posX - radio < 0) {
            posX = radio;
            velX = Math.abs(velX) * 0.8;
        } else if (posX + radio > limiteX) {
            posX = limiteX - radio;
            velX = -Math.abs(velX) * 0.8;
        }
    }

    public boolean isDisparoEspecial(){ return disparoEspecial; }
    public int getPropietarioDisparo(){ return propietarioDisparo; }
    public void consumirDisparoEspecial(){ disparoEspecial=false; propietarioDisparo=0; }

    void guardar(EscritorPartida e) {
        e.decimal(posX);
        e.decimal(posY);
        e.decimal(velX);
        e.decimal(velY);
        e.logico(disparoEspecial);
        e.entero(propietarioDisparo, 2);
        e.decimal(origenDisparo);
    }

    void restaurar(LectorPartida l) {
        posX = l.decimal();
        posY = l.decimal();
        velX = l.decimal();
        velY = l.decimal();
        disparoEspecial = l.logico();
        propietarioDisparo = l.entero(2);
        origenDisparo = l.decimal();
    }

    // Getters con estructura estricta

    public double getOrigenDisparo() {
        return origenDisparo;
    }

    public double getPosX(){
        return this.posX;
    }

    public double getPosY(){
        return this.posY;
    }

    public double getVelX(){
        return this.velX;
    }

    public double getVelY(){
        return this.velY;
    }

    public int getRadio(){
        return this.radio;
    }
}