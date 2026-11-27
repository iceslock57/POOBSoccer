package tests;

import Domain.*;
import Domain.PerfilJugador.Estadistica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las mejoras: cada jugador tiene sus puntos y cada personaje sus propias estadísticas. */
public class EstadisticasTest {

    private PerfilJugador perfil;

    @BeforeEach
    public void preparar() {
        perfil = new PerfilJugador("jugador1", "Uno");
    }

    private void conPuntos(long puntos) {
        perfil.agregarPuntos(puntos);
    }

    // Niveles

    @Test
    public void todasLasEstadisticasEmpiezanEnUno() {
        for (String pais : CatalogoPersonajes.paises()) {
            for (Estadistica e : Estadistica.values()) assertEquals(1, perfil.getNivel(pais, e), pais + " " + e);
        }
    }

    @Test
    public void mejorarUnPersonajeNoCambiaALosDemas() {
        conPuntos(500);
        assertTrue(perfil.mejorar("Rusia", Estadistica.SPEED));
        assertEquals(2, perfil.getNivel("Rusia", Estadistica.SPEED));
        for (String pais : CatalogoPersonajes.paises()) {
            if (!pais.equals("Rusia")) assertEquals(1, perfil.getNivel(pais, Estadistica.SPEED), pais);
        }
    }

    @Test
    public void mejorarUnaEstadisticaNoCambiaLasOtras() {
        conPuntos(500);
        perfil.mejorar("Rusia", Estadistica.KICK);
        assertEquals(2, perfil.getNivel("Rusia", Estadistica.KICK));
        assertEquals(1, perfil.getNivel("Rusia", Estadistica.SPEED));
        assertEquals(1, perfil.getNivel("Rusia", Estadistica.JUMP));
        assertEquals(1, perfil.getNivel("Rusia", Estadistica.VIDA));
    }

    @Test
    public void elMismoPaisDelOtroJugadorNoCambia() {
        PerfilJugador otro = new PerfilJugador("jugador2", "Dos");
        conPuntos(500);
        perfil.mejorar("Rusia", Estadistica.VIDA);
        assertEquals(1, otro.getNivel("Rusia", Estadistica.VIDA));
        assertEquals(0, otro.getPuntos());
    }

    @Test
    public void ningunaEstadisticaPasaDeDiez() {
        conPuntos(Long.MAX_VALUE / 2);
        for (int i = 0; i < 9; i++) assertTrue(perfil.mejorar("Japón", Estadistica.JUMP));
        long puntos = perfil.getPuntos();
        assertFalse(perfil.mejorar("Japón", Estadistica.JUMP));
        assertEquals(10, perfil.getNivel("Japón", Estadistica.JUMP));
        assertEquals(puntos, perfil.getPuntos(), "No cobra una mejora que no se hizo");
    }

    // Precios

    @Test
    public void elPrecioSeDuplicaConCadaMejora() {
        conPuntos(100000);
        long[] precios = {500, 1000, 2000, 4000};
        Estadistica[] compras = {Estadistica.SPEED, Estadistica.KICK, Estadistica.SPEED, Estadistica.VIDA};
        for (int i = 0; i < precios.length; i++) {
            assertEquals(precios[i], perfil.getCostoSiguienteMejora("Rusia"));
            long antes = perfil.getPuntos();
            perfil.mejorar("Rusia", compras[i]);
            assertEquals(antes - precios[i], perfil.getPuntos());
        }
    }

    @Test
    public void cadaPersonajeTieneSuPropioPrecio() {
        conPuntos(500);
        perfil.mejorar("Rusia", Estadistica.SPEED);
        assertEquals(1000, perfil.getCostoSiguienteMejora("Rusia"));
        assertEquals(500, perfil.getCostoSiguienteMejora("Japón"));
    }

    @Test
    public void sinPuntosSuficientesNoSeMejora() {
        conPuntos(499);
        assertFalse(perfil.puedeMejorar("Rusia", Estadistica.SPEED));
        assertFalse(perfil.mejorar("Rusia", Estadistica.SPEED));
        assertEquals(1, perfil.getNivel("Rusia", Estadistica.SPEED));
        assertEquals(499, perfil.getPuntos());
    }

    @Test
    public void unaEstadisticaNulaNoSeMejora() {
        conPuntos(500);
        assertFalse(perfil.mejorar("Rusia", null));
        assertEquals(500, perfil.getPuntos());
    }

    @Test
    public void losPuntosSonDelJugadorYSirvenParaCualquierPersonaje() {
        conPuntos(1000);
        assertTrue(perfil.mejorar("Rusia", Estadistica.SPEED));
        assertTrue(perfil.mejorar("Nigeria", Estadistica.SPEED));
        assertEquals(0, perfil.getPuntos());
    }

    @Test
    public void losPuntosNegativosNoCuentan() {
        conPuntos(-50);
        assertEquals(0, perfil.getPuntos());
    }

    @Test
    public void unNombreVacioNoReemplazaAlAnterior() {
        perfil.setNombre("  ");
        perfil.setNombre(null);
        assertEquals("Uno", perfil.getNombre());
        perfil.setNombre(" Ana ");
        assertEquals("Ana", perfil.getNombre());
    }

    // Efecto en el personaje

    @Test
    public void elPersonajeSeCreaConSusMejoras() {
        conPuntos(10000);
        perfil.mejorar("Rusia", Estadistica.SPEED);
        perfil.mejorar("Rusia", Estadistica.JUMP);
        perfil.mejorar("Rusia", Estadistica.KICK);
        perfil.mejorar("Rusia", Estadistica.VIDA);
        Personaje rusia = CatalogoPersonajes.crear("Rusia", perfil);
        assertEquals(2, rusia.getVelocidad());
        assertEquals(2, rusia.getSalto());
        assertEquals(1.1, rusia.getMultiplicadorKick(), 1e-6);
        assertEquals(110, rusia.getVidaMaxima());

        Personaje japon = CatalogoPersonajes.crear("Japón", perfil);
        assertEquals(1, japon.getVelocidad());
        assertEquals(100, japon.getVidaMaxima());
    }

    // Progreso entre partidos

    @Test
    public void losPuntosSeConservanEntreUnPartidoYOtro() {
        POOBSoccer juego = new POOBSoccer();
        for (int i = 0; i < 3; i++) {
            juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_JUGADOR,
                    CatalogoPersonajes.crear("Rusia"), CatalogoPersonajes.crear("Japón"));
            juego.getPartidoActual().getMarcador()[0] = 1;
            juego.getPartidoActual().terminar();
        }
        assertEquals(300, juego.getPerfil(1).getPuntos(), "Ganó tres partidos");
        assertEquals(75, juego.getPerfil(2).getPuntos(), "Perdió tres partidos");
    }

    @Test
    public void lasMejorasSeConservanEnElSiguientePartido() {
        POOBSoccer juego = new POOBSoccer();
        juego.getPerfil(1).agregarPuntos(500);
        juego.getPerfil(1).mejorar("Rusia", Estadistica.VIDA);
        for (int i = 0; i < 2; i++) {
            juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_JUGADOR,
                    CatalogoPersonajes.crear("Rusia", juego.getPerfil(1)),
                    CatalogoPersonajes.crear("Rusia", juego.getPerfil(2)));
            assertEquals(110, juego.getPartidoActual().getJugador1().getPersonaje().getVidaMaxima());
            assertEquals(100, juego.getPartidoActual().getJugador2().getPersonaje().getVidaMaxima());
            juego.getPartidoActual().terminar();
        }
    }

    @Test
    public void contraLaMaquinaSoloLaPersonaGanaPuntos() {
        POOBSoccer juego = new POOBSoccer();
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, CatalogoPersonajes.crear("Rusia"),
                CatalogoPersonajes.crear("Japón"), 5, 1000, 650, "Torpe");
        juego.getPartidoActual().getMarcador()[0] = 1;
        juego.getPartidoActual().terminar();
        assertEquals(100, juego.getPerfil(1).getPuntos());
        assertEquals(0, juego.getPerfil(2).getPuntos(), "El perfil 2 es de la otra persona, no de la máquina");
    }

    // Archivo

    @Test
    public void elArchivoGuardaLasMejorasDeCadaPersonaje() throws Exception {
        Path archivo = Files.createTempDirectory("poobsoccer-mejoras-").resolve("perfiles.properties");
        RepositorioPerfiles repositorio = new RepositorioPerfiles(archivo);
        conPuntos(5000);
        perfil.mejorar("Corea del Sur", Estadistica.KICK);
        perfil.mejorar("Corea del Sur", Estadistica.KICK);
        perfil.mejorar("Japón", Estadistica.VIDA);
        repositorio.guardarPerfiles(perfil, new PerfilJugador("jugador2", "Dos"));

        PerfilJugador recuperado = repositorio.cargarPerfiles()[0];
        assertEquals(3, recuperado.getNivel("Corea del Sur", Estadistica.KICK));
        assertEquals(2, recuperado.getNivel("Japón", Estadistica.VIDA));
        assertEquals(1, recuperado.getNivel("Rusia", Estadistica.KICK));
        assertEquals(2000, recuperado.getCostoSiguienteMejora("Corea del Sur"), "El precio sigue donde iba");
        assertEquals(1, repositorio.cargarPerfiles()[1].getNivel("Corea del Sur", Estadistica.KICK));
    }

    @Test
    public void unArchivoConDatosRarosNoDaniaElPerfil() throws Exception {
        Path archivo = Files.createTempDirectory("poobsoccer-raro-").resolve("perfiles.properties");
        Files.writeString(archivo, "jugador1.puntos=muchos\njugador1.Rusia.speed=99\njugador1.Rusia.kick=hola\n");
        PerfilJugador recuperado = new RepositorioPerfiles(archivo).cargarPerfiles()[0];
        assertEquals(0, recuperado.getPuntos());
        assertEquals(10, recuperado.getNivel("Rusia", Estadistica.SPEED), "Se limita al máximo");
        assertEquals(1, recuperado.getNivel("Rusia", Estadistica.KICK));
    }
}
