package Domain;

/**
 * Representa una portería y determina si el balón cruzó completamente su abertura.
 * También conserva el equipo propietario para identificar qué lado recibió el gol.
 */
public class Arco {

    private final int posX;
    private final int posY;
    private final int ancho;
    private final int alto;
    private final String equipoPropietario;
    private final boolean izquierdo;

    public Arco(int x, int y, int w, int h, String equipo, boolean izquierdo) {
        posX = x;
        posY = y;
        ancho = w;
        alto = h;
        equipoPropietario = equipo;
        this.izquierdo = izquierdo;
    }

    /** Comprueba que el balón haya atravesado la línea y permanezca dentro de la altura del arco. */
    public boolean contieneBalon(Balon b) {
        double y = b.getPosY();
        int r = b.getRadio();

        return y - r > posY + 5
                && y + r < posY + alto - 5
                && (izquierdo
                ? b.getPosX() + r <= posX
                : b.getPosX() - r >= posX + ancho);
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public String getEquipoPropietario() {
        return equipoPropietario;
    }

    public boolean isEsArcoIzquierdo() {
        return izquierdo;
    }
}
