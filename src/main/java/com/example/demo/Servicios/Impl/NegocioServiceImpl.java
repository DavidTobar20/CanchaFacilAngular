package com.example.demo.Servicios.Impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entidades.Negocio;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Repositorios.NegocioRepository;
import com.example.demo.Servicios.NegocioService;
import com.example.demo.Servicios.UsuarioService;

@Service
@Transactional(readOnly = true)
public class NegocioServiceImpl implements NegocioService {

    private final NegocioRepository negocioRepository;
    private final UsuarioService usuarioService;

    public NegocioServiceImpl(NegocioRepository negocioRepository, UsuarioService usuarioService) {
        this.negocioRepository = negocioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public List<Negocio> listar() {
        return negocioRepository.findAll();
    }

    @Override
    public Negocio buscarPorId(Long id) {
        return negocioRepository.findById(id).orElse(null);
    }

    @Override
    public Negocio obtenerPorId(Long id) {
        return negocioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El negocio", id));
    }

    @Override
    public List<Negocio> buscarPorNombre(String nombre) {
        return negocioRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    public List<Negocio> listarPorAdministrador(Long administradorId) {
        return negocioRepository.findByAdministradorId(administradorId);
    }

    /**
     * Regla de negocio: el NIT es unico y todo negocio debe tener un administrador
     * que exista realmente.
     */
    @Override
    @Transactional
    public Negocio guardar(Negocio negocio, Long administradorId) {
        Negocio conEseNit = negocioRepository.findByNitIgnoreCase(negocio.getNit());
        if (conEseNit != null && !conEseNit.getId().equals(negocio.getId())) {
            throw new ReglaNegocioException("Ya existe un negocio con el NIT " + negocio.getNit());
        }

        Usuario administrador = administradorId == null ? null : usuarioService.buscarPorId(administradorId);
        if (administrador == null) {
            throw new ReglaNegocioException("Debe seleccionar un administrador valido");
        }

        Negocio actual = negocio.getId() == null ? null : buscarPorId(negocio.getId());
        if (actual == null) {
            negocio.setId(null);
            negocio.setAdministrador(administrador);
            return negocioRepository.save(negocio);
        }

        // Edicion: se actualiza la fila que ya existe y su lista de espacios queda intacta.
        actual.setAdministrador(administrador);
        actual.setNombre(negocio.getNombre());
        actual.setNit(negocio.getNit());
        actual.setDireccion(negocio.getDireccion());
        actual.setDescripcion(negocio.getDescripcion());
        return negocioRepository.save(actual);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        negocioRepository.delete(obtenerPorId(id));
    }
}
