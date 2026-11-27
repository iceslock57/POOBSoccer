package Domain;

import java.util.Set;

/**
 * Forma de jugar de una máquina. Para crear un perfil nuevo basta con implementar esta interfaz
 * y registrarlo en CatalogoMaquinas.
 */
public interface EstrategiaMaquina {

    /** Mira cómo va el partido y responde qué movimientos hacer; puede no hacer ninguno. */
    Set<Accion> decidir(SituacionPartido situacion);

    /** Cuadros que espera antes de volver a decidir. Entre menos espere, más rápido reacciona. */
    int cuadrosEntreDecisiones();

    /** Frase corta que le explica al usuario cómo juega. */
    String descripcion();
}
