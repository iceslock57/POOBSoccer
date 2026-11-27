package Domain;

import java.util.Collections;
import java.util.List;

/** Rusia: un bloque de hielo que vuela en arco, cae al suelo y sigue patinando; si golpea, congela al rival. */
public class PoderHielo implements Poder {

    private static final double DANIO_BASE = 35;
    private static final double SEGUNDOS_CONGELADO = 2.5;
    private static final double IMPULSO_HACIA_ARRIBA = 9;
    private static final double CAIDA = .5;
    private static final double VELOCIDAD_PATINANDO = 17;

    @Override
    public double velocidadDisparo() {
        return 11;
    }

    @Override
    public double duracionEfecto() {
        return SEGUNDOS_CONGELADO;
    }

    /** El balón sale hacia arriba para pasar por encima de lo que tenga cerca. */
    @Override
    public List<Balon> lanzar(Balon balon, int direccion, int propietario) {
        balon.aplicarDisparoEspecial(direccion * velocidadDisparo(), -IMPULSO_HACIA_ARRIBA, propietario);
        return Collections.emptyList();
    }

    /** En el aire va cayendo; al tocar el suelo deja de rebotar y se desliza más rápido. */
    @Override
    public void avanzarDisparo(Balon balon, int suelo) {
        boolean enElSuelo = balon.getPosY() + balon.getRadio() >= suelo;
        if (enElSuelo) {
            balon.cambiarVelocidad(Math.signum(balon.getVelX()) * VELOCIDAD_PATINANDO, 0);
        } else {
            balon.cambiarVelocidad(balon.getVelX(), balon.getVelY() + CAIDA);
        }
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        aplicarEfecto(objetivo, 1.0);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        objetivo.recibirDanio(DANIO_BASE * multiplicadorKick);
        objetivo.congelar(SEGUNDOS_CONGELADO);
    }
}