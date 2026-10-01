package com.example.demo.Controladores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Manejo centralizado de errores para las paginas Thymeleaf.
 *
 * Cualquier excepcion que salga de un controlador (y que el controlador no
 * atrape) llega aqui y se muestra la plantilla templates/error.html con el
 * codigo HTTP correcto, en vez de la pagina blanca de Spring.
 *
 * Se limita a los controladores MVC con assignableTypes para no afectar al
 * API REST (UsuarioRestController), que sigue respondiendo como antes.
 */
@ControllerAdvice(assignableTypes = {
        HomeController.class, AuthController.class, ConsultaController.class,
        UsuarioController.class, NegocioController.class, EspacioController.class,
        ReservaController.class, PagoController.class, CalificacionController.class,
        NotificacionController.class, PerfilController.class
})
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    /** Registro inexistente: /espacios/999 -> 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return pagina(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request);
    }

    /** Regla de negocio violada fuera de un formulario (ej: cancelar una reserva completada). */
    @ExceptionHandler(ReglaNegocioException.class)
    public ModelAndView reglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return pagina(HttpStatus.BAD_REQUEST, "Operacion no permitida", ex.getMessage(), request);
    }

    /** Parametro con tipo incorrecto (/usuarios/abc) o parametro obligatorio ausente. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ModelAndView peticionInvalida(Exception ex, HttpServletRequest request) {
        return pagina(HttpStatus.BAD_REQUEST, "Peticion invalida",
                "Los datos enviados en la direccion no son validos.", request);
    }

    /** Datos que no pasan las validaciones de la entidad al guardar. */
    @ExceptionHandler({ConstraintViolationException.class, TransactionSystemException.class})
    public ModelAndView datosInvalidos(Exception ex, HttpServletRequest request) {
        log.warn("Datos invalidos en {}: {}", request.getRequestURI(), ex.getMessage());
        return pagina(HttpStatus.BAD_REQUEST, "Datos invalidos",
                "Alguno de los datos no cumple las validaciones. Revise el formulario e intente de nuevo.", request);
    }

    /** Violacion de llaves foraneas o campos unicos en la base de datos. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView integridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Error de integridad en {}: {}", request.getRequestURI(), ex.getMessage());
        return pagina(HttpStatus.CONFLICT, "No se pudo completar la operacion",
                "El registro esta relacionado con otros datos o tiene un valor repetido.", request);
    }

    /** Cualquier otro error inesperado -> 500, y se deja el detalle en el log. */
    @ExceptionHandler(Exception.class)
    public ModelAndView general(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return pagina(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrio un error inesperado. Intente de nuevo mas tarde.", request);
    }

    private ModelAndView pagina(HttpStatus status, String titulo, String mensaje, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", titulo);
        mav.addObject("message", mensaje);
        mav.addObject("path", request.getRequestURI());
        return mav;
    }
}
