import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from '../navbar/navbar';
import { Footer } from '../footer/footer';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, Navbar, Footer],
  template: `
    <div class="layout">
      <app-navbar></app-navbar>
      <main class="main-content">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
    </div>
  `,
  styles: [`
    .layout {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .main-content {
      flex: 1;
      margin-top: 64px; // Height of navbar
      background-color: #f5f5f5;
    }

    @media (max-width: 600px) {
      .main-content {
        margin-top: 56px;
      }
    }
  `]
})
export class Layout {}
