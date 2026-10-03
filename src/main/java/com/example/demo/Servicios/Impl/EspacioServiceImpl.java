package com.example.demo.Servicios.Impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entidades.Espacio;
import com.example.demo.Entidades.Negocio;
import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Repositorios.EspacioRepository;
import com.example.demo.Servicios.EspacioService;
import com.example.demo.Servicios.NegocioService;

@Service
@Transactional(readOnly = true)
public class EspacioServiceImpl implements EspacioService {

    private final EspacioRepository espacioRepository;
    private final NegocioService negocioService;

    public EspacioServiceImpl(EspacioRepository espacioRepository, NegocioService negocioService) {
        this.espacioRepository = espacioRepository;
        this.negocioService = negocioService;
    }

    @Override
    public List<Espacio> listar() {
        return espacioRepository.findAll();
    }

    @Override
    public Espacio buscarPorId(Long id) {
        return espacioRepository.findById(id).orElse(null);
    }

    @Override
    public Espacio obtenerPorId(Long id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El espacio", id));
    }

    @Override
    public List<Espacio> listarPorNegocio(Long negocioId) {
        return espacioRepository.findByNegocioId(negocioId);
    }

    @Override
    public List<Espacio> buscarPorDeporte(String tipoDeporte) {
        return espacioRepository.findByTipoDeporteIgnoreCase(tipoDeporte);
    }

    @Override
    public List<Espacio> buscarPorRangoDePrecio(BigDecimal min, BigDecimal max) {
        if (min == null) {
            min = BigDecimal.ZERO;
        }
        if (max == null) {
            max = new BigDecimal("99999999.99");
        }
        if (min.compareTo(max) > 0) {
            throw new ReglaNegocioException("El precio minimo no puede ser mayor que el maximo");
        }
        return espacioRepository.buscarPorRangoDePrecio(min, max);
    }

    /**
     * Regla de negocio: el precio por hora no puede ser negativo y el espacio
     * siempre pertenece a un negocio existente.
     */
    @Override
    @Transactional
    public Espacio guardar(Espacio espacio, Long negocioId) {
        if (espacio.getPrecioHora() == null || espacio.getPrecioHora().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El precio por hora no puede ser negativo");
        }

        Negocio negocio = negocioId == null ? null : negocioService.buscarPorId(negocioId);
        if (negocio == null) {
            throw new ReglaNegocioException("Debe seleccionar un negocio valido");
        }

        Espacio actual = espacio.getId() == null ? null : buscarPorId(espacio.getId());
        if (actual == null) {
            espacio.setId(null);
            espacio.setNegocio(negocio);
            return espacioRepository.save(espacio);
        }

        // Edicion: reservas y calificaciones ya guardadas se conservan solas.
        actual.setNegocio(negocio);
        actual.setNombre(espacio.getNombre());
        actual.setTipoDeporte(espacio.getTipoDeporte());
        actual.setPrecioHora(espacio.getPrecioHora());
        actual.setCapacidad(espacio.getCapacidad());
        actual.setDescripcion(espacio.getDescripcion());
        return espacioRepository.save(actual);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        espacioRepository.delete(obtenerPorId(id));
    }
}
