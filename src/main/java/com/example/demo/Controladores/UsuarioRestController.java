package com.example.demo.Controladores;

import com.example.demo.Entidades.Usuario;
import com.example.demo.Entidades.Rol;
import com.example.demo.DTO.UsuarioDTO;
import com.example.demo.Servicios.UsuarioService;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de la entidad Usuario. Expone los servicios de la API para
 * consultar usuarios.
 *
 * Nunca devuelve la entidad Usuario directamente: siempre la convierte a
 * UsuarioDTO, asi el JSON no incluye el password ni las relaciones .
 */
// Marca esta clase como controlador que devuelve datos, normalmente JSON.
@RestController
// Agrega esta ruta base a todos los endpoints de esta clase.
@RequestMapping("/api/usuarios")
public class UsuarioRestController {

    // Servicio que contiene las operaciones de negocio de usuarios.
    private final UsuarioService usuarioService;

    // Spring entrega el servicio al crear el controlador.
    public UsuarioRestController(UsuarioService usuarioService) {
        // Guarda el servicio para usarlo en los endpoints.
        this.usuarioService = usuarioService;
    }

    // Atiende GET /api/usuarios para consultar todos los usuarios.
    @GetMapping
    public List<UsuarioDTO> listar() {
        // Obtiene los usuarios, convierte cada uno a DTO y arma la lista de respuesta.
        return usuarioService.listar().stream()
                .map(UsuarioDTO::desde)
                .toList();
    }

    // Atiende GET /api/usuarios/{id}; toma el id desde la URL.
    @GetMapping("/{id}")
    public UsuarioDTO buscarPorId(@PathVariable Long id) {
        // Busca el usuario y convierte el resultado a DTO antes de responder.
        return UsuarioDTO.desde(usuarioService.buscarPorId(id));
    }

    // Atiende POST /api/usuarios para crear un usuario.
    @PostMapping
    public UsuarioDTO crear(@RequestBody Usuario usuario) {
        // Convierte el JSON recibido en Usuario; el servicio lo guarda y el DTO
        // evita incluir la contrasena cifrada en la respuesta.
        return UsuarioDTO.desde(usuarioService.guardar(usuario));
    }

    // Atiende PUT /api/usuarios/{id} para actualizar un usuario existente.
    @PutMapping("/{id}")
    public UsuarioDTO actualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuario) {
        // Confirma que el ID exista; de lo contrario, no debe crear otro usuario.
        usuarioService.obtenerPorId(id);
        // Usa el ID de la URL y no uno que pudiera venir en el JSON.
        usuario.setId(id);
        // Guarda los cambios y devuelve el DTO sin exponer el password.
        return UsuarioDTO.desde(usuarioService.guardar(usuario));
    }
    //Atiende DELETE /api/usuarios/{id} para eliminar un usuario existente.
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }

    // Atiende GET /api/usuarios/rol/{rol} para filtrar por CLIENTE o ADMINISTRADOR.
    @GetMapping("/rol/{rol}")
    public List<UsuarioDTO> listarPorRol(@PathVariable Rol rol) {
        // Busca por rol y convierte los resultados a DTO antes de responder.
        return usuarioService.listarPorRol(rol).stream()
                .map(UsuarioDTO::desde)
                .toList();
    }

}
