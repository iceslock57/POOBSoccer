package Domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Guarda el progreso de una persona: su nombre, los puntos que ha ganado y las estadísticas de cada
 * uno de sus personajes. Mejorar un personaje no cambia a los demás ni a los del otro jugador.
 */
public class PerfilJugador {

    /** Las cuatro estadísticas que se pueden mejorar. */
    public enum Estadistica {
        VIDA, SPEED, JUMP, KICK
    }

    private final String identificador;
    private String nombre;
    private long puntos;
    private final Map<String, EstadisticasPersonaje> personajes = new HashMap<>();

    /** Crea un perfil nuevo, sin puntos y con todos los personajes en el nivel inicial. */
    public PerfilJugador(String identificador, String nombre) {
        this(identificador, nombre, 0);
    }

    /** Reconstruye un perfil con los puntos que se recuperaron del archivo local. */
    PerfilJugador(String identificador, String nombre, long puntos) {
        this.identificador = identificador;
        this.nombre = "Jugador";
        setNombre(nombre);
        this.puntos = Math.max(0, puntos);
    }

    /** Evita que un nombre vacío o con espacios termine guardado como identidad. */
    public synchronized void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return;
        this.nombre = nombre.trim();
    }

    /** Suma los puntos que se ganaron al finalizar un partido. */
    public synchronized void agregarPuntos(long cantidad) {
        if (cantidad > 0 && puntos <= Long.MAX_VALUE - cantidad) {
            puntos += cantidad;
        }
    }

    /** Estadísticas del personaje de ese país. La primera vez que se piden están todas en uno. */
    public synchronized EstadisticasPersonaje getEstadisticas(String pais) {
        return personajes.computeIfAbsent(pais, p -> new EstadisticasPersonaje());
    }

    /** Precio de la siguiente mejora de ese personaje, sin importar cuál estadística se elija. */
    public synchronized long getCostoSiguienteMejora(String pais) {
        return getEstadisticas(pais).getCostoSiguienteMejora();
    }

    /** Indica si le alcanzan los puntos y la estadística todavía no llega al máximo. */
    public synchronized boolean puedeMejorar(String pais, Estadistica estadistica) {
        return estadistica != null
                && getEstadisticas(pais).puedeSubir(estadistica)
                && puntos >= getCostoSiguienteMejora(pais);
    }

    /** Compra un nivel para ese personaje. Devuelve false cuando la compra no se puede realizar. */
    public synchronized boolean mejorar(String pais, Estadistica estadistica) {
        if (!puedeMejorar(pais, estadistica)) return false;

        puntos -= getCostoSiguienteMejora(pais);
        getEstadisticas(pais).subir(estadistica);
        RegistroErrores.anotar(pais + " sube " + estadistica + " al nivel " + getNivel(pais, estadistica)
                + "; quedan " + puntos + " puntos");
        return true;
    }

    /** Nivel actual de una estadística de ese personaje. */
    public synchronized int getNivel(String pais, Estadistica estadistica) {
        return getEstadisticas(pais).getNivel(estadistica);
    }

    /** Guarda el nombre, los puntos y las mejoras de cada personaje. */
    synchronized void guardar(EscritorPartida e) {
        e.texto(nombre);
        e.largo(puntos);
        e.entero(personajes.size(), 8);
        for (Map.Entry<String, EstadisticasPersonaje> personaje : personajes.entrySet()) {
            e.texto(personaje.getKey());
            personaje.getValue().guardar(e);
        }
    }

    /** Lee un perfil guardado sin tocar el que se está usando. */
    static PerfilJugador leer(LectorPartida l, String identificador) {
        PerfilJugador leido = new PerfilJugador(identificador, l.texto(), l.largo());
        int cantidad = l.entero(8);
        for (int i = 0; i < cantidad; i++) {
            leido.personajes.put(l.texto(), EstadisticasPersonaje.leer(l));
        }
        return leido;
    }

    /** Reemplaza todo el progreso por el de otro perfil. */
    synchronized void copiarDe(PerfilJugador otro) {
        nombre = otro.nombre;
        puntos = otro.puntos;
        personajes.clear();
        personajes.putAll(otro.personajes);
    }

    /** Devuelve la clave que permite guardar este perfil sin confundirlo con el otro jugador. */
    public String getIdentificador() {
        return identificador;
    }

    /** Devuelve el nombre visible que eligió la persona. */
    public synchronized String getNombre() {
        return nombre;
    }

    /** Devuelve los puntos que todavía puede gastar en nuevas mejoras. */
    public synchronized long getPuntos() {
        return puntos;
    }
}
