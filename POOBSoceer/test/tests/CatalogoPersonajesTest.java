package tests;

import Domain.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del catálogo: qué países existen y qué personaje y poder recibe cada uno. */
public class CatalogoPersonajesTest {

    private static final double DELTA = 1e-6;

    // Países registrados

    @Test
    public void catalogoListaLosPaisesEnElOrdenDeRegistro() {
        List<String> esperados = Arrays.asList(
                "Corea del Sur", "Camerún", "Nigeria", "Estados Unidos", "Rusia", "Japón");
        assertEquals(esperados, CatalogoPersonajes.paises(), "Lista de países");
    }

    @Test
    public void paisesDevuelveUnaCopiaQueNoAlteraElCatalogo() {
        List<String> copia = CatalogoPersonajes.paises();
        copia.clear();
        copia.add("Inventado");
        assertEquals(6, CatalogoPersonajes.paises().size(), "El catálogo conserva sus 6 países");
        assertFalse(CatalogoPersonajes.existe("Inventado"), "El país agregado a la copia no existe");
    }

    @Test
    public void existeSoloParaPaisesRegistrados() {
        for (String pais : CatalogoPersonajes.paises()) {
            assertTrue(CatalogoPersonajes.existe(pais), "Debe existir " + pais);
        }
        assertFalse(CatalogoPersonajes.existe("Brasil"), "País no registrado");
        assertFalse(CatalogoPersonajes.existe(""), "Cadena vacía");
        assertFalse(CatalogoPersonajes.existe("corea del sur"), "La búsqueda distingue mayúsculas");
        assertFalse(CatalogoPersonajes.existe(null), "Nulo");
    }

    // Personajes creados

    @Test
    public void crearConstruyeCadaPersonajeConLosAtributosEstandar() {
        for (String pais : CatalogoPersonajes.paises()) {
            Personaje p = CatalogoPersonajes.crear(pais);
            assertEquals(pais, p.getNombre(), pais + ": nombre");
            assertEquals(pais, p.getPais(), pais + ": país");
            assertEquals(1, p.getVelocidad(), pais + ": velocidad inicial");
            assertEquals(1, p.getSalto(), pais + ": salto inicial");
            assertEquals(1, p.getKick(), pais + ": kick inicial");
            assertEquals(1, p.getSurvival(), pais + ": survival inicial");
            assertEquals(100, p.getVidaMaxima(), pais + ": vida máxima inicial");
            assertEquals(100, p.getVidaActual(), pais + ": inicia con la vida llena");
            assertEquals(1.0, p.getMultiplicadorKick(), DELTA, pais + ": multiplicador de kick inicial");
            assertNotNull(p.getPoderEspecial(), pais + ": tiene poder");
        }
    }

    @Test
    public void cadaPaisRecibeSuPropioPoder() {
        String[] paises = {"Corea del Sur", "Camerún", "Nigeria", "Estados Unidos", "Rusia", "Japón"};
        Class<?>[] poderes = {PoderAuraAzul.class, PoderRayo.class, PoderTornado.class, PoderIlusion.class,
                PoderHielo.class, PoderFuego.class};
        Set<Class<?>> distintos = new HashSet<>();
        for (int i = 0; i < paises.length; i++) {
            Class<?> real = CatalogoPersonajes.crear(paises[i]).getPoderEspecial().getClass();
            assertEquals(poderes[i], real, paises[i] + ": poder asignado");
            distintos.add(real);
        }
        assertEquals(6, distintos.size(), "Cada país tiene un poder diferente");
    }

    @Test
    public void cadaCreacionDevuelveInstanciasIndependientes() {
        for (String pais : CatalogoPersonajes.paises()) {
            Personaje a = CatalogoPersonajes.crear(pais);
            Personaje b = CatalogoPersonajes.crear(pais);
            assertNotSame(a, b, pais + ": personajes distintos");
            assertNotSame(a.getPoderEspecial(), b.getPoderEspecial(), pais + ": poderes distintos");
            a.recibirDanio(50);
            assertEquals(100, b.getVidaActual(), pais + ": el daño de uno no afecta al otro");
        }
    }

    // Países inválidos

    @Test
    public void crearConPaisInexistenteLanzaExcepcionIndicandoElPais() {
        for (String pais : new String[]{"Brasil", "", "corea del sur"}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> CatalogoPersonajes.crear(pais), "País inválido: '" + pais + "'");
            assertTrue(ex.getMessage().contains(pais), "El mensaje menciona el país: " + ex.getMessage());
        }
        assertThrows(IllegalArgumentException.class, () -> CatalogoPersonajes.crear(null), "País nulo");
    }

    // Integración

    @Test
    public void todasLasCombinacionesDePaisesPuedenJugarUnPartido() {
        List<String> paises = new ArrayList<>(CatalogoPersonajes.paises());
        for (String a : paises) {
            for (String b : paises) {
                assertDoesNotThrow(() -> {
                    Jugador j1 = new JugadorHumano(a, CatalogoPersonajes.crear(a), new String[]{"A", "D", "W", "S", "Q"});
                    Jugador j2 = new JugadorHumano(b, CatalogoPersonajes.crear(b), new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
                    Partido partido = new PartidoPorGoles(j1, j2, 5);
                    for (int i = 0; i < 10; i++) partido.actualizarEstado();
                    assertFalse(partido.isTerminado(), a + " vs " + b + ": sigue en juego");
                }, a + " vs " + b);
            }
        }
    }
}