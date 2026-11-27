package Domain;

import java.util.Collections;
import java.util.List;

/** Nigeria: un remolino que avanza a ras de suelo y lanza al rival por el aire, dejándolo inconsciente. */
public class PoderTornado implements Poder {

    private static final double DANIO_BASE = 40;
    private static final double SEGUNDOS_INCONSCIENTE = 3;
    private static final double VELOCIDAD_BAJADA = 8;
    private static final double CAIDA = 1.2;
    private static final double SALTO_CORTO = 5;

    @Override
    public double velocidadDisparo() {
        return 18;
    }

    @Override
    public double duracionEfecto() {
        return SEGUNDOS_INCONSCIENTE;
    }

    /** El balón baja de inmediato para buscar el suelo. */
    @Override
    public List<Balon> lanzar(Balon balon, int direccion, int propietario) {
        balon.aplicarDisparoEspecial(direccion * velocidadDisparo(), VELOCIDAD_BAJADA, propietario);
        return Collections.emptyList();
    }

    /** Cada vez que toca el suelo da un salto muy corto, por eso rebota rápido y casi no se levanta. */
    @Override
    public void avanzarDisparo(Balon balon, int suelo) {
        boolean enElSuelo = balon.getPosY() + balon.getRadio() >= suelo;
        double velY = enElSuelo ? -SALTO_CORTO : balon.getVelY() + CAIDA;
        balon.cambiarVelocidad(balon.getVelX(), velY);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        aplicarEfecto(objetivo, 1.0);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        objetivo.recibirDanio(DANIO_BASE * multiplicadorKick);
        objetivo.aplicarInconsciencia(SEGUNDOS_INCONSCIENTE);
    }
}
