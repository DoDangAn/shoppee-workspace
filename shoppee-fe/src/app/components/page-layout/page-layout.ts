import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-page-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="page-container">
      <h2 class="page-title">
        <mat-icon *ngIf="icon">{{icon}}</mat-icon>
        {{title}}
      </h2>
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .page-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      min-height: calc(100vh - 64px);
    }

    .page-title {
      font-size: 2rem;
      font-weight: 600;
      color: #1976d2;
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 3px solid #1976d2;
      display: flex;
      align-items: center;
      gap: 12px;

      mat-icon {
        font-size: 2rem;
        width: 2rem;
        height: 2rem;
      }
    }

    @media (max-width: 600px) {
      .page-container {
        padding: 16px;
      }
    }
  `]
})
export class PageLayout {
  @Input() title: string = '';
  @Input() icon?: string;
}
