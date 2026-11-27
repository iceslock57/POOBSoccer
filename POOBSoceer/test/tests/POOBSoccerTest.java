package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la clase que inicia los partidos y controla la pausa. */
public class POOBSoccerTest {

    private POOBSoccer juego;
    private Personaje rusia, japon;

    @BeforeEach
    public void preparar() {
        juego = new POOBSoccer();
        rusia = CatalogoPersonajes.crear("Rusia");
        japon = CatalogoPersonajes.crear("Japón");
    }

    @Test
    public void alComienzoNoHayPartidoNiPausa() {
        assertNull(juego.getPartidoActual());
        assertFalse(juego.isPausa());
        juego.pausarJuego();
        juego.actualizar();
        assertFalse(juego.isPausa(), "Sin partido no hay nada que pausar");
    }

    @Test
    public void iniciaPartidosPorTiempoYPorGoles() {
        juego.iniciarPartido("Goles", "PvsP", rusia, japon);
        assertInstanceOf(PartidoPorGoles.class, juego.getPartidoActual());
        juego.iniciarPartido("Tiempo", "PvsP", rusia, japon, 60);
        assertInstanceOf(PartidoPorTiempo.class, juego.getPartidoActual());
    }

    @Test
    public void iniciaPartidosConElTamanoDeCanchaElegido() {
        juego.iniciarPartido("Goles", "PvsP", rusia, japon, 3, 1200, 750);
        assertInstanceOf(PartidoPorGoles.class, juego.getPartidoActual());
        assertEquals(1200, juego.getPartidoActual().getCancha().getAncho());
        juego.iniciarPartido("Tiempo", "PvsP", rusia, japon, 60, 800, 550);
        assertInstanceOf(PartidoPorTiempo.class, juego.getPartidoActual());
        assertSame(rusia, juego.getPartidoActual().getJugador1().getPersonaje());
        assertSame(japon, juego.getPartidoActual().getJugador2().getPersonaje());
    }

    @Test
    public void rechazaDatosQueNoSirvenParaIniciar() {
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Goles", "PvsP", null, japon));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Goles", "PvsP", rusia, null));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Goles", "Otra", rusia, japon));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Otro", "PvsP", rusia, japon));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Goles", "PvsP", null, japon, 5, 1000, 650));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Goles", "PvsP", rusia, null, 5, 1000, 650));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Goles", "Otra", rusia, japon, 5, 1000, 650));
        assertThrows(ExcepcionJuego.class, () -> juego.iniciarPartido("Otro", "PvsP", rusia, japon, 5, 1000, 650));
        assertNull(juego.getPartidoActual(), "Ningún intento fallido deja un partido a medias");
    }

    @Test
    public void enPausaElPartidoNoAvanza() {
        juego.iniciarPartido("Goles", "PvsP", rusia, japon);
        Balon balon = juego.getPartidoActual().getBalon();
        double y = balon.getPosY();
        juego.pausarJuego();
        juego.actualizar();
        assertTrue(juego.isPausa());
        assertEquals(y, balon.getPosY(), "El balón no cae en pausa");
        juego.reanudarJuego();
        juego.actualizar();
        assertFalse(juego.isPausa());
        assertTrue(balon.getPosY() > y, "Al reanudar vuelve a caer");
    }

    @Test
    public void losJugadoresRespondenASusTeclas() {
        juego.iniciarPartido("Goles", "PvsP", rusia, japon, 5, 1000, 650);
        Partido partido = juego.getPartidoActual();
        double x1 = rusia.getPosX(), x2 = japon.getPosX();
        partido.getJugador1().realizarAccion("D", partido);
        partido.getJugador2().realizarAccion("LEFT", partido);
        assertEquals(x1 + Personaje.PASO_BASE, rusia.getPosX(), 1e-6, "Avanza a la derecha");
        assertEquals(x2 - Personaje.PASO_BASE, japon.getPosX(), 1e-6, "Avanza a la izquierda");
        partido.getJugador1().realizarAccion("A", partido);
        partido.getJugador2().realizarAccion("RIGHT", partido);
        assertEquals(x1, rusia.getPosX(), 1e-6, "Vuelve a su lugar");
        assertEquals(x2, japon.getPosX(), 1e-6, "Vuelve a su lugar");

        double y = rusia.getPosY();
        partido.getJugador1().realizarAccion("W", partido);
        juego.actualizar();
        assertTrue(rusia.getPosY() < y, "Salta");
    }

    @Test
    public void contraLaMaquinaElSegundoJugadorLoControlaElComputador() {
        for (String perfil : CatalogoMaquinas.perfiles()) {
            juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, rusia, japon, 3, 1000, 650, perfil);
            Partido partido = juego.getPartidoActual();
            assertInstanceOf(JugadorHumano.class, partido.getJugador1());
            JugadorMaquina maquina = assertInstanceOf(JugadorMaquina.class, partido.getJugador2());
            assertEquals(perfil, maquina.getPerfilMaquina());
        }
    }

    @Test
    public void contraLaMaquinaHayQueElegirUnPerfilValido() {
        assertThrows(ExcepcionJuego.class, () ->
                juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, rusia, japon));
        assertThrows(ExcepcionJuego.class, () ->
                juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, rusia, japon, 3, 1000, 650, "Genio"));
        assertNull(juego.getPartidoActual());
    }

    @Test
    public void entreDosPersonasElPerfilNoImporta() {
        juego.iniciarPartido("Tiempo", POOBSoccer.JUGADOR_VS_JUGADOR, rusia, japon, 2, 1000, 650, "Experta");
        assertInstanceOf(JugadorHumano.class, juego.getPartidoActual().getJugador2());
    }

    @Test
    public void laMaquinaTambienSeDetieneEnPausa() {
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, rusia, japon, 3, 1000, 650, "Ofensiva");
        double x = japon.getPosX();
        juego.pausarJuego();
        for (int i = 0; i < 30; i++) juego.actualizar();
        assertEquals(x, japon.getPosX(), "En pausa no se mueve");
        juego.reanudarJuego();
        for (int i = 0; i < 30; i++) juego.actualizar();
        assertTrue(japon.getPosX() < x, "Al reanudar va por el balón");
    }

    // Bitácora

    /** Cuántas veces aparece un texto en el archivo de registro; cuenta antes y después porque el archivo se acumula. */
    private static int vecesEnLaBitacora(String texto) throws Exception {
        java.nio.file.Path archivo = java.nio.file.Path.of(System.getProperty("poobsoccer.log", "poobsoccer.log"));
        if (!java.nio.file.Files.exists(archivo)) return 0;
        String contenido = java.nio.file.Files.readString(archivo, java.nio.charset.StandardCharsets.ISO_8859_1);
        return contenido.split(java.util.regex.Pattern.quote(texto), -1).length - 1;
    }

    @Test
    public void laBitacoraAnotaCuandoEmpiezaUnPartidoYCadaGol() throws Exception {
        int empiezan = vecesEnLaBitacora("Empieza un partido por Goles (PvsP): Rusia contra");
        int goles = vecesEnLaBitacora("Gol. El marcador va 1 a 0");
        juego.iniciarPartido("Goles", "PvsP", rusia, japon);
        Partido p = juego.getPartidoActual();
        Arco derecho = p.getCancha().getArcoDerecho();
        p.getBalon().resetearPosicion(derecho.getPosX() - 5, derecho.getPosY() + derecho.getAlto() / 2.0);
        p.getBalon().aplicarFuerza(12, 0);
        juego.actualizar();
        assertEquals(1, p.getMarcador()[0], "Precondición: hubo gol");
        assertEquals(empiezan + 1, vecesEnLaBitacora("Empieza un partido por Goles (PvsP): Rusia contra"));
        assertEquals(goles + 1, vecesEnLaBitacora("Gol. El marcador va 1 a 0"));
    }

    @Test
    public void laBitacoraAnotaLaPausaSoloCuandoCambia() throws Exception {
        int pausas = vecesEnLaBitacora("El partido se pausa");
        juego.pausarJuego();
        assertEquals(pausas, vecesEnLaBitacora("El partido se pausa"), "Sin partido no hay pausa que anotar");
        juego.iniciarPartido("Goles", "PvsP", rusia, japon);
        juego.pausarJuego();
        assertEquals(pausas + 1, vecesEnLaBitacora("El partido se pausa"));
    }
}
