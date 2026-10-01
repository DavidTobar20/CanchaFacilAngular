import { CommonModule, DatePipe } from '@angular/common'; // Importa utilidades comunes y el pipe para fechas.
import { Component } from '@angular/core'; // Permite definir el componente Angular.
import { FormsModule } from '@angular/forms'; // Habilita el enlace de datos de los formularios.

interface Usuario { // Define la estructura de cada usuario.
  id: number; // Identificador único del usuario.
  nombre: string; // Nombre completo del usuario.
  email: string; // Correo electrónico del usuario.
  telefono: string; // Número telefónico del usuario.
  rol: 'ADMINISTRADOR' | 'CLIENTE'; // Limita el rol a estas dos opciones.
  fechaRegistro: string; // Guarda la fecha de registro como texto.
}

@Component({// decorador: patron de diseño de software que permite agregar funcionalidades a una clase 
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  selector: 'app-usuarios',
  styleUrl: './usuarios.component.css',
  templateUrl: './usuarios.component.html',
})
export class Usuarios {
  filtro = '';

  usuarios: Usuario[] = [
    {
      id: 1,
      nombre: 'Christian Medina',
      email: 'christian@javeriana.edu.co',
      telefono: '3132178001',
      rol: 'CLIENTE',
      fechaRegistro: '2026-09-07',
    },
    {
      id: 2,
      nombre: 'Laura Gómez',
      email: 'laura@correo.com',
      telefono: '3001234567',
      rol: 'CLIENTE',
      fechaRegistro: '2026-09-15',
    },
    {
      id: 3,
      nombre: 'Mateo Ruiz',
      email: 'mateo@canchafacil.com',
      telefono: '3207654321',
      rol: 'ADMINISTRADOR',
      fechaRegistro: '2026-08-20',
    },
    {
      id: 4,
      nombre: 'Valentina López',
      email: 'valentina@correo.com',
      telefono: '3119876543',
      rol: 'CLIENTE',
      fechaRegistro: '2026-09-24',
    },
  ];

  get usuariosFiltrados(): Usuario[] {
    const texto = this.filtro.trim().toLowerCase();

    if (!texto) {
      return this.usuarios;
    }

    return this.usuarios.filter((usuario) =>
      usuario.nombre.toLowerCase().includes(texto) ||
      usuario.email.toLowerCase().includes(texto) ||
      usuario.rol.toLowerCase().includes(texto)
    );
  }

  limpiarFiltro(): void {
    this.filtro = '';
  }
}
