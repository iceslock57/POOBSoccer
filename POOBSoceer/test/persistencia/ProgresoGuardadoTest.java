package persistencia;

import Domain.*;
import Domain.PerfilJugador.Estadistica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de guardar y abrir solo los puntos y las estadísticas, sin un partido en curso. */
public class ProgresoGuardadoTest {

    private static final ArchivoPartida[] FORMATOS = {new ArchivoDat(), new ArchivoBits()};

    @TempDir
    File carpeta;

    private POOBSoccer juego;

    @BeforeEach
    public void preparar() {
        juego = new POOBSoccer();
        juego.getPerfil(1).agregarPuntos(1700);
        juego.getPerfil(1).mejorar("Rusia", Estadistica.SPEED);
        juego.getPerfil(2).agregarPuntos(600);
        juego.getPerfil(2).mejorar("Nigeria", Estadistica.VIDA);
    }

    private static void iniciarPartido(POOBSoccer juego) {
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_JUGADOR,
                CatalogoPersonajes.crear("Rusia", juego.getPerfil(1)), CatalogoPersonajes.crear("Japón", juego.getPerfil(2)));
    }

    private File guardado(ArchivoPartida formato) {
        File archivo = new File(carpeta, "progreso." + formato.getExtension());
        formato.guardar(juego, archivo);
        return archivo;
    }

    private POOBSoccer guardarYAbrir(ArchivoPartida formato) {
        POOBSoccer otro = new POOBSoccer();
        formato.abrir(otro, guardado(formato));
        return otro;
    }

    @Test
    public void conservaLosPuntosDeLosDos() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(formato);
            assertEquals(1200, abierto.getPerfil(1).getPuntos());
            assertEquals(100, abierto.getPerfil(2).getPuntos());
        }
    }

    @Test
    public void conservaLasEstadisticasDeCadaUno() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(formato);
            assertEquals(2, abierto.getPerfil(1).getNivel("Rusia", Estadistica.SPEED));
            assertEquals(1, abierto.getPerfil(1).getNivel("Nigeria", Estadistica.VIDA));
            assertEquals(2, abierto.getPerfil(2).getNivel("Nigeria", Estadistica.VIDA));
            assertEquals(1, abierto.getPerfil(2).getNivel("Rusia", Estadistica.SPEED));
            assertEquals(1000, abierto.getPerfil(1).getCostoSiguienteMejora("Rusia"), "El precio sigue donde iba");
        }
    }

    @Test
    public void unJuegoRecienAbiertoTambienSePuedeGuardar() {
        juego = new POOBSoccer();
        for (ArchivoPartida formato : FORMATOS) {
            assertEquals(0, guardarYAbrir(formato).getPerfil(1).getPuntos());
        }
    }

    @Test
    public void alAbrirNoQuedaNingunPartido() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(formato);
            assertNull(abierto.getPartidoActual());
            assertFalse(abierto.hayPartidoEnCurso());
            assertFalse(abierto.isPausa());
        }
    }

    @Test
    public void deUnPartidoTerminadoSeGuardanSusPuntos() {
        iniciarPartido(juego);
        juego.getPartidoActual().getMarcador()[0] = 1;
        juego.getPartidoActual().terminar();
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(formato);
            assertEquals(1300, abierto.getPerfil(1).getPuntos(), "Con el premio de ganar");
            assertEquals(125, abierto.getPerfil(2).getPuntos(), "Con el premio de perder");
            assertNull(abierto.getPartidoActual(), "El partido terminado no vuelve");
        }
    }

    @Test
    public void guardarNoCambiaElJuego() {
        iniciarPartido(juego);
        juego.getPartidoActual().terminar();
        Partido antes = juego.getPartidoActual();
        long puntos = juego.getPerfil(1).getPuntos();
        for (ArchivoPartida formato : FORMATOS) guardado(formato);
        assertSame(antes, juego.getPartidoActual());
        assertEquals(puntos, juego.getPerfil(1).getPuntos());
    }

    @Test
    public void abrirProgresoReemplazaElPartidoQueHabia() {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = guardado(formato);
            POOBSoccer otro = new POOBSoccer();
            iniciarPartido(otro);
            formato.abrir(otro, archivo);
            assertNull(otro.getPartidoActual());
            assertEquals(1200, otro.getPerfil(1).getPuntos());
        }
    }

    @Test
    public void despuesDeAbrirSePuedeJugarConLasMejoras() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(formato);
            iniciarPartido(abierto);
            assertEquals(2, abierto.getPartidoActual().getJugador1().getPersonaje().getVelocidad());
            abierto.getPartidoActual().terminar();
            assertEquals(1225, abierto.getPerfil(1).getPuntos(), "Los puntos nuevos se suman a los recuperados");
        }
    }

    @Test
    public void despuesDeAbrirSePuedeSeguirMejorando() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(formato);
            assertTrue(abierto.getPerfil(1).mejorar("Rusia", Estadistica.KICK));
            assertEquals(200, abierto.getPerfil(1).getPuntos());
        }
    }

    @Test
    public void conPartidoOcupaMasQueSinPartido() {
        for (ArchivoPartida formato : FORMATOS) {
            long sinPartido = guardado(formato).length();
            iniciarPartido(juego);
            assertTrue(guardado(formato).length() > sinPartido);
            juego.getPartidoActual().terminar();
        }
    }
}
