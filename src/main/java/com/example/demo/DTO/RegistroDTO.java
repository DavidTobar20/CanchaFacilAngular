package com.example.demo.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos del formulario de registro.
 *
 * Se usa un DTO en vez de la entidad Usuario porque el formulario tiene campos
 * que no existen en la tabla (confirmarPassword) y no debe dejar elegir el rol:
 * todo el que se registra entra como CLIENTE.
 */
@Data
@NoArgsConstructor
public class RegistroDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 6, max = 60, message = "La contrasena debe tener entre 6 y 60 caracteres")
    private String password;

    @NotBlank(message = "Debe confirmar la contrasena")
    private String confirmarPassword;

    @Pattern(regexp = "^$|^[0-9+ ]{7,15}$", message = "El telefono debe tener entre 7 y 15 digitos")
    private String telefono;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 60, message = "La direccion no puede superar 60 caracteres")
    private String direccion;
}
