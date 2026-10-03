package com.example.demo.Repositorios;

import com.example.demo.DTO.IngresoNegocioDTO;

import com.example.demo.Entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Pago findByReservaId(Long reservaId);

    Pago findByReferenciaIgnoreCase(String referencia);

    List<Pago> findByEstadoIgnoreCase(String estado);

    /**
     * Consulta personalizada (JPQL): total recaudado por cada negocio, sumando
     * solo los pagos APROBADOS. Recorre Pago -> Reserva -> Espacio -> Negocio.
     */
    @Query("select new com.example.demo.DTO.IngresoNegocioDTO(n.nombre, sum(p.monto), count(p)) "
            + "from Pago p join p.reserva r join r.espacio e join e.negocio n "
            + "where upper(p.estado) = 'APROBADO' "
            + "group by n.nombre "
            + "order by sum(p.monto) desc")
    List<IngresoNegocioDTO> ingresosPorNegocio();
}
