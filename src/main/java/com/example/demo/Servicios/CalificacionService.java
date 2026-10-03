package com.example.demo.Servicios;

import java.util.List;

import com.example.demo.DTO.EspacioRankingDTO;
import com.example.demo.Entidades.Calificacion;

public interface CalificacionService {

    List<Calificacion> listar();

    Calificacion buscarPorId(Long id);

    Calificacion obtenerPorId(Long id);

    List<Calificacion> listarPorEspacio(Long espacioId);

    List<Calificacion> listarPorUsuario(Long usuarioId);

    /** Promedio de puntuacion de un espacio; 0 si todavia no tiene calificaciones. */
    double promedioPorEspacio(Long espacioId);

    /** Consulta personalizada: espacios ordenados por promedio de calificacion. */
    List<EspacioRankingDTO> rankingDeEspacios();

    Calificacion guardar(Calificacion calificacion, Long reservaId);

    void eliminar(Long id);
}
