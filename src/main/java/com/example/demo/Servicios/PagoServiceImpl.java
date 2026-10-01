package com.example.demo.Servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Repositorios.IngresoNegocioDTO;
import com.example.demo.Entidades.Pago;
import com.example.demo.Entidades.Reserva;
import com.example.demo.Repositorios.PagoRepository;

@Service
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(PENDIENTE, APROBADO, RECHAZADO, REEMBOLSADO);

    private final PagoRepository pagoRepository;
    private final ReservaService reservaService;

    public PagoServiceImpl(PagoRepository pagoRepository, ReservaService reservaService) {
        this.pagoRepository = pagoRepository;
        this.reservaService = reservaService;
    }

    @Override
    public List<Pago> listar() {
        return pagoRepository.findAll();
    }

    @Override
    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id).orElse(null);
    }

    @Override
    public Pago obtenerPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pago", id));
    }

    @Override
    public Pago buscarPorReserva(Long reservaId) {
        return pagoRepository.findByReservaId(reservaId);
    }

    @Override
    public List<Pago> listarPorEstado(String estado) {
        return pagoRepository.findByEstadoIgnoreCase(estado);
    }

    @Override
    public List<IngresoNegocioDTO> ingresosPorNegocio() {
        return pagoRepository.ingresosPorNegocio();
    }

    /**
     * Reglas de negocio: el monto no puede ser negativo y una reserva solo puede
     * tener un pago asociado.
     */
    @Override
    @Transactional
    public Pago guardar(Pago pago, Long reservaId) {
        if (pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El monto del pago no puede ser negativo");
        }

        Reserva reserva = reservaId == null ? null : reservaService.buscarPorId(reservaId);
        if (reserva == null) {
            throw new ReglaNegocioException("Debe seleccionar una reserva valida");
        }

        Pago existente = pagoRepository.findByReservaId(reservaId);
        if (existente != null && !existente.getId().equals(pago.getId())) {
            throw new ReglaNegocioException("La reserva " + reservaId + " ya tiene un pago registrado");
        }

        Pago actual = pago.getId() == null ? null : buscarPorId(pago.getId());
        if (actual == null) {
            pago.setId(null);
            pago.setReserva(reserva);
            pago.setFechaPago(LocalDateTime.now());
            pago.setEstado(PENDIENTE);
            Pago guardado = pagoRepository.save(pago);
            reserva.setPago(guardado);
            return guardado;
        }

        // Edicion: la fecha y el estado no se tocan desde el formulario; el estado
        // se cambia con cambiarEstado().
        actual.setReserva(reserva);
        actual.setMonto(pago.getMonto());
        actual.setMetodoPago(pago.getMetodoPago());
        actual.setReferencia(pago.getReferencia());
        Pago guardado = pagoRepository.save(actual);
        reserva.setPago(guardado);
        return guardado;
    }

    /**
     * Al aprobar el pago la reserva queda confirmada; si se rechaza o se
     * reembolsa, la reserva se cancela.
     */
    @Override
    @Transactional
    public Pago cambiarEstado(Long id, String estado) {
        String nuevoEstado = estado == null ? "" : estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new ReglaNegocioException("Estado de pago no valido: " + estado);
        }

        Pago pago = obtenerPorId(id);
        pago.setEstado(nuevoEstado);
        pagoRepository.save(pago);

        if (pago.getReserva() != null) {
            if (APROBADO.equals(nuevoEstado)) {
                reservaService.cambiarEstado(pago.getReserva().getId(), ReservaService.CONFIRMADA);
            } else if (RECHAZADO.equals(nuevoEstado) || REEMBOLSADO.equals(nuevoEstado)) {
                reservaService.cambiarEstado(pago.getReserva().getId(), ReservaService.CANCELADA);
            }
        }
        return pago;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Pago pago = obtenerPorId(id);
        // Se desengancha del lado inverso para que la reserva en memoria quede
        // coherente; la fila que se borra es la de pagos.
        if (pago.getReserva() != null) {
            pago.getReserva().setPago(null);
        }
        pagoRepository.delete(pago);
    }
}
