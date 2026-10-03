package com.example.demo.Servicios.Impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entidades.Espacio;
import com.example.demo.Entidades.Reserva;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Repositorios.ReservaRepository;
import com.example.demo.Servicios.EspacioService;
import com.example.demo.Servicios.ReservaService;
import com.example.demo.Servicios.UsuarioService;

@Service
@Transactional(readOnly = true)
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;
    private final EspacioService espacioService;

    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              UsuarioService usuarioService,
                              EspacioService espacioService) {
        this.reservaRepository = reservaRepository;
        this.usuarioService = usuarioService;
        this.espacioService = espacioService;
    }

    @Override
    public List<Reserva> listar() {
        return reservaRepository.findAll();
    }

    @Override
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id).orElse(null);
    }

    @Override
    public Reserva obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva", id));
    }

    @Override
    public List<Reserva> listarPorUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Reserva> listarPorEmailDeUsuario(String email) {
        return reservaRepository.buscarPorEmailDeUsuario(email);
    }

    @Override
    public List<Reserva> listarPorEspacio(Long espacioId) {
        return reservaRepository.findByEspacioId(espacioId);
    }

    @Override
    public List<Reserva> listarPorEstado(String estado) {
        return reservaRepository.findByEstadoIgnoreCase(estado);
    }

    /** Convierte las filas Object[]{estado, cantidad} de la consulta en un mapa ordenado. */
    @Override
    public Map<String, Long> contarPorEstado() {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Object[] fila : reservaRepository.contarPorEstado()) {
            String estado = fila[0] == null ? "SIN ESTADO" : fila[0].toString();
            resultado.put(estado, ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    /**
     * Reglas de negocio de una reserva:
     * 1. El usuario y el espacio deben existir.
     * 2. Una reserva nueva no puede quedar en una fecha pasada.
     * 3. La hora de inicio debe ser anterior a la hora de fin.
     * 4. El espacio no puede tener otra reserva activa que se cruce con ese horario.
     */
    @Override
    @Transactional
    public Reserva guardar(Reserva reserva, Long usuarioId, Long espacioId) {
        Usuario usuario = usuarioId == null ? null : usuarioService.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new ReglaNegocioException("Debe seleccionar un usuario valido");
        }
        Espacio espacio = espacioId == null ? null : espacioService.buscarPorId(espacioId);
        if (espacio == null) {
            throw new ReglaNegocioException("Debe seleccionar un espacio valido");
        }
        if (reserva.getFecha() == null) {
            throw new ReglaNegocioException("La fecha de la reserva es obligatoria");
        }
        if (reserva.getHoraInicio() == null || reserva.getHoraFin() == null
                || !reserva.getHoraInicio().isBefore(reserva.getHoraFin())) {
            throw new ReglaNegocioException("La hora de inicio debe ser anterior a la hora de fin");
        }

        Reserva actual = reserva.getId() == null ? null : buscarPorId(reserva.getId());
        if (actual == null && reserva.getFecha().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede crear una reserva en una fecha pasada");
        }

        Long excluirId = actual == null ? -1L : actual.getId();
        boolean hayCruce = !reservaRepository.buscarCruces(espacioId, reserva.getFecha(),
                reserva.getHoraInicio(), reserva.getHoraFin(), excluirId).isEmpty();
        if (hayCruce) {
            throw new ReglaNegocioException("El espacio ya esta reservado en ese horario");
        }

        if (actual == null) {
            reserva.setId(null);
            reserva.setUsuario(usuario);
            reserva.setEspacio(espacio);
            reserva.setFechaCreacion(LocalDateTime.now());
            reserva.setEstado(PENDIENTE);
            return reservaRepository.save(reserva);
        }

        // Edicion: fecha de creacion, estado, pago y calificacion se conservan
        // porque se escribe sobre la fila que ya existe.
        actual.setUsuario(usuario);
        actual.setEspacio(espacio);
        actual.setFecha(reserva.getFecha());
        actual.setHoraInicio(reserva.getHoraInicio());
        actual.setHoraFin(reserva.getHoraFin());
        return reservaRepository.save(actual);
    }

    @Override
    @Transactional
    public Reserva cambiarEstado(Long id, String estado) {
        Reserva reserva = obtenerPorId(id);
        reserva.setEstado(estado);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = obtenerPorId(id);
        if (COMPLETADA.equalsIgnoreCase(reserva.getEstado())) {
            throw new ReglaNegocioException("No se puede cancelar una reserva que ya fue completada");
        }
        return cambiarEstado(id, CANCELADA);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        reservaRepository.delete(obtenerPorId(id));
    }
}
