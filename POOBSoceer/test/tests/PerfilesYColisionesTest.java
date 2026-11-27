package tests;

import Domain.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica la conservación del progreso, la compra de niveles y la separación física entre jugadores. */
public class PerfilesYColisionesTest {

    @Test
    public void lasEstadisticasComienzanEnUnoYSeConstruyenDesdeElPerfil() {
        PerfilJugador perfil = new PerfilJugador("jugador1", "Santy");
        Personaje personaje = CatalogoPersonajes.crear("Corea del Sur", perfil);

        assertEquals(1, personaje.getSurvival());
        assertEquals(1, personaje.getVelocidad());
        assertEquals(1, personaje.getSalto());
        assertEquals(1, personaje.getKick());
        assertEquals(100, personaje.getVidaMaxima());

        perfil.agregarPuntos(1500);
        assertTrue(perfil.mejorar("Corea del Sur", PerfilJugador.Estadistica.VIDA));
        assertEquals(2, perfil.getNivel("Corea del Sur", PerfilJugador.Estadistica.VIDA));
        assertEquals(1000, perfil.getPuntos());
        assertEquals(1000, perfil.getCostoSiguienteMejora("Corea del Sur"));

        Personaje mejorado = CatalogoPersonajes.crear("Corea del Sur", perfil);
        assertEquals(110, mejorado.getVidaMaxima(), "Un nivel de Survival aumenta diez puntos la vida máxima");

        Personaje velocidadBase = new Personaje("Base", "Base", 1, 1, 1, new PoderAuraAzul());
        Personaje velocidadMejorada = new Personaje("Mejorado", "Mejorado", 2, 2, 1, new PoderAuraAzul());
        velocidadBase.configurarCancha(1000, 550);
        velocidadMejorada.configurarCancha(1000, 550);
        velocidadBase.resetearPosicion(100, 470);
        velocidadMejorada.resetearPosicion(100, 470);
        velocidadBase.mover(1, null);
        velocidadMejorada.mover(1, null);
        assertEquals(Personaje.PASO_BASE * 1.1, velocidadMejorada.getPosX() - 100, 0.0001,
                "Speed 2 debe desplazarse un diez por ciento más que Speed 1");

        velocidadBase.saltar();
        velocidadMejorada.saltar();
        velocidadBase.actualizarEstado();
        velocidadMejorada.actualizarEstado();
        assertEquals(0.77, velocidadBase.getPosY() - velocidadMejorada.getPosY(), 0.0001,
                "Jump 2 debe aumentar un diez por ciento el impulso base");
    }

    @Test
    public void losNombresPuntosYMejorasSeConservanEnElArchivoLocal() throws Exception {
        Path carpetaTemporal = Files.createTempDirectory("poobsoccer-perfiles-");
        Path archivo = carpetaTemporal.resolve("perfiles.properties");
        RepositorioPerfiles repositorio = new RepositorioPerfiles(archivo);
        PerfilJugador[] perfiles = repositorio.cargarPerfiles();

        perfiles[0].setNombre("Jugador Azul");
        perfiles[0].agregarPuntos(500);
        assertTrue(perfiles[0].mejorar("Japón", PerfilJugador.Estadistica.SPEED));
        perfiles[1].agregarPuntos(25);
        repositorio.guardarPerfiles(perfiles[0], perfiles[1]);

        PerfilJugador[] recuperados = repositorio.cargarPerfiles();
        assertEquals("Jugador Azul", recuperados[0].getNombre());
        assertEquals(2, recuperados[0].getNivel("Japón", PerfilJugador.Estadistica.SPEED), "La mejora queda en nivel 2");
        assertEquals(1, recuperados[0].getNivel("Rusia", PerfilJugador.Estadistica.SPEED), "Los demás personajes siguen igual");
        assertEquals(0, recuperados[0].getPuntos());
        assertEquals(25, recuperados[1].getPuntos());
    }

    @Test
    public void losPuntosDelResultadoSeAgreganAlPerfilPermanente() {
        PerfilJugador perfil1 = new PerfilJugador("jugador1", "Uno");
        PerfilJugador perfil2 = new PerfilJugador("jugador2", "Dos");
        Personaje p1 = CatalogoPersonajes.crear("Corea del Sur", perfil1);
        Personaje p2 = CatalogoPersonajes.crear("Camerún", perfil2);
        Jugador j1 = new JugadorHumano("Uno", p1, new String[]{"A", "D", "W", "S", "Q"}, perfil1);
        Jugador j2 = new JugadorHumano("Dos", p2, new String[]{"LEFT", "RIGHT", "UP", "M", "N"}, perfil2);
        Partido partido = new PartidoPorGoles(j1, j2, 5);

        // Un empate entrega 25 puntos a cada participante, según las reglas del proyecto.
        partido.terminar();
        assertEquals(25, perfil1.getPuntos());
        assertEquals(25, perfil2.getPuntos());
    }

    @Test
    public void unNuevoEfectoEspecialReemplazaAlAnterior() {
        Personaje rival = new Personaje("Rival", "Rival", 1, 1, 1, 1, new PoderRayo());
        rival.aplicarElectrocucion(3);
        rival.congelar(3);
        assertFalse(rival.isElectrificado());
        assertTrue(rival.congelado());

        rival.aplicarInconsciencia(3);
        assertFalse(rival.congelado());
        assertTrue(rival.isInconsciente());

        rival.aplicarConfusion(2);
        assertFalse(rival.isInconsciente());
        assertTrue(rival.isConfundido());

        rival.aplicarEmpuje(1);
        assertFalse(rival.isConfundido());
    }

    @Test
    public void losPersonajesNoPuedenAtravesarseAunqueUnoEsteSaltando() {
        Personaje izquierda = new Personaje("Izquierda", "A", 3, 3, 3, new PoderAuraAzul());
        Personaje derecha = new Personaje("Derecha", "B", 3, 3, 3, new PoderAuraAzul());
        izquierda.configurarCancha(1000, 550);
        derecha.configurarCancha(1000, 550);
        izquierda.resetearPosicion(200, 300);
        derecha.resetearPosicion(200 + Personaje.ANCHO, 200);

        // La barrera horizontal se mantiene incluso cuando los personajes están a distintas alturas.
        izquierda.mover(1, derecha);
        assertEquals(200, izquierda.getPosX(), 0.0001);
        derecha.mover(-1, izquierda);
        assertEquals(200 + Personaje.ANCHO, derecha.getPosX(), 0.0001);

        // Un empuje tampoco puede saltarse la barrera aunque venga de un poder especial.
        izquierda.aplicarEmpuje(1);
        izquierda.actualizarEstado(derecha);
        assertTrue(izquierda.getPosX() + Personaje.ANCHO <= derecha.getPosX() + 0.0001);

        // Si por una carga anterior llegaran exactamente a la misma coordenada, la barrera también los separa.
        izquierda.resetearPosicion(300, 250);
        derecha.resetearPosicion(300, 150);
        izquierda.mover(1, derecha);
        derecha.mover(-1, izquierda);
        assertTrue(izquierda.getPosX() + Personaje.ANCHO <= derecha.getPosX() + 0.0001);
    }

    @Test
    public void laPatadaSoloHaceDanioCuandoElPieAlcanzaAlRival() {
        Personaje atacante = new Personaje("Atacante", "A", 1, 1, 3, 1, new PoderAuraAzul());
        Personaje rival = new Personaje("Rival", "B", 1, 1, 1, 1, new PoderAuraAzul());
        Jugador j1 = new JugadorHumano("Uno", atacante, new String[]{"A", "D", "W", "S", "Q"});
        Jugador j2 = new JugadorHumano("Dos", rival, new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
        Partido partido = new PartidoPorGoles(j1, j2, 5);
        atacante.resetearPosicion(100, 370);
        rival.resetearPosicion(100 + Personaje.ANCHO + 24, 370);

        atacante.patear();
        partido.ejecutarPatada(j1);
        assertEquals(88, rival.getVidaActual(), "Kick 3 produce 12 de daño; vida inicial 100");

        // Un personaje inmovilizado no puede hacer daño solo porque se pulse la tecla de patada.
        rival.resetearPosicion(100 + Personaje.ANCHO, 370);
        atacante.aturdir(3);
        atacante.consumirPatada();
        atacante.patear(); // El método debe rechazar la patada mientras dura el aturdimiento.
        partido.ejecutarPatada(j1);
        assertEquals(100, rival.getVidaActual(), "Un jugador aturdido no puede golpear");

        atacante.resetearPosicion(100, 370);
        rival.resetearPosicion(100 + Personaje.ANCHO + 25, 370);
        atacante.patear();
        partido.ejecutarPatada(j1);
        assertEquals(100, rival.getVidaActual(), "A un píxel fuera del alcance no debe haber daño");
    }

    @Test
    public void elPieAlcanzaElBalonDelanteYNoElDeAtras() {
        Personaje p = new Personaje("Uno", "A", 1, 1, 1, new PoderAuraAzul());
        p.resetearPosicion(300, 370);
        Balon delante = new Balon(300 + Personaje.ANCHO + 30, 370 + 65, 16);
        Balon atras = new Balon(300 - 10, 370 + 65, 16);
        Balon cabeza = new Balon(300 + Personaje.ANCHO / 2.0, 370 - 10, 16);
        assertFalse(p.alcanzaConElPie(delante, 1), "Sin patear el pie no golpea");

        p.patear();
        assertTrue(p.alcanzaConElPie(delante, 1), "El pie sale por delante del cuerpo");
        assertFalse(p.alcanzaConElPie(atras, 1), "Lo de atrás no lo toca el pie");
        assertTrue(p.alcanzaConElPie(atras, -1), "Mirando al otro lado sí");
        assertFalse(p.alcanzaConElPie(cabeza, 1), "La cabeza no es el pie");
        assertFalse(p.alcanzaConElPie(new Balon(300 + Personaje.ANCHO + 60, 370 + 65, 16), 1), "Muy lejos");
    }

    @Test
    public void conUnBalonDetrasLaPatadaNoLoMandaHaciaAdelante() {
        Personaje uno = new Personaje("Uno", "A", 1, 1, 1, new PoderAuraAzul());
        Personaje dos = new Personaje("Dos", "B", 1, 1, 1, new PoderAuraAzul());
        Jugador j1 = new JugadorHumano("Uno", uno, new String[]{"A", "D", "W", "S", "Q"});
        Jugador j2 = new JugadorHumano("Dos", dos, new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
        Partido partido = new PartidoPorGoles(j1, j2, 5);
        partido.getBalon().resetearPosicion(uno.getPosX() + 5, uno.getPosY() + 60);
        uno.patear();
        partido.actualizarEstado();
        assertTrue(partido.getBalon().getVelX() < 0, "Rebota hacia atrás");
        assertTrue(uno.isPateando(), "La patada no se gastó en un balón que no tocó");
    }

    @Test
    public void alPegarleAlBalonElPieTerminaSuMovimiento() {
        Personaje uno = new Personaje("Uno", "A", 1, 1, 1, new PoderAuraAzul());
        Personaje dos = new Personaje("Dos", "B", 1, 1, 1, new PoderAuraAzul());
        Jugador j1 = new JugadorHumano("Uno", uno, new String[]{"A", "D", "W", "S", "Q"});
        Jugador j2 = new JugadorHumano("Dos", dos, new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
        Partido partido = new PartidoPorGoles(j1, j2, 5);
        Balon balon = partido.getBalon();
        balon.resetearPosicion(uno.getPosX() + Personaje.ANCHO + 10, uno.getPosY() + 60);

        uno.patear();
        partido.actualizarEstado();
        assertEquals(12, balon.getVelX(), 1e-6, "Precondición: el balón salió pateado");
        assertTrue(uno.isPateando(), "El pie sigue moviéndose después del golpe");
        assertTrue(uno.getFramesPateando() > 0 && uno.getFramesPateando() < Personaje.DURACION_PATADA);

        balon.resetearPosicion(uno.getPosX() + Personaje.ANCHO + 10, uno.getPosY() + 60);
        assertFalse(uno.alcanzaConElPie(balon, 1), "En la misma patada no vuelve a golpear");
        uno.patear();
        assertTrue(uno.alcanzaConElPie(balon, 1), "Con una patada nueva sí");
    }
}
