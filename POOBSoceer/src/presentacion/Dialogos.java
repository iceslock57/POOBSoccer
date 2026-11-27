package presentacion;

import persistencia.ArchivoPartida;
import java.awt.Component;
import java.io.File;

/** Ventanas con las que el juego le pregunta o le avisa algo al usuario. */
interface Dialogos {

    /** Archivo donde se quiere guardar, o null si se cancela. */
    File paraGuardar(Component padre, ArchivoPartida formato);

    /** Archivo que se quiere abrir, o null si se cancela. */
    File paraAbrir(Component padre, ArchivoPartida formato);

    void informar(Component padre, String mensaje);

    void error(Component padre, String mensaje);
}
