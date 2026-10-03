package com.example.demo.Servicios;

import java.util.List;

import com.example.demo.DTO.IngresoNegocioDTO;
import com.example.demo.Entidades.Pago;

public interface PagoService {

    /** Estados posibles de un pago. */
    String PENDIENTE = "PENDIENTE";
    String APROBADO = "APROBADO";
    String RECHAZADO = "RECHAZADO";
    String REEMBOLSADO = "REEMBOLSADO";

    List<Pago> listar();

    Pago buscarPorId(Long id);

    Pago obtenerPorId(Long id);

    Pago buscarPorReserva(Long reservaId);

    List<Pago> listarPorEstado(String estado);

    /** Consulta personalizada: total recaudado (pagos aprobados) por negocio. */
    List<IngresoNegocioDTO> ingresosPorNegocio();

    Pago guardar(Pago pago, Long reservaId);

    Pago cambiarEstado(Long id, String estado);

    void eliminar(Long id);
}
