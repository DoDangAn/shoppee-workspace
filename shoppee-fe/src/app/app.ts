
import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HttpClientModule
  ],
  template: '<router-outlet></router-outlet>'
})
export class App {
  protected readonly title = signal('shoppee-fe');
}
