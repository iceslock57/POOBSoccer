package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del jugador que controla el computador y del catálogo de perfiles. */
public class JugadorMaquinaTest {

    private static final double DELTA = 1e-6;

    private Jugador persona;
    private JugadorMaquina maquina;
    private Partido partido;
    private EstrategiaFija estrategia;

    /** Estrategia que siempre responde lo mismo, para probar al jugador sin depender de un perfil. */
    private static final class EstrategiaFija implements EstrategiaMaquina {
        private Set<Accion> respuesta = EnumSet.noneOf(Accion.class);
        private int consultas;

        @Override
        public Set<Accion> decidir(SituacionPartido situacion) {
            consultas++;
            return respuesta;
        }

        @Override
        public int cuadrosEntreDecisiones() {
            return 5;
        }

        @Override
        public String descripcion() {
            return "Siempre igual";
        }
    }

    @BeforeEach
    public void preparar() {
        estrategia = new EstrategiaFija();
        persona = new JugadorHumano("Uno", CatalogoPersonajes.crear("Camerún"), new String[]{"A", "D", "W", "S", "Q"});
        maquina = new JugadorMaquina("Dos", CatalogoPersonajes.crear("Japón"), "Fija", estrategia);
        partido = new PartidoPorGoles(persona, maquina, 5);
    }

    private void avanzar(int cuadros) {
        for (int i = 0; i < cuadros; i++) partido.actualizarEstado();
    }

    // Catálogo

    @Test
    public void elCatalogoOfreceLosPerfilesDelProyecto() {
        assertEquals(Arrays.asList("Torpe", "Ofensiva", "Defensiva", "Experta"), CatalogoMaquinas.perfiles());
    }

    @Test
    public void cadaPerfilTieneSuPropiaEstrategia() {
        assertInstanceOf(EstrategiaTorpe.class, CatalogoMaquinas.crear("Torpe"));
        assertInstanceOf(EstrategiaOfensiva.class, CatalogoMaquinas.crear("Ofensiva"));
        assertInstanceOf(EstrategiaDefensiva.class, CatalogoMaquinas.crear("Defensiva"));
        assertInstanceOf(EstrategiaExperta.class, CatalogoMaquinas.crear("Experta"));
    }

    @Test
    public void cadaPerfilSeExplicaConUnaFraseDistinta() {
        long distintas = CatalogoMaquinas.perfiles().stream()
                .map(perfil -> CatalogoMaquinas.crear(perfil).descripcion()).distinct().count();
        assertEquals(CatalogoMaquinas.perfiles().size(), distintas);
    }

    @Test
    public void unPerfilQueNoExisteSeRechaza() {
        ExcepcionJuego error = assertThrows(ExcepcionJuego.class, () -> CatalogoMaquinas.crear("Genio"));
        assertTrue(error.getMessage().contains("Genio"), "El mensaje dice cuál falló");
        assertThrows(ExcepcionJuego.class, () -> new JugadorMaquina("Dos", CatalogoPersonajes.crear("Japón"), null));
    }

    @Test
    public void modificarLaListaNoCambiaElCatalogo() {
        CatalogoMaquinas.perfiles().clear();
        assertEquals(4, CatalogoMaquinas.perfiles().size());
    }

    // Turnos

    @Test
    public void caminaEnTodosLosCuadrosPeroDecideCadaCierto() {
        estrategia.respuesta = EnumSet.of(Accion.IZQUIERDA);
        double x = maquina.getPersonaje().getPosX();
        avanzar(10);
        assertEquals(2, estrategia.consultas, "Decide dos veces en diez cuadros");
        assertEquals(x - 10 * Personaje.PASO_BASE, maquina.getPersonaje().getPosX(), DELTA, "Camina sin detenerse");
    }

    @Test
    public void caminaHaciaLaDerecha() {
        estrategia.respuesta = EnumSet.of(Accion.DERECHA);
        double x = maquina.getPersonaje().getPosX();
        avanzar(1);
        assertEquals(x + Personaje.PASO_BASE, maquina.getPersonaje().getPosX(), DELTA);
    }

    @Test
    public void sinAccionesSeQuedaQuieta() {
        double x = maquina.getPersonaje().getPosX();
        avanzar(20);
        assertEquals(x, maquina.getPersonaje().getPosX(), DELTA);
    }

    @Test
    public void salta() {
        estrategia.respuesta = EnumSet.of(Accion.SALTAR);
        double y = maquina.getPersonaje().getPosY();
        avanzar(5);
        assertTrue(maquina.getPersonaje().getPosY() < y);
    }

    @Test
    public void preparaSuPoderSoloConLaBarraLlena() {
        estrategia.respuesta = EnumSet.of(Accion.PODER);
        avanzar(1);
        assertFalse(maquina.tienePoderPreparado(), "Barra vacía");
        maquina.cargarPoder(100);
        avanzar(5);
        assertTrue(maquina.tienePoderPreparado(), "Barra llena");
    }

    @Test
    public void lanzaSuPoderAlPatearElBalon() {
        maquina.cargarPoder(100);
        estrategia.respuesta = EnumSet.of(Accion.PODER, Accion.PATEAR);
        partido.getBalon().resetearPosicion(maquina.getPersonaje().getPosX() + 20, maquina.getPersonaje().getPosY() + 20);
        avanzar(1);
        assertTrue(partido.getBalon().isDisparoEspecial());
        assertEquals(2, partido.getBalon().getPropietarioDisparo());
        assertTrue(partido.getBalon().getVelX() < 0, "Va hacia el arco contrario");
    }

    @Test
    public void descansaEntreUnaPatadaYOtra() {
        persona.getPersonaje().resetearPosicion(maquina.getPersonaje().getPosX() - Personaje.ANCHO - 5, 470);
        partido.getBalon().resetearPosicion(100, 60);
        estrategia.respuesta = EnumSet.of(Accion.PATEAR);
        avanzar(30);
        assertEquals(90, persona.getPersonaje().getVidaActual(), "Un solo golpe en medio segundo");
        avanzar(5);
        assertEquals(80, persona.getPersonaje().getVidaActual(), "Luego vuelve a golpear");
    }

    @Test
    public void aturdidaNoPuedeHacerNada() {
        estrategia.respuesta = EnumSet.allOf(Accion.class);
        maquina.getPersonaje().aturdir(3);
        double x = maquina.getPersonaje().getPosX(), y = maquina.getPersonaje().getPosY();
        avanzar(30);
        assertEquals(x, maquina.getPersonaje().getPosX(), DELTA);
        assertEquals(y, maquina.getPersonaje().getPosY(), DELTA);
        assertFalse(maquina.getPersonaje().isPateando());
    }

    @Test
    public void noJuegaSiElPartidoYaTermino() {
        partido.terminar();
        avanzar(10);
        assertEquals(0, estrategia.consultas);
    }

    // Acciones por nombre

    @Test
    public void elTecladoNoLaControla() {
        double x = maquina.getPersonaje().getPosX();
        for (String tecla : new String[]{"LEFT", "RIGHT", "UP", "M", "N", null}) maquina.realizarAccion(tecla, partido);
        assertEquals(x, maquina.getPersonaje().getPosX(), DELTA);
        assertFalse(maquina.getPersonaje().isPateando());
    }

    @Test
    public void entiendeSusAccionesPorNombre() {
        double x = maquina.getPersonaje().getPosX();
        maquina.realizarAccion("IZQUIERDA", partido);
        assertEquals(x - Personaje.PASO_BASE, maquina.getPersonaje().getPosX(), DELTA);
        maquina.realizarAccion("PATEAR", partido);
        assertTrue(maquina.getPersonaje().isPateando());
    }

    @Test
    public void tambienPuedeSerElPrimerJugador() {
        EstrategiaFija fija = new EstrategiaFija();
        fija.respuesta = EnumSet.of(Accion.DERECHA);
        JugadorMaquina primera = new JugadorMaquina("Uno", CatalogoPersonajes.crear("Rusia"), "Fija", fija);
        partido = new PartidoPorGoles(primera, maquina, 5);
        double x = primera.getPersonaje().getPosX();
        avanzar(1);
        assertEquals(x + Personaje.PASO_BASE, primera.getPersonaje().getPosX(), DELTA);
    }

    @Test
    public void muestraSuPerfilEnElNombre() {
        assertEquals("Fija", maquina.getPerfilMaquina());
        assertEquals("Japón (Máquina Fija)", maquina.getNombreVisible());
        assertEquals("Camerún", persona.getNombreVisible(), "Una persona solo muestra su país");
    }

    // Partidos completos

    @Test
    public void todosLosPerfilesPuedenJugarUnPartidoEntero() {
        for (String uno : CatalogoMaquinas.perfiles()) {
            for (String dos : CatalogoMaquinas.perfiles()) {
                Partido juego = new PartidoPorTiempo(
                        new JugadorMaquina("Uno", CatalogoPersonajes.crear("Estados Unidos"), uno),
                        new JugadorMaquina("Dos", CatalogoPersonajes.crear("Rusia"), dos), 1);
                assertDoesNotThrow(() -> {
                    for (int i = 0; i < 4000; i++) juego.actualizarEstado();
                }, uno + " contra " + dos);
            }
        }
    }

    @Test
    public void laExpertaLeGanaAUnRivalQueNoSeMueve() {
        maquina = new JugadorMaquina("Dos", CatalogoPersonajes.crear("Japón"), "Experta");
        partido = new PartidoPorGoles(persona, maquina, 3);
        for (int i = 0; i < 60 * 120 && !partido.isTerminado(); i++) partido.actualizarEstado();
        assertSame(maquina, partido.getGanador());
    }
}
