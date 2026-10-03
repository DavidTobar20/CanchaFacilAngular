package com.example.demo.Controladores;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Entidades.Rol;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Servicios.NegocioService;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.UsuarioService;

import jakarta.validation.Valid;

/**
 * Controlador de la entidad Usuario (solo para ADMINISTRADOR, ver SeguridadConfig).
 * Recibe las peticiones del navegador, pide la logica al servicio y devuelve
 * el nombre de la plantilla Thymeleaf que se debe mostrar.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final NegocioService negocioService;

    public UsuarioController(UsuarioService usuarioService, NegocioService negocioService) {
        this.usuarioService = usuarioService;
        this.negocioService = negocioService;
    }

    /** GET /usuarios -> lista completa. */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("titulo", "Usuarios");
        return "usuarios/lista";
    }

    /** GET /usuarios?nombre=Juan -> busqueda por nombre. */
    @GetMapping(params = "nombre")
    public String buscarPorNombre(@RequestParam String nombre, Model model) {
        model.addAttribute("usuarios", usuarioService.buscarPorNombre(nombre));
        model.addAttribute("titulo", "Usuarios que coinciden con: " + nombre);
        return "usuarios/lista";
    }

    /** GET /usuarios/add -> formulario vacio para crear. */
    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("usuario", new Usuario());
        prepararFormulario(model, "Crear usuario");
        return "usuarios/formulario";
    }

    /** GET /usuarios/update/5 -> formulario con los datos del usuario 5 (404 si no existe). */
    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("usuario", usuarioService.obtenerPorId(id));
        prepararFormulario(model, "Editar usuario");
        return "usuarios/formulario";
    }

    /**
     * POST /usuarios/add -> guarda el usuario que viene del formulario.
     * @Valid dispara las validaciones de la entidad y los errores quedan en
     * BindingResult, que Thymeleaf muestra junto a cada campo.
     */
    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("usuario") Usuario usuario,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirect) {
        boolean esNuevo = usuario.getId() == null;
        if (esNuevo && (usuario.getPassword() == null || usuario.getPassword().isBlank())) {
            result.rejectValue("password", "obligatoria", "La contrasena es obligatoria");
        }
        if (result.hasErrors()) {
            prepararFormulario(model, esNuevo ? "Crear usuario" : "Editar usuario");
            return "usuarios/formulario";
        }
        try {
            usuarioService.guardar(usuario);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, esNuevo ? "Crear usuario" : "Editar usuario");
            return "usuarios/formulario";
        }
        redirect.addFlashAttribute("exito", "Usuario guardado correctamente");
        return "redirect:/usuarios";
    }

    /** GET /usuarios/delete/5 -> elimina el usuario 5. */
    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        usuarioService.eliminar(id);
        redirect.addFlashAttribute("exito", "Usuario eliminado");
        return "redirect:/usuarios";
    }

    /** GET /usuarios/5 -> detalle de un usuario. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("usuario", usuarioService.obtenerPorId(id));
        // Usuario ya no tiene getNegocios(): se cuentan desde el servicio de negocios.
        model.addAttribute("totalNegocios", negocioService.listarPorAdministrador(id).size());
        return "usuarios/detalle";
    }

    private void prepararFormulario(Model model, String accion) {
        model.addAttribute("roles", Rol.values());
        model.addAttribute("accion", accion);
    }
}
