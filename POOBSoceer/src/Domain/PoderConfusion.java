package Domain;

/**
 * Deja confundido al rival, pero sin crear balones falsos. El catálogo usa PoderIlusion en su lugar.
 */
public class PoderConfusion implements Poder {
    
    private int duracionSegundos;

    public PoderConfusion(int duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        objetivo.aplicarConfusion(duracionSegundos);
    }

    public int getDuracionSegundos(){
        return this.duracionSegundos;
    }
}