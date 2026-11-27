package persistencia;

import Domain.*;
import Domain.PerfilJugador.Estadistica;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de que una partida guardada se recupera completa, tanto en .dat como en bits. */
public class PartidaGuardadaTest {

    private static final ArchivoPartida[] FORMATOS = {new ArchivoDat(), new ArchivoBits()};

    @TempDir
    File carpeta;

    private static POOBSoccer juego(String tipo, String pais1, String pais2, String maquina) {
        POOBSoccer juego = new POOBSoccer();
        iniciar(juego, tipo, pais1, pais2, maquina);
        return juego;
    }

    private static void iniciar(POOBSoccer juego, String tipo, String pais1, String pais2, String maquina) {
        juego.iniciarPartido(tipo, maquina == null ? POOBSoccer.JUGADOR_VS_JUGADOR : POOBSoccer.JUGADOR_VS_MAQUINA,
                CatalogoPersonajes.crear(pais1, juego.getPerfil(1)),
                CatalogoPersonajes.crear(pais2, maquina == null ? juego.getPerfil(2) : null), 3, 1200, 750, maquina);
    }

    private static void avanzar(POOBSoccer juego, int cuadros) {
        for (int i = 0; i < cuadros; i++) juego.actualizar();
    }

    /** Guarda la partida y la abre en un juego nuevo. */
    private POOBSoccer guardarYAbrir(POOBSoccer juego, ArchivoPartida formato) {
        File archivo = new File(carpeta, "partida." + formato.getExtension());
        formato.guardar(juego, archivo);
        POOBSoccer otro = new POOBSoccer();
        formato.abrir(otro, archivo);
        return otro;
    }

    private static Personaje personaje(POOBSoccer juego, int numero) {
        Partido p = juego.getPartidoActual();
        return (numero == 1 ? p.getJugador1() : p.getJugador2()).getPersonaje();
    }

    /** Juega los dos a la par y comprueba que les pase exactamente lo mismo. */
    private static void assertSiguenIguales(POOBSoccer a, POOBSoccer b, int cuadros) {
        for (int i = 0; i < cuadros; i++) {
            a.actualizar();
            b.actualizar();
            Balon ba = a.getPartidoActual().getBalon(), bb = b.getPartidoActual().getBalon();
            assertEquals(ba.getPosX(), bb.getPosX(), "Balón en x, cuadro " + i);
            assertEquals(ba.getPosY(), bb.getPosY(), "Balón en y, cuadro " + i);
            for (int n = 1; n <= 2; n++) {
                assertEquals(personaje(a, n).getPosX(), personaje(b, n).getPosX(), "Personaje " + n + ", cuadro " + i);
                assertEquals(personaje(a, n).getPosY(), personaje(b, n).getPosY(), "Personaje " + n + ", cuadro " + i);
                assertEquals(personaje(a, n).getVidaActual(), personaje(b, n).getVidaActual(), "Vida " + n + ", cuadro " + i);
            }
            assertEquals(a.getPartidoActual().getBalonesFalsos().size(), b.getPartidoActual().getBalonesFalsos().size());
            assertArrayEquals(a.getPartidoActual().getMarcador(), b.getPartidoActual().getMarcador());
        }
    }

    private static void lanzarPoder(POOBSoccer juego) {
        Partido p = juego.getPartidoActual();
        Jugador j = p.getJugador1();
        j.cargarPoder(100);
        j.realizarAccion("Q", p);
        p.getBalon().resetearPosicion(j.getPersonaje().getPosX() + 20, j.getPersonaje().getPosY() + 20);
        j.realizarAccion("S", p);
        juego.actualizar();
    }

    // Configuración

    @Test
    public void conservaLosPersonajesYLaCancha() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(juego("Goles", "Nigeria", "Japón", null), formato);
            assertEquals("Nigeria", personaje(abierto, 1).getPais());
            assertEquals("Japón", personaje(abierto, 2).getPais());
            assertEquals(1200, abierto.getPartidoActual().getCancha().getAncho());
            assertEquals(750, abierto.getPartidoActual().getCancha().getAlto());
        }
    }

    @Test
    public void conservaElTipoDePartidoYSuMeta() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(juego("Goles", "Rusia", "Japón", null), formato);
            PartidoPorGoles partido = assertInstanceOf(PartidoPorGoles.class, abierto.getPartidoActual());
            assertEquals(3, partido.getGolesObjetivo());
            assertEquals("Goles", abierto.getTipoPartido());
        }
    }

    @Test
    public void conservaContraQuienSeJuega() {
        for (ArchivoPartida formato : FORMATOS) {
            for (String perfil : CatalogoMaquinas.perfiles()) {
                POOBSoccer abierto = guardarYAbrir(juego("Goles", "Rusia", "Japón", perfil), formato);
                assertEquals(perfil, abierto.getPartidoActual().getJugador2().getPerfilMaquina());
            }
            POOBSoccer personas = guardarYAbrir(juego("Goles", "Rusia", "Japón", null), formato);
            assertInstanceOf(JugadorHumano.class, personas.getPartidoActual().getJugador2());
        }
    }

    // Partido

    @Test
    public void conservaElMarcador() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", null);
            juego.getPartidoActual().getMarcador()[0] = 2;
            juego.getPartidoActual().getMarcador()[1] = 1;
            juego.getPartidoActual().getJugador1().anotarGol();
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            assertArrayEquals(new int[]{2, 1}, abierto.getPartidoActual().getMarcador());
            assertEquals(1, abierto.getPartidoActual().getJugador1().getGolesAnotados());
        }
    }

    @Test
    public void conservaElTiempoQueQueda() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Tiempo", "Rusia", "Japón", null);
            avanzar(juego, 60 * 25 + 40);
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            PartidoPorTiempo partido = assertInstanceOf(PartidoPorTiempo.class, abierto.getPartidoActual());
            assertEquals(155, partido.getTiempoRestante());
            abierto.reanudarJuego();
            avanzar(abierto, 19);
            assertEquals(155, partido.getTiempoRestante(), "También conserva la parte del segundo que ya había pasado");
            avanzar(abierto, 1);
            assertEquals(154, partido.getTiempoRestante());
        }
    }

    @Test
    public void conservaElGolDeOro() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Tiempo", "Rusia", "Japón", null);
            avanzar(juego, 60 * 180);
            PartidoPorTiempo partido = (PartidoPorTiempo) guardarYAbrir(juego, formato).getPartidoActual();
            assertTrue(partido.isEsGolDeOro());
            assertFalse(partido.isTerminado());
        }
    }

    @Test
    public void conservaLaVidaYLaBarraDePoder() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", null);
            personaje(juego, 2).recibirDanio(35);
            juego.getPartidoActual().getJugador1().cargarPoder(63);
            juego.getPartidoActual().getJugador2().cargarPoder(100);
            juego.getPartidoActual().getJugador2().activarPoder();
            Partido abierto = guardarYAbrir(juego, formato).getPartidoActual();
            assertEquals(65, abierto.getJugador2().getPersonaje().getVidaActual());
            assertEquals(63, abierto.getJugador1().getBarraPoder());
            assertTrue(abierto.getJugador2().tienePoderPreparado(), "El aura sigue puesta");
        }
    }

    @Test
    public void conservaLosEfectosSobreElRival() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", null);
            personaje(juego, 1).aplicarElectrocucion(3);
            personaje(juego, 2).aplicarEmpuje(-1);
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            assertTrue(personaje(abierto, 1).isElectrificado());
            assertTrue(personaje(abierto, 2).isEmpujado());

            personaje(juego, 1).aplicarConfusion(3);
            personaje(juego, 2).aplicarQuemadura(3, 5);
            abierto = guardarYAbrir(juego, formato);
            assertTrue(personaje(abierto, 1).isConfundido());
            assertTrue(personaje(abierto, 2).isQuemado());

            personaje(juego, 1).congelar(2);
            personaje(juego, 2).aturdir(2);
            abierto = guardarYAbrir(juego, formato);
            assertTrue(personaje(abierto, 1).isCongelado());
            assertTrue(personaje(abierto, 2).isAturdido());

            personaje(juego, 1).aplicarInconsciencia(2);
            assertTrue(personaje(guardarYAbrir(juego, formato), 1).isInconsciente());
        }
    }

    @Test
    public void elPartidoSigueExactamenteIgual() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", "Experta");
            avanzar(juego, 200);
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            abierto.reanudarJuego();
            assertSiguenIguales(juego, abierto, 600);
        }
    }

    @Test
    public void unTiroDePoderSigueSuCamino() {
        for (ArchivoPartida formato : FORMATOS) {
            for (String pais : CatalogoPersonajes.paises()) {
                POOBSoccer juego = juego("Goles", pais, "Rusia", null);
                lanzarPoder(juego);
                avanzar(juego, 6);
                POOBSoccer abierto = guardarYAbrir(juego, formato);
                abierto.reanudarJuego();
                assertTrue(abierto.getPartidoActual().getBalon().isDisparoEspecial(), pais);
                assertSiguenIguales(juego, abierto, 240);
            }
        }
    }

    @Test
    public void conservaLosBalonesFalsos() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Estados Unidos", null);
            Partido p = juego.getPartidoActual();
            Jugador j = p.getJugador2();
            j.cargarPoder(100);
            j.realizarAccion("N", p);
            p.getBalon().resetearPosicion(j.getPersonaje().getPosX() + 20, j.getPersonaje().getPosY() + 20);
            j.realizarAccion("M", p);
            avanzar(juego, 5);
            Partido abierto = guardarYAbrir(juego, formato).getPartidoActual();
            assertEquals(7, abierto.getBalonesFalsos().size());
            assertInstanceOf(PoderIlusion.class, abierto.getPoderVisible(abierto.getBalon()));
        }
    }

    // Progreso

    @Test
    public void conservaLosPuntosYElNombre() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", null);
            juego.getPerfil(1).agregarPuntos(725);
            juego.getPerfil(1).setNombre("Ana");
            juego.getPerfil(2).agregarPuntos(50);
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            assertEquals(725, abierto.getPerfil(1).getPuntos());
            assertEquals("Ana", abierto.getPerfil(1).getNombre());
            assertEquals(50, abierto.getPerfil(2).getPuntos());
        }
    }

    @Test
    public void conservaLasMejorasDeCadaPersonaje() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = new POOBSoccer();
            juego.getPerfil(1).agregarPuntos(5000);
            juego.getPerfil(1).mejorar("Rusia", Estadistica.SPEED);
            juego.getPerfil(1).mejorar("Nigeria", Estadistica.VIDA);
            iniciar(juego, "Goles", "Rusia", "Rusia", null);
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            assertEquals(2, abierto.getPerfil(1).getNivel("Rusia", Estadistica.SPEED));
            assertEquals(2, abierto.getPerfil(1).getNivel("Nigeria", Estadistica.VIDA));
            assertEquals(1, abierto.getPerfil(1).getNivel("Japón", Estadistica.VIDA));
            assertEquals(1, abierto.getPerfil(2).getNivel("Rusia", Estadistica.SPEED));
            assertEquals(2, personaje(abierto, 1).getVelocidad(), "Sigue jugando con su mejora");
            assertEquals(1, personaje(abierto, 2).getVelocidad());
        }
    }

    @Test
    public void alTerminarElPartidoAbiertoSeGananLosPuntos() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", null);
            juego.getPartidoActual().getMarcador()[1] = 1;
            POOBSoccer abierto = guardarYAbrir(juego, formato);
            abierto.getPartidoActual().terminar();
            assertEquals(25, abierto.getPerfil(1).getPuntos());
            assertEquals(100, abierto.getPerfil(2).getPuntos());
        }
    }

    @Test
    public void abrirReemplazaElProgresoQueHabia() {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = new File(carpeta, "progreso." + formato.getExtension());
            POOBSoccer juego = juego("Goles", "Rusia", "Japón", null);
            juego.getPerfil(1).agregarPuntos(600);
            formato.guardar(juego, archivo);
            juego.getPerfil(1).mejorar("Rusia", Estadistica.KICK);
            formato.abrir(juego, archivo);
            assertEquals(600, juego.getPerfil(1).getPuntos());
            assertEquals(1, juego.getPerfil(1).getNivel("Rusia", Estadistica.KICK));
        }
    }

    // Después de abrir

    @Test
    public void laPartidaAbiertaQuedaEnPausa() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(juego("Goles", "Rusia", "Japón", null), formato);
            assertTrue(abierto.isPausa());
            assertTrue(abierto.hayPartidoEnCurso());
        }
    }

    @Test
    public void lasTeclasSiguenFuncionando() {
        for (ArchivoPartida formato : FORMATOS) {
            POOBSoccer abierto = guardarYAbrir(juego("Goles", "Rusia", "Japón", null), formato);
            Partido p = abierto.getPartidoActual();
            double x1 = personaje(abierto, 1).getPosX(), x2 = personaje(abierto, 2).getPosX();
            p.getJugador1().realizarAccion("D", p);
            p.getJugador2().realizarAccion("LEFT", p);
            assertTrue(personaje(abierto, 1).getPosX() > x1);
            assertTrue(personaje(abierto, 2).getPosX() < x2);
        }
    }
}
