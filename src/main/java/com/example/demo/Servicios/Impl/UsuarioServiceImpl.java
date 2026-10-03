package com.example.demo.Servicios.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.DTO.RegistroDTO;
import com.example.demo.Entidades.Rol;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Repositorios.NegocioRepository;
import com.example.demo.Repositorios.UsuarioRepository;
import com.example.demo.Servicios.UsuarioService;

/**
 * Implementacion de UsuarioService: aqui viven las reglas de negocio de los usuarios.
 * El controlador llama al servicio y el servicio llama al repositorio.
 *
 * @Transactional(readOnly = true) a nivel de clase: todas las consultas viajan en
 * una transaccion de solo lectura; los metodos que escriben lo sobreescriben con
 * su propio @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private static final int LONGITUD_MINIMA_PASSWORD = 6;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    // Se usa el repositorio (y no NegocioService) porque NegocioServiceImpl ya depende
    // de UsuarioService: inyectar el servicio crearia una dependencia circular.
    private final NegocioRepository negocioRepository;

    // Inyeccion de dependencias por constructor.
    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              NegocioRepository negocioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.negocioRepository = negocioRepository;
    }

    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario", id));
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public List<Usuario> buscarPorNombre(String nombre) {
        return usuarioRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    public List<Usuario> listarPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
    }

    /**
     * Reglas de negocio:
     * - el email no se puede repetir;
     * - al crear, la contrasena es obligatoria (minimo 6 caracteres) y se guarda cifrada;
     * - al editar, si la contrasena llega vacia se conserva la actual;
     * - la fecha de registro se asigna sola la primera vez.
     *
     * En la edicion se copian los campos sobre la entidad que ya esta en la BD
     * para conservar la fecha de registro y las listas relacionadas.
     */
    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        Usuario conEseEmail = usuarioRepository.findByEmailIgnoreCase(usuario.getEmail());
        if (conEseEmail != null && !conEseEmail.getId().equals(usuario.getId())) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + usuario.getEmail());
        }

        String password = usuario.getPassword();
        boolean passwordVacia = password == null || password.isBlank();
        if (!passwordVacia && password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new ReglaNegocioException("La contrasena debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
        }

        Usuario actual = usuario.getId() == null ? null : buscarPorId(usuario.getId());
        if (actual == null) {
            if (passwordVacia) {
                throw new ReglaNegocioException("La contrasena es obligatoria");
            }
            usuario.setId(null);
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setFechaRegistro(LocalDateTime.now());
            return usuarioRepository.save(usuario);
        }

        actual.setNombre(usuario.getNombre());
        actual.setEmail(usuario.getEmail());
        if (!passwordVacia) {
            actual.setPassword(passwordEncoder.encode(password));
        }
        actual.setTelefono(usuario.getTelefono());
        actual.setRol(usuario.getRol());
        actual.setDireccion(usuario.getDireccion());
        return usuarioRepository.save(actual);
    }

    /**
     * Registro publico. Reglas: las dos contrasenas deben coincidir, el email no
     * puede existir y el nuevo usuario siempre entra como CLIENTE.
     */
    @Override
    @Transactional
    public Usuario registrar(RegistroDTO registro) {
        if (!registro.getPassword().equals(registro.getConfirmarPassword())) {
            throw new ReglaNegocioException("Las contrasenas no coinciden");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(registro.getEmail())) {
            throw new ReglaNegocioException("Ya existe una cuenta con el email " + registro.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(registro.getNombre().trim());
        usuario.setEmail(registro.getEmail().trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(registro.getPassword()));
        usuario.setTelefono(registro.getTelefono());
        usuario.setDireccion(registro.getDireccion());
        usuario.setRol(Rol.CLIENTE);
        usuario.setFechaRegistro(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = obtenerPorId(id);
        // Usuario ya no tiene la lista de negocios (relacion unidireccional), asi que el
        // cascade no los borra solo: se eliminan primero sus negocios (y con ellos, por
        // cascade de Negocio, sus espacios) y despues el usuario.
        negocioRepository.deleteAll(negocioRepository.findByAdministradorId(id));
        usuarioRepository.delete(usuario);
    }
}
