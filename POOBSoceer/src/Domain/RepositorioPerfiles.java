package Domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lee y escribe los dos perfiles locales de DOPOSoccer.
 * El archivo queda en la carpeta personal del sistema operativo, separado
 * del código fuente para que actualizar el juego no borre el progreso.
 */
public class RepositorioPerfiles {

    private final Path archivo;

    /** Usa una ubicación estable dentro de la carpeta personal del usuario. */
    public RepositorioPerfiles() {
        this(Paths.get(System.getProperty("user.home"), ".poobsoccer", "perfiles.properties"));
    }

    /** Permite indicar otra ruta cuando se necesite probar o respaldar los perfiles. */
    public RepositorioPerfiles(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Recupera los perfiles guardados. Cuando es la primera ejecución o todavía
     * no existe el archivo, crea dos perfiles nuevos con todos los niveles en 1.
     */
    public PerfilJugador[] cargarPerfiles() {
        Properties datos = new Properties();
        if (Files.isRegularFile(archivo)) {
            try (InputStream entrada = Files.newInputStream(archivo)) {
                datos.load(entrada);
            } catch (IOException ex) {
                RegistroErrores.registrar("No fue posible leer los perfiles guardados", ex);
            }
        }

        PerfilJugador primero = cargarPerfil(datos, "jugador1", "Jugador 1");
        PerfilJugador segundo = cargarPerfil(datos, "jugador2", "Jugador 2");
        return new PerfilJugador[]{primero, segundo};
    }

    /** Lee todos los datos de un jugador y aplica valores seguros si falta alguna propiedad. */
    private PerfilJugador cargarPerfil(Properties datos, String clave, String nombrePorDefecto) {
        PerfilJugador perfil = new PerfilJugador(
                clave,
                datos.getProperty(clave + ".nombre", nombrePorDefecto),
                leerLong(datos, clave + ".puntos", 0L));
        for (String pais : CatalogoPersonajes.paises()) {
            for (PerfilJugador.Estadistica e : PerfilJugador.Estadistica.values()) {
                perfil.getEstadisticas(pais).setNivel(e, leerEntero(datos, claveNivel(clave, pais, e), 1));
            }
        }
        return perfil;
    }

    /** Nombre con el que se guarda el nivel de una estadística de un personaje. */
    private String claveNivel(String clave, String pais, PerfilJugador.Estadistica estadistica) {
        return clave + "." + pais + "." + estadistica.name().toLowerCase();
    }

    /** Convierte un dato numérico del archivo sin dejar que un valor inválido cierre el juego. */
    private int leerEntero(Properties datos, String clave, int valorPorDefecto) {
        try {
            return Integer.parseInt(datos.getProperty(clave, String.valueOf(valorPorDefecto)));
        } catch (NumberFormatException ex) {
            return valorPorDefecto;
        }
    }

    /** Lee el saldo de puntos sin perder precisión cuando las mejoras llegan a precios altos. */
    private long leerLong(Properties datos, String clave, long valorPorDefecto) {
        try {
            long valor = Long.parseLong(datos.getProperty(clave, String.valueOf(valorPorDefecto)));
            return Math.max(0L, valor);
        } catch (NumberFormatException ex) {
            return valorPorDefecto;
        }
    }

    /** Guarda ambos perfiles juntos para mantener sincronizado el progreso de los dos jugadores. */
    public synchronized void guardarPerfiles(PerfilJugador primero, PerfilJugador segundo) throws IOException {
        Properties datos = new Properties();
        agregarPerfil(datos, primero);
        agregarPerfil(datos, segundo);

        Path carpeta = archivo.getParent();
        if (carpeta != null) Files.createDirectories(carpeta);

        try (OutputStream salida = Files.newOutputStream(archivo)) {
            datos.store(salida, "Progreso local de DOPOSoccer. No borrar para conservar los perfiles.");
        }
    }

    /** Escribe todos los campos de un perfil con nombres legibles dentro del archivo. */
    private void agregarPerfil(Properties datos, PerfilJugador perfil) {
        if (perfil == null) return;
        String clave = perfil.getIdentificador();
        datos.setProperty(clave + ".nombre", perfil.getNombre());
        datos.setProperty(clave + ".puntos", String.valueOf(perfil.getPuntos()));
        for (String pais : CatalogoPersonajes.paises()) {
            for (PerfilJugador.Estadistica e : PerfilJugador.Estadistica.values()) {
                datos.setProperty(claveNivel(clave, pais, e), String.valueOf(perfil.getNivel(pais, e)));
            }
        }
    }
}
