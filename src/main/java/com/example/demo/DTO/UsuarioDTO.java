package com.example.demo.DTO;

import java.time.LocalDateTime;

import com.example.demo.Entidades.Rol;
import com.example.demo.Entidades.Usuario;

/**
 * DTO (Data Transfer Object) de salida para la API REST de usuarios.
 *
 * La entidad Usuario representa la tabla de la base de datos; este DTO representa
 * lo que el frontend (Angular) puede ver. Se deja afuera a proposito:
 *  - password: el hash nunca debe salir del backend.
 *  - negocios, reservas, calificaciones, notificaciones: son relaciones con otras
 *    entidades que ademas apuntan de vuelta al usuario, lo que provocaba un JSON
 *    infinito (usuario -> negocios -> administrador -> negocios -> ...).
 *
 * Es un "record" de Java: una clase inmutable que genera sola el constructor,
 * los getters (id(), nombre(), ...), equals, hashCode y toString.
 */
public record UsuarioDTO(
        Long id,
        String nombre,
        String email,
        String telefono,
        Rol rol,
        String direccion,
        LocalDateTime fechaRegistro) {

    /** Convierte una entidad Usuario en el DTO que se envia como JSON. */
    public static UsuarioDTO desde(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getDireccion(),
                usuario.getFechaRegistro());
    }
}
