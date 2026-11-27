package presentacion;

import Domain.*;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas de las animaciones de los poderes: auras, tiros y efectos sobre el rival. */
public class EfectosPoderTest {

    private static final int LADO = 300;

    /** Poder que nadie ha registrado en la parte visual. */
    private static final Poder PODER_NUEVO = objetivo -> { };

    private static Personaje personaje(Poder poder) {
        Personaje p = new Personaje("X", "X", 1, 1, 1, poder);
        p.configurarCancha(1000, 550);
        return p;
    }

    private static Balon balon() {
        Balon b = new Balon(150, 150, 16);
        b.aplicarDisparoEspecial(12, -3, 1);
        return b;
    }

    private static int puntosPintados(Consumer<java.awt.Graphics2D> dibujo) {
        BufferedImage vacia = dibujo(LADO, LADO, g -> { });
        return diferencias(vacia, dibujo(LADO, LADO, dibujo));
    }

    private static int efecto(Consumer<Personaje> aplicar) {
        Personaje p = personaje(new PoderRayo());
        aplicar.accept(p);
        return puntosPintados(g -> EfectosPoder.dibujarEfectoSobre(g, p, 120, 120));
    }

    // Colores

    @Test
    public void cadaPoderTieneSuPropioColor() {
        Set<Color> colores = new HashSet<>();
        for (String pais : CatalogoPersonajes.paises()) {
            colores.add(EfectosPoder.colorDe(CatalogoPersonajes.crear(pais).getPoderEspecial()));
        }
        assertEquals(CatalogoPersonajes.paises().size(), colores.size());
    }

    @Test
    public void unPoderNuevoUsaElColorPorDefecto() {
        assertEquals(Color.YELLOW, EfectosPoder.colorDe(PODER_NUEVO));
        assertEquals(Color.YELLOW, EfectosPoder.colorDe(null));
    }

    // Auras

    @Test
    public void todosLosPaisesTienenAuraVisible() {
        for (String pais : CatalogoPersonajes.paises()) {
            Personaje p = CatalogoPersonajes.crear(pais);
            assertTrue(puntosPintados(g -> EfectosPoder.dibujarAura(g, p, 120, 120)) > 1000, pais);
        }
    }

    @Test
    public void cadaPaisAdornaSuAura() {
        int simple = puntosPintados(g -> EfectosPoder.dibujarAura(g, personaje(PODER_NUEVO), 120, 120));
        for (String pais : CatalogoPersonajes.paises()) {
            Personaje p = CatalogoPersonajes.crear(pais);
            assertTrue(puntosPintados(g -> EfectosPoder.dibujarAura(g, p, 120, 120)) > simple, pais);
        }
    }

    @Test
    public void unPersonajeSinPoderNoDaniaElDibujo() {
        assertDoesNotThrow(() -> puntosPintados(g -> EfectosPoder.dibujarAura(g, personaje(null), 120, 120)));
    }

    // Tiros

    @Test
    public void losTirosSeAdornanAlrededorDelBalon() {
        Poder[] poderes = {new PoderAuraAzul(), new PoderRayo(), new PoderTornado(), new PoderHielo(), new PoderFuego()};
        for (Poder poder : poderes) {
            assertTrue(puntosPintados(g -> EfectosPoder.dibujarTiro(g, balon(), poder)) > 300, poder.getClass().getSimpleName());
        }
    }

    @Test
    public void losBalonesDeLaIlusionSeAdornanTodosIgual() {
        Balon real = balon();
        Balon otro = balon();
        // Aunque uno vaya hacia otro lado o ya no sea un tiro especial, el adorno es el mismo.
        otro.aplicarFuerza(-5, 4);
        BufferedImage uno = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, real, new PoderIlusion(), 1234));
        BufferedImage dos = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, otro, new PoderIlusion(), 1234));
        assertEquals(0, diferencias(uno, dos));
        assertTrue(puntosPintados(g -> EfectosPoder.dibujarTiro(g, real, new PoderIlusion())) > 300);
    }

    @Test
    public void losTirosSeAnimanConElTiempo() {
        for (String pais : CatalogoPersonajes.paises()) {
            Poder poder = CatalogoPersonajes.crear(pais).getPoderEspecial();
            BufferedImage antes = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, balon(), poder, 1000));
            BufferedImage despues = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, balon(), poder, 1150));
            assertTrue(diferencias(antes, despues) > 20, pais);
        }
    }

    @Test
    public void cadaTiroSeVeDistintoALosDemas() {
        Set<Integer> dibujos = new HashSet<>();
        for (String pais : CatalogoPersonajes.paises()) {
            Poder poder = CatalogoPersonajes.crear(pais).getPoderEspecial();
            BufferedImage imagen = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, balon(), poder, 1000));
            dibujos.add(java.util.Arrays.hashCode(imagen.getRGB(0, 0, LADO, LADO, null, 0, LADO)));
        }
        assertEquals(CatalogoPersonajes.paises().size(), dibujos.size());
    }

    @Test
    public void unPoderNuevoSeDibujaConUnBrillo() {
        BufferedImage imagen = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, balon(), PODER_NUEVO));
        assertEquals(Color.YELLOW.getRGB(), imagen.getRGB(150, 150));
    }

    @Test
    public void laEstelaQuedaDetrasDelBalon() {
        Balon haciaLaDerecha = balon();
        Balon haciaLaIzquierda = balon();
        haciaLaIzquierda.aplicarDisparoEspecial(-12, 0, 2);
        BufferedImage derecha = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, haciaLaDerecha, new PoderAuraAzul()));
        BufferedImage izquierda = dibujo(LADO, LADO, g -> EfectosPoder.dibujarTiro(g, haciaLaIzquierda, new PoderAuraAzul()));
        int negro = Color.BLACK.getRGB();
        assertNotEquals(negro, derecha.getRGB(105, 150));
        assertEquals(negro, derecha.getRGB(195, 150));
        assertNotEquals(negro, izquierda.getRGB(195, 150));
        assertEquals(negro, izquierda.getRGB(105, 150));
    }

    // Efectos sobre el rival

    @Test
    public void sinEfectosNoDibujaNada() {
        assertEquals(0, efecto(p -> { }));
    }

    @Test
    public void todosLosEfectosSeVen() {
        assertTrue(efecto(p -> p.aplicarEmpuje(1)) > 200, "Empuje");
        assertTrue(efecto(p -> p.aplicarElectrocucion(1)) > 200, "Electrocución");
        assertTrue(efecto(p -> p.aplicarInconsciencia(1)) > 200, "Inconsciencia");
        assertTrue(efecto(p -> p.aplicarConfusion(1)) > 200, "Confusión");
        assertTrue(efecto(p -> p.congelar(1)) > 200, "Congelamiento");
        assertTrue(efecto(p -> p.aplicarQuemadura(1, 1)) > 200, "Quemadura");
    }

    @Test
    public void elHieloCubreTodoElCuerpo() {
        Personaje p = personaje(new PoderRayo());
        p.congelar(1);
        BufferedImage imagen = dibujo(LADO, LADO, g -> EfectosPoder.dibujarEfectoSobre(g, p, 120, 120));
        int negro = Color.BLACK.getRGB();
        assertNotEquals(negro, imagen.getRGB(122, 130), "Arriba a la izquierda");
        assertNotEquals(negro, imagen.getRGB(170, 195), "Abajo a la derecha");
    }

    @Test
    public void laEtiquetaConservaElTipoDeLetra() {
        Personaje p = personaje(new PoderRayo());
        p.aplicarConfusion(1);
        dibujo(LADO, LADO, g -> {
            java.awt.Font antes = g.getFont();
            EfectosPoder.dibujarEfectoSobre(g, p, 120, 120);
            assertEquals(antes, g.getFont());
        });
    }
}
