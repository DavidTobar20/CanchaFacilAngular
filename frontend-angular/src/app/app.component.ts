import { Component, signal } from '@angular/core';
import { Usuarios } from './components/usuarios/usuarios.component';

@Component({
  imports: [Usuarios],
  selector: 'app-root',
  styleUrl: './app.component.css',
  templateUrl: './app.component.html',
})
export class App {
  protected readonly title = signal('frontend-angular');
}
