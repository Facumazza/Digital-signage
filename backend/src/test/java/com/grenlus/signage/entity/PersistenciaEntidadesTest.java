package com.grenlus.signage.entity;

import com.grenlus.signage.enums.TipoContenido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las entidades persistan y que los @PrePersist completen los
 * valores por defecto. Corre contra la base real (replace = NONE): el objetivo
 * es justamente comprobar el mapeo contra PostgreSQL, no contra una base en
 * memoria que se comporta distinto.
 *
 * Cada test corre en una transaccion que se revierte al terminar, asi que no
 * deja datos.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenciaEntidadesTest {

    @Autowired
    private TestEntityManager em;

    private Cliente clientePersistido() {
        return em.persistFlushFind(Cliente.builder()
                .nombre("Grenlus SA")
                .email("contacto@grenlus.com")
                .build());
    }

    /** El codigo es unique en la base, y la base de desarrollo puede tener
     *  datos de pruebas manuales. Uno nuevo por test lo hace independiente. */
    private String codigoUnico() {
        return "GRN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** Sucursal no usa Lombok, asi que se arma con setters. */
    private Sucursal sucursalPersistida() {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre("Lomas");
        sucursal.setCiudad("Lomas de Zamora");
        sucursal.setCliente(clientePersistido());
        return em.persistFlushFind(sucursal);
    }

    @Test
    @DisplayName("Cliente: activo y fechaAlta se completan solos")
    void clienteCompletaDefaults() {
        Cliente guardado = clientePersistido();

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getFechaAlta()).isNotNull();
    }

    @Test
    @DisplayName("Playlist: version arranca en 1, que es lo que compara el player")
    void playlistArrancaEnVersionUno() {
        Playlist playlist = em.persistFlushFind(Playlist.builder()
                .nombre("Promos Septiembre")
                .cliente(clientePersistido())
                .build());

        assertThat(playlist.getVersion()).isEqualTo(1L);
        assertThat(playlist.getActivo()).isTrue();
        assertThat(playlist.getFechaCreacion()).isNotNull();
        assertThat(playlist.getDescripcion()).isNull();
    }

    @Test
    @DisplayName("Contenido: una IMAGEN puede no tener duracion")
    void imagenSinDuracion() {
        Contenido imagen = em.persistFlushFind(Contenido.builder()
                .nombre("Menu del dia")
                .tipo(TipoContenido.IMAGEN)
                .rutaArchivo("/media/menu.jpg")
                .nombreArchivo("menu.jpg")
                .tamanoBytes(482_133L)
                .cliente(clientePersistido())
                .build());

        assertThat(imagen.getDuracionSegundos()).isNull();
        assertThat(imagen.getFechaSubida()).isNotNull();
        assertThat(imagen.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Pantalla: nace sin playlist y sin conexion previa")
    void pantallaNaceSinAsignar() {
        Pantalla pantalla = em.persistFlushFind(Pantalla.builder()
                .codigo(codigoUnico())
                .nombre("TV Entrada")
                .sucursal(sucursalPersistida())
                .build());

        assertThat(pantalla.getActivo()).isTrue();
        assertThat(pantalla.getUltimaConexion()).isNull();
        assertThat(pantalla.getPlaylist()).isNull();
    }

    @Test
    @DisplayName("Pantalla: se le puede asignar una playlist")
    void pantallaConPlaylistAsignada() {
        Playlist playlist = em.persistFlushFind(Playlist.builder()
                .nombre("Promos Septiembre")
                .cliente(clientePersistido())
                .build());

        Pantalla pantalla = em.persistFlushFind(Pantalla.builder()
                .codigo(codigoUnico())
                .nombre("TV Salon")
                .sucursal(sucursalPersistida())
                .playlist(playlist)
                .ultimaConexion(LocalDateTime.now())
                .build());

        assertThat(pantalla.getPlaylist().getId()).isEqualTo(playlist.getId());
        assertThat(pantalla.getUltimaConexion()).isNotNull();
    }
}
