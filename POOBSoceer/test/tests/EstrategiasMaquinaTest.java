package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de lo que decide cada perfil de máquina según lo que ve en la cancha. */
public class EstrategiasMaquinaTest {

    private Jugador persona, maquina;
    private Partido partido;
    private Balon balon;

    /** La máquina siempre es el jugador 2: está a la derecha y ataca hacia la izquierda. */
    @BeforeEach
    public void preparar() {
        partidoPorGoles();
    }

    private void partidoPorGoles() {
        crearJugadores();
        partido = new PartidoPorGoles(persona, maquina, 5);
        balon = partido.getBalon();
    }

    /** Partido por tiempo en los últimos segundos, con la máquina ganando. */
    private void ganandoAlFinal() {
        crearJugadores();
        partido = new PartidoPorTiempo(persona, maquina, 1) {
            @Override
            public boolean estaPorTerminar() {
                return true;
            }
        };
        balon = partido.getBalon();
        maquina.anotarGol();
    }

    private void crearJugadores() {
        persona = new JugadorHumano("Uno", CatalogoPersonajes.crear("Camerún"), new String[]{"A", "D", "W", "S", "Q"});
        maquina = new JugadorMaquina("Dos", CatalogoPersonajes.crear("Japón"), "Experta");
    }

    private Set<Accion> decide(EstrategiaMaquina estrategia) {
        return estrategia.decidir(new SituacionPartido(maquina, partido));
    }

    private void balonEn(double x, double y) {
        balon.resetearPosicion(x, y);
    }

    private void ponerAMaquinaEn(double x) {
        maquina.getPersonaje().resetearPosicion(x, partido.getCancha().getSuelo() - Personaje.ALTO);
    }

    // Torpe

    @Test
    public void laTorpeNoSeFijaEnElBalon() {
        balonEn(100, 530);
        Set<Accion> lejos = new EstrategiaTorpe(new Random(7)).decidir(new SituacionPartido(maquina, partido));
        balonEn(760, 530);
        Set<Accion> cerca = new EstrategiaTorpe(new Random(7)).decidir(new SituacionPartido(maquina, partido));
        assertEquals(lejos, cerca, "Con el mismo azar decide lo mismo esté donde esté el balón");
    }

    @Test
    public void laTorpeTerminaUsandoTodosSusMovimientos() {
        EstrategiaMaquina torpe = new EstrategiaTorpe(new Random(1));
        Set<Accion> usadas = EnumSet.noneOf(Accion.class);
        for (int i = 0; i < 200; i++) usadas.addAll(decide(torpe));
        assertEquals(EnumSet.allOf(Accion.class), usadas);
    }

    @Test
    public void laTorpeNuncaCaminaALosDosLadosALaVez() {
        EstrategiaMaquina torpe = new EstrategiaTorpe();
        for (int i = 0; i < 200; i++) {
            assertFalse(decide(torpe).containsAll(EnumSet.of(Accion.IZQUIERDA, Accion.DERECHA)));
        }
    }

    // Ofensiva

    @Test
    public void laOfensivaPersigueElBalonPorTodaLaCancha() {
        balonEn(100, 530);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.IZQUIERDA), "Balón en la mitad contraria");
        balonEn(950, 530);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.DERECHA), "Balón detrás de ella");
    }

    @Test
    public void laOfensivaPateaCuandoAlcanzaElBalon() {
        balonEn(730, 520);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.PATEAR));
        balonEn(500, 520);
        assertFalse(decide(new EstrategiaOfensiva()).contains(Accion.PATEAR), "Lejos no gasta la patada");
    }

    @Test
    public void laOfensivaSaltaSiElBalonPasaPorEncima() {
        balonEn(766, 400);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.SALTAR));
        balonEn(766, 100);
        assertFalse(decide(new EstrategiaOfensiva()).contains(Accion.SALTAR), "Está demasiado alto");
    }

    @Test
    public void laOfensivaUsaSuPoderApenasLoTiene() {
        balonEn(100, 530);
        assertFalse(decide(new EstrategiaOfensiva()).contains(Accion.PODER), "Barra vacía");
        maquina.cargarPoder(100);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.PODER), "Barra llena");
    }

    @Test
    public void laOfensivaGolpeaAlRivalSoloSiElBalonEstaLejos() {
        persona.getPersonaje().resetearPosicion(740 - Personaje.ANCHO - 5, 470);
        balonEn(500, 60);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.PATEAR), "Balón lejos: golpea al rival");
        balonEn(680, 420);
        assertFalse(decide(new EstrategiaOfensiva()).contains(Accion.PATEAR), "Balón cerca: guarda la patada para él");
    }

    @Test
    public void laOfensivaSeAdelantaAlMovimientoDelBalon() {
        balonEn(740, 530);
        balon.aplicarFuerza(12, 0);
        assertTrue(decide(new EstrategiaOfensiva()).contains(Accion.DERECHA), "Va a donde estará y no a donde está");
    }

    // Defensiva

    @Test
    public void laDefensivaCuidaElArcoSiElBalonEstaLejos() {
        balonEn(100, 530);
        assertEquals(EnumSet.of(Accion.DERECHA), decide(new EstrategiaDefensiva()), "Vuelve hacia su arco");
        ponerAMaquinaEn(1000 - Cancha.ANCHO_ARCO - Personaje.ANCHO - Personaje.ANCHO / 2.0);
        assertTrue(decide(new EstrategiaDefensiva()).isEmpty(), "Ya en su puesto espera quieta");
    }

    @Test
    public void laDefensivaSaleCuandoElBalonEntraEnSuMitad() {
        balonEn(600, 530);
        assertTrue(decide(new EstrategiaDefensiva()).contains(Accion.IZQUIERDA));
    }

    @Test
    public void laDefensivaGuardaElPoderHastaQueHayPeligro() {
        maquina.cargarPoder(100);
        balonEn(100, 530);
        assertFalse(decide(new EstrategiaDefensiva()).contains(Accion.PODER), "Sin peligro lo guarda");
        balonEn(600, 530);
        assertTrue(decide(new EstrategiaDefensiva()).contains(Accion.PODER), "Con peligro lo usa");
    }

    @Test
    public void laDefensivaDespejaConPatadasYSaltos() {
        balonEn(730, 520);
        assertTrue(decide(new EstrategiaDefensiva()).contains(Accion.PATEAR), "Balón al pie");
        balonEn(766, 400);
        assertTrue(decide(new EstrategiaDefensiva()).contains(Accion.SALTAR), "Balón por arriba");
    }

    // Experta

    @Test
    public void laExpertaAtacaCuandoNoHayPeligro() {
        balonEn(200, 530);
        assertTrue(decide(new EstrategiaExperta()).contains(Accion.IZQUIERDA), "Va por el balón y no a su arco");
    }

    @Test
    public void laExpertaCuidaLaVentajaAlFinal() {
        ganandoAlFinal();
        balonEn(200, 530);
        assertTrue(decide(new EstrategiaExperta()).contains(Accion.DERECHA), "Gana y queda poco tiempo: vuelve a su arco");
    }

    @Test
    public void laExpertaNoSeEncierraSiVaGanandoPeroFaltaMucho() {
        balonEn(200, 530);
        maquina.anotarGol();
        assertTrue(decide(new EstrategiaExperta()).contains(Accion.IZQUIERDA));
    }

    @Test
    public void laExpertaAprovechaAlRivalIndefenso() {
        ganandoAlFinal();
        balonEn(200, 530);
        persona.getPersonaje().aturdir(3);
        assertTrue(decide(new EstrategiaExperta()).contains(Accion.IZQUIERDA), "Con el rival aturdido siempre ataca");
    }

    @Test
    public void laExpertaEsquivaElTiroSoloSiTienePocaVida() {
        balonEn(650, 510);
        balon.aplicarDisparoEspecial(12, 0, 1);
        assertFalse(decide(new EstrategiaExperta()).contains(Accion.SALTAR), "Con vida de sobra lo enfrenta");
        maquina.getPersonaje().recibirDanio(80);
        assertTrue(decide(new EstrategiaExperta()).contains(Accion.SALTAR), "Con poca vida lo salta");
    }

    @Test
    public void laExpertaUsaElPoderTambienCuandoDefiende() {
        ganandoAlFinal();
        maquina.cargarPoder(100);
        balonEn(200, 530);
        assertTrue(decide(new EstrategiaExperta()).contains(Accion.PODER));
    }

    @Test
    public void laExpertaReaccionaMasRapidoQueLasDemas() {
        int experta = new EstrategiaExperta().cuadrosEntreDecisiones();
        for (String perfil : new String[]{"Torpe", "Ofensiva", "Defensiva"}) {
            assertTrue(experta < CatalogoMaquinas.crear(perfil).cuadrosEntreDecisiones(), perfil);
        }
    }
}
