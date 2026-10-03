package com.example.demo.Servicios;

import java.util.List;

import com.example.demo.DTO.RegistroDTO;
import com.example.demo.Entidades.Rol;
import com.example.demo.Entidades.Usuario;

/**
 * Contrato de la capa de servicio para usuarios.
 * Los controladores dependen de esta interfaz; la logica vive en UsuarioServiceImpl.
 */
public interface UsuarioService {

    List<Usuario> listar();

    /** Devuelve el usuario o null si no existe (lo usa el API REST). */
    Usuario buscarPorId(Long id);

    /** Devuelve el usuario o lanza RecursoNoEncontradoException (pagina 404). */
    Usuario obtenerPorId(Long id);

    Usuario buscarPorEmail(String email);

    List<Usuario> buscarPorNombre(String nombre);

    List<Usuario> listarPorRol(Rol rol);

    /** Crea o edita un usuario desde el CRUD (lo usa el administrador). */
    Usuario guardar(Usuario usuario);

    /** Registro publico: crea un usuario con rol CLIENTE y contrasena cifrada. */
    Usuario registrar(RegistroDTO registro);

    void eliminar(Long id);
}
