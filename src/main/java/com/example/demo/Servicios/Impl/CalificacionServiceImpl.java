package com.example.demo.Servicios.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.DTO.EspacioRankingDTO;
import com.example.demo.Entidades.Calificacion;
import com.example.demo.Entidades.Reserva;
import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Repositorios.CalificacionRepository;
import com.example.demo.Servicios.CalificacionService;
import com.example.demo.Servicios.ReservaService;

@Service
@Transactional(readOnly = true)
public class CalificacionServiceImpl implements CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final ReservaService reservaService;

    public CalificacionServiceImpl(CalificacionRepository calificacionRepository, ReservaService reservaService) {
        this.calificacionRepository = calificacionRepository;
        this.reservaService = reservaService;
    }

    @Override
    public List<Calificacion> listar() {
        return calificacionRepository.findAll();
    }

    @Override
    public Calificacion buscarPorId(Long id) {
        return calificacionRepository.findById(id).orElse(null);
    }

    @Override
    public Calificacion obtenerPorId(Long id) {
        return calificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La calificacion", id));
    }

    @Override
    public List<Calificacion> listarPorEspacio(Long espacioId) {
        return calificacionRepository.findByEspacioId(espacioId);
    }

    @Override
    public List<Calificacion> listarPorUsuario(Long usuarioId) {
        return calificacionRepository.findByUsuarioId(usuarioId);
    }

    /** Usa la consulta JPQL avg(); si el espacio no tiene calificaciones devuelve 0. */
    @Override
    public double promedioPorEspacio(Long espacioId) {
        Double promedio = calificacionRepository.promedioPorEspacio(espacioId);
        return promedio == null ? 0.0 : promedio;
    }

    @Override
    public List<EspacioRankingDTO> rankingDeEspacios() {
        return calificacionRepository.rankingDeEspacios();
    }

    /**
     * Reglas de negocio: la puntuacion va de 1 a 5, la reserva no puede estar
     * cancelada y cada reserva se califica una sola vez. La calificacion hereda
     * el usuario y el espacio de la reserva.
     */
    @Override
    @Transactional
    public Calificacion guardar(Calificacion calificacion, Long reservaId) {
        if (calificacion.getPuntuacion() == null
                || calificacion.getPuntuacion() < 1 || calificacion.getPuntuacion() > 5) {
            throw new ReglaNegocioException("La puntuacion debe estar entre 1 y 5");
        }

        Reserva reserva = reservaId == null ? null : reservaService.buscarPorId(reservaId);
        if (reserva == null) {
            throw new ReglaNegocioException("Debe seleccionar una reserva valida");
        }
        if (ReservaService.CANCELADA.equalsIgnoreCase(reserva.getEstado())) {
            throw new ReglaNegocioException("No se puede calificar una reserva cancelada");
        }

        Calificacion existente = calificacionRepository.findByReservaId(reservaId);
        if (existente != null && !existente.getId().equals(calificacion.getId())) {
            throw new ReglaNegocioException("La reserva " + reservaId + " ya fue calificada");
        }

        Calificacion actual = calificacion.getId() == null ? null : buscarPorId(calificacion.getId());
        if (actual == null) {
            calificacion.setId(null);
            calificacion.setReserva(reserva);
            calificacion.setUsuario(reserva.getUsuario());
            calificacion.setEspacio(reserva.getEspacio());
            calificacion.setFecha(LocalDateTime.now());
            Calificacion guardada = calificacionRepository.save(calificacion);
            reserva.setCalificacion(guardada);
            return guardada;
        }

        // Edicion: la fecha original se conserva porque se escribe sobre la fila existente.
        actual.setReserva(reserva);
        actual.setUsuario(reserva.getUsuario());
        actual.setEspacio(reserva.getEspacio());
        actual.setPuntuacion(calificacion.getPuntuacion());
        actual.setComentario(calificacion.getComentario());
        Calificacion guardada = calificacionRepository.save(actual);
        reserva.setCalificacion(guardada);
        return guardada;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Calificacion calificacion = obtenerPorId(id);
        if (calificacion.getReserva() != null) {
            calificacion.getReserva().setCalificacion(null);
        }
        calificacionRepository.delete(calificacion);
    }
}
