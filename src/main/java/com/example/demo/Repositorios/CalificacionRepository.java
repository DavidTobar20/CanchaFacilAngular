package com.example.demo.Repositorios;

import com.example.demo.DTO.EspacioRankingDTO;

import com.example.demo.Entidades.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    List<Calificacion> findByEspacioId(Long espacioId);

    List<Calificacion> findByUsuarioId(Long usuarioId);

    Calificacion findByReservaId(Long reservaId);

    /** Consulta personalizada (JPQL): promedio de puntuacion de un espacio; null si no tiene. */
    @Query("select avg(c.puntuacion) from Calificacion c where c.espacio.id = :espacioId")
    Double promedioPorEspacio(@Param("espacioId") Long espacioId);

    /**
     * Consulta personalizada (JPQL con constructor): ranking de espacios por
     * promedio de calificacion. "select new" arma un DTO por cada fila.
     */
    @Query("select new com.example.demo.DTO.EspacioRankingDTO("
            + "e.id, e.nombre, e.tipoDeporte, avg(c.puntuacion), count(c)) "
            + "from Calificacion c join c.espacio e "
            + "group by e.id, e.nombre, e.tipoDeporte "
            + "order by avg(c.puntuacion) desc, count(c) desc")
    List<EspacioRankingDTO> rankingDeEspacios();
}
