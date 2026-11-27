package Domain;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Bitácora del juego. Anota en el archivo poobsoccer.log los errores, para poder revisarlos después,
 * y los momentos importantes de cada partida, como cuándo empieza, los goles o cuándo se guarda.
 */
public final class RegistroErrores {

    private static final Logger LOGGER = Logger.getLogger("POOBSoccer");

    static {
        // El archivo se puede cambiar con la propiedad poobsoccer.log; las pruebas usan uno propio.
        try {
            FileHandler archivo = new FileHandler(System.getProperty("poobsoccer.log", "poobsoccer.log"), true);
            archivo.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(archivo);
            // Lo anotado va solo al archivo, para no llenar la consola mientras se juega.
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo crear poobsoccer.log", e);
        }
    }

    private RegistroErrores() {
    }

    /** Anota un error junto con su causa, para saber qué pasó y dónde. */
    public static void registrar(String mensaje, Throwable error) {
        LOGGER.log(Level.SEVERE, mensaje, error);
    }

    /** Anota algo que pasó en el juego y que vale la pena recordar. */
    public static void anotar(String mensaje) {
        LOGGER.info(mensaje);
    }

    /** Anota algo extraño que no detiene el juego. */
    public static void advertir(String mensaje) {
        LOGGER.warning(mensaje);
    }
}
