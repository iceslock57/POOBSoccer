package Domain;

import java.util.Collections;
import java.util.List;

/**
 * Describe un tiro de poder: cómo sale el balón, cómo avanza y qué le ocurre al rival si lo golpea.
 * Para crear un poder nuevo basta con implementar esta interfaz y registrarlo en el catálogo.
 */
public interface Poder {

    void aplicarEfecto(Personaje objetivo);

    default void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        aplicarEfecto(objetivo);
    }

    /** Velocidad horizontal con la que sale el tiro. */
    default double velocidadDisparo() {
        return 12;
    }

    /** Segundos que dura el efecto sobre el rival. */
    default double duracionEfecto() {
        return 0;
    }

    /**
     * Lanza el tiro hacia el arco contrario. Si nadie lo cambia, el balón sale recto y horizontal.
     * Devuelve los balones falsos que acompañan al real, o una lista vacía si el poder no los usa.
     */
    default List<Balon> lanzar(Balon balon, int direccion, int propietario) {
        balon.aplicarDisparoEspecial(direccion * velocidadDisparo(), 0, propietario);
        return Collections.emptyList();
    }

    /** Permite que el tiro cambie su recorrido en cada cuadro. Un tiro recto no necesita hacer nada. */
    default void avanzarDisparo(Balon balon, int suelo) {
    }

    /** Efecto al golpear al rival; direccion es 1 si el tiro iba a la derecha y -1 si iba a la izquierda. */
    default void aplicarImpacto(Personaje rival, double multiplicadorKick, int direccion) {
        aplicarEfecto(rival, multiplicadorKick);
    }
}
