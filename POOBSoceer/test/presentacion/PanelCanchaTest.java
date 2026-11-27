package presentacion;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas del dibujo del partido: lo que cambia en el juego debe notarse en la pantalla. */
public class PanelCanchaTest {

    // Las dos cajas de goles del tablero, el letrero de abajo y el panel del jugador 1 con su nombre.
    private static final Rectangle MARCADOR = new Rectangle(390, 20, 220, 62);
    private static final Rectangle LETRERO = new Rectangle(385, 86, 230, 28);
    private static final Rectangle DATOS_J1 = new Rectangle(10, 5, 300, 110);

    private POOBSoccer juego;
    private Partido partido;

    @BeforeEach
    public void preparar() {
        juego = ApoyoVisual.partido("Rusia", "Japón");
        partido = juego.getPartidoActual();
    }

    private BufferedImage foto() {
        return ApoyoVisual.foto(new PanelCancha(juego), 1000, 650);
    }

    /** Zona de la pantalla que ocupa un personaje con un margen a los lados. */
    private Rectangle zonaDe(Personaje p) {
        return new Rectangle((int) p.getPosX() - 40, (int) p.getPosY() - 60, Personaje.ANCHO + 80, Personaje.ALTO + 60);
    }

    @Test
    public void sinPartidoNoDibujaLaCancha() {
        BufferedImage vacia = ApoyoVisual.foto(new PanelCancha(new POOBSoccer()), 1000, 650);
        assertEquals(new Color(115, 190, 235).getRGB(), vacia.getRGB(500, 640), "Solo se ve el fondo");
    }

    /** El pasto de la imagen es verde: el verde domina sobre el rojo y el azul. */
    private static boolean esPasto(int rgb) {
        Color c = new Color(rgb);
        return c.getGreen() > c.getRed() + 40 && c.getGreen() > c.getBlue() + 40;
    }

    @Test
    public void dibujaElCespedDebajoDelSuelo() {
        BufferedImage imagen = foto();
        assertTrue(esPasto(imagen.getRGB(500, 600)));
        assertFalse(esPasto(imagen.getRGB(500, 300)), "Arriba del suelo sigue el cielo");
    }

    @Test
    public void seAdaptaAlTamanoDeLaVentana() {
        BufferedImage chica = ApoyoVisual.foto(new PanelCancha(juego), 500, 325);
        assertTrue(esPasto(chica.getRGB(250, 310)), "El césped sigue abajo");
    }

    @Test
    public void elFondoEsSoloElCieloSinLineas() {
        // Donde antes iban la línea del centro y el círculo ahora se ve el cielo.
        BufferedImage imagen = foto();
        for (int[] punto : new int[][]{{500, 300}, {500, 450}, {435, 485}}) {
            Color c = new Color(imagen.getRGB(punto[0], punto[1]));
            assertTrue(c.getBlue() > c.getRed() + 20, "Cielo en " + punto[0] + "," + punto[1]);
        }
    }

    @Test
    public void elTravesanoDibujadoCoincideConElDelJuego() {
        BufferedImage imagen = foto();
        Cancha cancha = partido.getCancha();
        for (Arco arco : new Arco[]{cancha.getArcoIzquierdo(), cancha.getArcoDerecho()}) {
            Color palo = new Color(imagen.getRGB(arco.getPosX() + arco.getAncho() / 2, arco.getPosY()));
            assertTrue(palo.getRed() > 170 && palo.getGreen() > 170 && palo.getBlue() > 170,
                    "El palo blanco pasa justo por donde rebota el balón");
        }
    }

    @Test
    public void elBalonSeVeDondeEsta() {
        partido.getBalon().resetearPosicion(600, 300);
        assertEquals(Color.WHITE.getRGB(), foto().getRGB(600, 300));
    }

    @Test
    public void elMarcadorCambiaConLosGoles() {
        BufferedImage antes = foto();
        partido.getMarcador()[0] = 3;
        assertTrue(diferencias(antes, foto(), MARCADOR) > 0);
    }

    @Test
    public void laVidaSeVeEnSuBarra() {
        BufferedImage antes = foto();
        partido.getJugador1().getPersonaje().recibirDanio(50);
        assertTrue(diferencias(antes, foto(), DATOS_J1) > 500, "La barra se acorta");
    }

    @Test
    public void laCargaDelPoderSeVeEnSuBarra() {
        BufferedImage antes = foto();
        partido.getJugador1().cargarPoder(60);
        assertTrue(diferencias(antes, foto(), DATOS_J1) > 500);
    }

    @Test
    public void elPoderListoSeAvisaYMuestraElAura() {
        BufferedImage antes = foto();
        partido.getJugador1().cargarPoder(100);
        partido.getJugador1().activarPoder();
        BufferedImage despues = foto();
        assertTrue(diferencias(antes, despues, DATOS_J1) > 0, "Aviso de poder listo");
        assertTrue(diferencias(antes, despues, zonaDe(partido.getJugador1().getPersonaje())) > 500, "Aura");
        assertEquals(0, diferencias(antes, despues, zonaDe(partido.getJugador2().getPersonaje())), "El rival no tiene aura");
    }

    @Test
    public void laPausaOscureceLaCancha() {
        Color antes = new Color(foto().getRGB(300, 600));
        juego.pausarJuego();
        assertTrue(new Color(foto().getRGB(300, 600)).getGreen() < antes.getGreen());
    }

    @Test
    public void alTerminarMuestraElResultado() {
        Rectangle cartel = new Rectangle(270, 260, 460, 130);
        BufferedImage enJuego = foto();
        partido.terminar();
        BufferedImage empate = foto();
        assertTrue(diferencias(enJuego, empate, cartel) > 10000, "Aparece el cartel");
        partido.getMarcador()[0] = 1;
        assertTrue(diferencias(empate, foto(), cartel) > 0, "Un ganador se anuncia distinto al empate");
    }

    @Test
    public void elPartidoPorTiempoMuestraElReloj() {
        Rectangle reloj = LETRERO;
        BufferedImage sinReloj = foto();
        juego.iniciarPartido("Tiempo", POOBSoccer.JUGADOR_VS_JUGADOR, CatalogoPersonajes.crear("Rusia"),
                CatalogoPersonajes.crear("Japón"), 1, 1000, 650);
        BufferedImage conReloj = foto();
        assertTrue(diferencias(sinReloj, conReloj, reloj) > 0);
        for (int i = 0; i < 60; i++) juego.actualizar();
        assertTrue(diferencias(conReloj, foto(), reloj) > 0, "El tiempo avanza");
    }

    @Test
    public void elGolDeOroSeAnuncia() {
        juego.iniciarPartido("Tiempo", POOBSoccer.JUGADOR_VS_JUGADOR, CatalogoPersonajes.crear("Rusia"),
                CatalogoPersonajes.crear("Japón"), 1, 1000, 650);
        // El gol de oro ocupa el letrero donde antes estaba el reloj.
        Rectangle aviso = LETRERO;
        BufferedImage antes = foto();
        for (int i = 0; i < 3600; i++) juego.actualizar();
        assertTrue(((PartidoPorTiempo) juego.getPartidoActual()).isEsGolDeOro(), "Precondición");
        assertTrue(diferencias(antes, foto(), aviso) > 0);
    }

    @Test
    public void cadaEfectoSobreElRivalSeNota() {
        Personaje rival = partido.getJugador2().getPersonaje();
        Rectangle zona = zonaDe(rival);
        BufferedImage normal = foto();
        rival.congelar(3);
        assertTrue(diferencias(normal, foto(), zona) > 500, "Congelado");
        rival.aplicarQuemadura(3, 1);
        assertTrue(diferencias(normal, foto(), zona) > 500, "Quemado");
        rival.aplicarConfusion(3);
        assertTrue(diferencias(normal, foto(), zona) > 100, "Confundido");
        rival.aplicarElectrocucion(3);
        assertTrue(diferencias(normal, foto(), zona) > 100, "Electrocutado");
        rival.aplicarEmpuje(1);
        assertTrue(diferencias(normal, foto(), zona) > 100, "Empujado");
        rival.resetearPosicion(rival.getPosX(), rival.getPosY());
        rival.aturdir(3);
        assertTrue(diferencias(normal, foto(), zona) > 100, "Aturdido");
    }

    @Test
    public void laPatadaMueveElZapato() {
        Personaje p = partido.getJugador1().getPersonaje();
        BufferedImage quieto = foto();
        p.patear();
        for (int i = 0; i < 6; i++) p.actualizarEstado();
        assertTrue(diferencias(quieto, foto(), zonaDe(p)) > 0);
    }

    @Test
    public void mientrasHayFalsosElBalonRealNoPierdeSuAdorno() {
        juego = ApoyoVisual.partido("Estados Unidos", "Japón");
        partido = juego.getPartidoActual();
        Jugador j = partido.getJugador1();
        Balon real = partido.getBalon();
        j.cargarPoder(100);
        j.realizarAccion("Q", partido);
        real.resetearPosicion(j.getPersonaje().getPosX() + 20, j.getPersonaje().getPosY() + 20);
        j.realizarAccion("S", partido);
        juego.actualizar();
        assertEquals(7, partido.getBalonesFalsos().size(), "Precondición");

        real.consumirDisparoEspecial();
        Balon falso = partido.getBalonesFalsos().get(0);
        assertNull(partido.getPoderDelDisparo(real), "El real ya no lleva el poder");
        assertSame(partido.getPoderVisible(falso), partido.getPoderVisible(real), "Pero se ven iguales");
        assertNotNull(partido.getPoderVisible(real));
    }

    @Test
    public void losBalonesFalsosTambienSeDibujan() {
        juego = ApoyoVisual.partido("Estados Unidos", "Japón");
        partido = juego.getPartidoActual();
        Jugador j = partido.getJugador1();
        j.cargarPoder(100);
        j.realizarAccion("Q", partido);
        partido.getBalon().resetearPosicion(j.getPersonaje().getPosX() + 20, j.getPersonaje().getPosY() + 20);
        j.realizarAccion("S", partido);
        for (int i = 0; i < 12; i++) juego.actualizar();
        assertEquals(7, partido.getBalonesFalsos().size(), "Precondición");

        BufferedImage imagen = foto();
        for (Balon falso : partido.getBalonesFalsos()) {
            // Puede verse el borde negro de otro balón si los dos se cruzan justo ahí.
            int color = imagen.getRGB((int) falso.getPosX(), (int) falso.getPosY());
            assertTrue(color == Color.WHITE.getRGB() || color == Color.BLACK.getRGB());
        }
    }

    @Test
    public void unPaisSinImagenesUsaLaFiguraBasica() {
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_JUGADOR,
                new Personaje("X", "Sin imagen", 1, 1, 1, new PoderRayo()), CatalogoPersonajes.crear("Japón"), 5, 1000, 650);
        Personaje p = juego.getPartidoActual().getJugador1().getPersonaje();
        int x = (int) p.getPosX() + Personaje.ANCHO / 2, y = (int) p.getPosY() + Personaje.ALTO - 15;
        assertEquals(new Color(35, 105, 225).getRGB(), foto().getRGB(x, y), "Azul para el jugador 1");
        p.aturdir(3);
        assertEquals(Color.GRAY.getRGB(), foto().getRGB(x, y), "Gris si está aturdido");
    }

    @Test
    public void laMaquinaMuestraSuPerfilEnLosDatos() {
        Rectangle nombreJ2 = new Rectangle(690, 86, 300, 26);
        BufferedImage persona = foto();
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, CatalogoPersonajes.crear("Rusia"),
                CatalogoPersonajes.crear("Japón"), 5, 1000, 650, "Experta");
        assertTrue(diferencias(persona, foto(), nombreJ2) > 0);
    }
}
