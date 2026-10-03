package com.example.demo.Entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Lombok: @Data genera getters, setters, toString, equals y hashCode.
// @NoArgsConstructor lo necesita Thymeleaf/Spring (y tambien JPA) para crear el objeto vacio.
//
// JPA: @Entity marca la clase como tabla; @Table le pone el nombre.
// Las asociaciones se excluyen de toString/equals para no provocar recursion
// infinita entre padre e hijo ni disparar cargas perezosas sin querer.
@Entity
@Table(name = "usuarios")
@Data
@ToString(exclude = {"reservas", "calificaciones", "notificaciones"})
@EqualsAndHashCode(exclude = {"reservas", "calificaciones", "notificaciones"})
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    // IDENTITY = la secuencia la genera PostgreSQL con una columna BIGSERIAL.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nombre;

    // El email es unico: la regla de negocio del servicio queda respaldada por la BD.
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Aqui se guarda el hash BCrypt, nunca la contrasena en texto plano.
    // Que sea obligatoria al crear se revisa en el controlador/servicio, porque
    // al editar se permite dejarla vacia para conservar la actual.
    @Size(max = 200, message = "La contrasena no puede superar 200 caracteres")
    @Column(nullable = false, length = 200)
    private String password;

    @Pattern(regexp = "^$|^[0-9+ ]{7,15}$", message = "El telefono debe tener entre 7 y 15 digitos")
    @Column(length = 30)
    private String telefono;

    // STRING guarda "CLIENTE"/"ADMINISTRADOR" en vez de 0/1: si manana se agrega
    // un rol en medio del enum, los datos ya guardados siguen siendo correctos.
    @NotNull(message = "Debe seleccionar un rol")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // El campo se llamaba "Direccion" (con mayuscula): se renombra a "direccion" para
    // que coincida con la propiedad del formulario y con los mensajes de validacion.
    // La columna en la BD sigue siendo la misma (direccion).
    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 60, message = "La direccion no puede superar 60 caracteres")
    @Column(nullable = false, length = 60)
    private String direccion;

    // La relacion con Negocio es UNIDIRECCIONAL: solo Negocio conoce a su administrador.
    // Antes habia aqui una List<Negocio> negocios, pero Usuario -> negocios -> administrador
    // formaba un ciclo. Los negocios de un usuario se consultan con
    // NegocioRepository.findByAdministradorId(id).

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Reserva> reservas = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Calificacion> calificaciones = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Notificacion> notificaciones = new ArrayList<>();
    
}
