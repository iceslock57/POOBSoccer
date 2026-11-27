package Domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa a un jugador controlado por una persona mediante el teclado.
 */
public class JugadorHumano extends Jugador {
    
    private ArrayList<String> teclas;
    private Map<String, AccionJugador> mapaAcciones;

    /**
     * Interfaz funcional interna para manejar las acciones de forma extensible.
     */
    private interface AccionJugador {
        void ejecutar(Partido partido);
    }

    /**
     * Constructor del Jugador Humano.
     * 
     * @param equipo El equipo o país del jugador.
     * @param personaje El personaje seleccionado.
     * @param teclasAsignadas Arreglo con las teclas en orden: [Izquierda, Derecha, Saltar, Patear, Poder].
     */
    public JugadorHumano(String equipo, Personaje personaje, String[] teclasAsignadas) {
        this(equipo, personaje, teclasAsignadas, null);
    }

    /** Vincula el jugador del partido con su perfil para conservar los puntos al finalizar. */
    public JugadorHumano(String equipo, Personaje personaje, String[] teclasAsignadas, PerfilJugador perfil) {
        super(equipo, personaje, perfil);
        this.teclas = new ArrayList<>(Arrays.asList(teclasAsignadas));
        this.mapaAcciones = new HashMap<>();

        configurarAcciones();
    }

    /**
     * Configura el mapa de acciones para evitar cascadas de condicionales IF.
     */
    private void configurarAcciones() {
        if (teclas.size() >= 5) {
            // Mover a la izquierda (-1 en el eje X)
            mapaAcciones.put(teclas.get(0), partido -> {
                Personaje rival = (partido.getJugador1() == this) ? partido.getJugador2().getPersonaje() : partido.getJugador1().getPersonaje();
                personaje.mover(-1, rival);
            });
            
            // Mover a la derecha (1 en el eje X)
            mapaAcciones.put(teclas.get(1), partido -> {
                Personaje rival = (partido.getJugador1() == this) ? partido.getJugador2().getPersonaje() : partido.getJugador1().getPersonaje();
                personaje.mover(1, rival);
            });
            
            // Saltar
            mapaAcciones.put(teclas.get(2), partido -> personaje.saltar());
            
            // Patear
            mapaAcciones.put(teclas.get(3), partido -> { personaje.patear(); partido.ejecutarPatada(this); });
            
            // Preparar el poder especial para que viaje con la siguiente patada que alcance el balón.
            mapaAcciones.put(teclas.get(4), partido -> activarPoder());
        }
    }

    @Override
    public void realizarAccion(String accion, Partido partido) {
        // Cero cascadas de condicionales: O(1) en búsqueda y ejecución
        AccionJugador estrategia = mapaAcciones.get(accion);
        if (estrategia != null) {
            estrategia.ejecutar(partido);
        }
    }

    public ArrayList<String> getTeclas(){
        return this.teclas;
    }
}
