package presentacion;

import Domain.CatalogoMaquinas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas de las pantallas del menú: cada botón debe avisar lo que corresponde. */
public class PantallasMenuTest {

    private List<String> avisos;

    @BeforeEach
    public void preparar() {
        avisos = new ArrayList<>();
    }

    private Runnable avisa(String texto) {
        return () -> avisos.add(texto);
    }

    // Inicio

    @Test
    public void inicioAvisaJugarYSalir() {
        PantallaInicio p = new PantallaInicio(avisa("jugar"), avisa("salir"));
        boton(p, "PLAY").doClick(0);
        boton(p, "SALIR").doClick(0);
        assertEquals(Arrays.asList("jugar", "salir"), avisos);
    }

    @Test
    public void inicioSeDibujaEnVariosTamanos() {
        PantallaInicio p = new PantallaInicio(avisa("jugar"), avisa("salir"));
        assertDoesNotThrow(() -> foto(p, 1000, 650));
        assertDoesNotThrow(() -> foto(p, 1600, 500));
        assertDoesNotThrow(() -> foto(p, 500, 900));
    }

    // Modo de juego

    @Test
    public void modalidadOfreceLasDosOpciones() {
        PantallaModalidad p = new PantallaModalidad(avisa("jugador"), avisa("máquina"), avisa("volver"));
        boton(p, "JUGADOR VS JUGADOR").doClick(0);
        boton(p, "JUGADOR VS MÁQUINA").doClick(0);
        boton(p, "VOLVER").doClick(0);
        assertEquals(Arrays.asList("jugador", "máquina", "volver"), avisos);
    }

    @Test
    public void modalidadDiceQueHayQueElegir() {
        assertTrue(hayTexto(new PantallaModalidad(avisa("a"), avisa("b"), avisa("c")), "SELECCIONA EL MODO DE JUEGO"));
    }

    // Máquina

    @Test
    public void maquinaTieneUnBotonPorCadaPerfilDelCatalogo() {
        PantallaMaquina p = new PantallaMaquina(avisos::add, avisa("volver"));
        for (String perfil : CatalogoMaquinas.perfiles()) boton(p, perfil.toUpperCase()).doClick(0);
        assertEquals(CatalogoMaquinas.perfiles(), avisos, "Cada botón entrega su propio perfil");
        assertEquals(CatalogoMaquinas.perfiles().size() + 1, buscar(p, JButton.class).size(), "Los perfiles y VOLVER");
    }

    @Test
    public void maquinaExplicaComoJuegaCadaPerfil() {
        PantallaMaquina p = new PantallaMaquina(avisos::add, avisa("volver"));
        for (String perfil : CatalogoMaquinas.perfiles()) {
            assertTrue(hayTexto(p, CatalogoMaquinas.crear(perfil).descripcion()), perfil);
        }
    }

    @Test
    public void maquinaPermiteVolver() {
        PantallaMaquina p = new PantallaMaquina(avisos::add, avisa("volver"));
        boton(p, "VOLVER").doClick(0);
        assertEquals(Collections.singletonList("volver"), avisos);
    }

    // Tipo de partido

    @Test
    public void tiposAvisaElTipoElegido() {
        PantallaTipos p = new PantallaTipos(avisos::add, avisa("volver"));
        boton(p, "PARTIDO POR TIEMPO").doClick(0);
        boton(p, "PARTIDO POR GOLES").doClick(0);
        boton(p, "VOLVER").doClick(0);
        assertEquals(Arrays.asList("Tiempo", "Goles", "volver"), avisos);
    }

    @Test
    public void lasPantallasDelMenuSeDibujanSinErrores() {
        assertDoesNotThrow(() -> foto(new PantallaModalidad(avisa("a"), avisa("b"), avisa("c")), 1000, 650));
        assertDoesNotThrow(() -> foto(new PantallaMaquina(avisos::add, avisa("b")), 1000, 650));
        assertDoesNotThrow(() -> foto(new PantallaTipos(avisos::add, avisa("b")), 1000, 650));
    }
}
