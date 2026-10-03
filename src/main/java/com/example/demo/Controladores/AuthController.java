package com.example.demo.Controladores;

import com.example.demo.Servicios.SesionActual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.DTO.RegistroDTO;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.UsuarioService;

import jakarta.validation.Valid;

/**
 * Login y registro de usuarios.
 *
 * El POST de /login no esta aqui: lo procesa Spring Security (ver SeguridadConfig).
 * Este controlador solo muestra la pagina de login y maneja el registro.
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final SesionActual sesion;

    public AuthController(UsuarioService usuarioService, SesionActual sesion) {
        this.usuarioService = usuarioService;
        this.sesion = sesion;
    }

    /** GET /login -> formulario de inicio de sesion. */
    @GetMapping("/login")
    public String login() {
        if (sesion.isAutenticado()) {
            return "redirect:/";
        }
        return "auth/login";
    }

    /** GET /registro -> formulario de registro vacio. */
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        if (sesion.isAutenticado()) {
            return "redirect:/";
        }
        model.addAttribute("registro", new RegistroDTO());
        return "auth/registro";
    }

    /**
     * POST /registro -> valida el formulario (@Valid), revisa las reglas de negocio
     * y crea la cuenta con rol CLIENTE. Luego manda al login con un aviso.
     */
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroDTO registro,
                            BindingResult result) {
        if (registro.getPassword() != null && registro.getConfirmarPassword() != null
                && !registro.getPassword().equals(registro.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "noCoincide", "Las contrasenas no coinciden");
        }
        if (result.hasErrors()) {
            return "auth/registro";
        }
        try {
            usuarioService.registrar(registro);
        } catch (ReglaNegocioException ex) {
            result.reject("registro", ex.getMessage());
            return "auth/registro";
        }
        return "redirect:/login?registrado=true";
    }
}
