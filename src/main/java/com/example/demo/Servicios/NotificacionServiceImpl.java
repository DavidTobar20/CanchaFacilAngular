package com.example.demo.Servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entidades.Notificacion;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Repositorios.NotificacionRepository;

@Service
@Transactional(readOnly = true)
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioService usuarioService;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository, UsuarioService usuarioService) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public List<Notificacion> listar() {
        return notificacionRepository.findAll();
    }

    @Override
    public Notificacion buscarPorId(Long id) {
        return notificacionRepository.findById(id).orElse(null);
    }

    @Override
    public Notificacion obtenerPorId(Long id) {
        return notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificacion", id));
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Notificacion> listarNoLeidas(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdYNoLeidas(usuarioId);
    }

    /** Toda notificacion nace no leida y con la fecha del momento en que se crea. */
    @Override
    @Transactional
    public Notificacion guardar(Notificacion notificacion, Long usuarioId) {
        Usuario usuario = usuarioId == null ? null : usuarioService.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new ReglaNegocioException("Debe seleccionar un usuario valido");
        }

        Notificacion actual = notificacion.getId() == null ? null : buscarPorId(notificacion.getId());
        if (actual == null) {
            notificacion.setId(null);
            notificacion.setUsuario(usuario);
            notificacion.setFecha(LocalDateTime.now());
            notificacion.setLeido(false);
            return notificacionRepository.save(notificacion);
        }

        // Edicion: la fecha y el estado de lectura se conservan.
        actual.setUsuario(usuario);
        actual.setTipo(notificacion.getTipo());
        actual.setMensaje(notificacion.getMensaje());
        return notificacionRepository.save(actual);
    }

    @Override
    @Transactional
    public Notificacion marcarComoLeida(Long id) {
        Notificacion notificacion = obtenerPorId(id);
        notificacion.setLeido(true);
        return notificacionRepository.save(notificacion);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        notificacionRepository.delete(obtenerPorId(id));
    }
}
