import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <footer class="footer">
      <div class="footer-container">
        <div class="footer-section">
          <h3>Về Shoppee</h3>
          <ul>
            <li><a routerLink="/about">Giới thiệu</a></li>
            <li><a routerLink="/privacy">Chính sách bảo mật</a></li>
            <li><a routerLink="/terms">Điều khoản sử dụng</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>Hỗ trợ khách hàng</h3>
          <ul>
            <li><a routerLink="/faq">Câu hỏi thường gặp</a></li>
            <li><a routerLink="/shipping">Chính sách vận chuyển</a></li>
            <li><a routerLink="/returns">Chính sách đổi trả</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>Theo dõi chúng tôi</h3>
          <div class="social-links">
            <a href="https://facebook.com" target="_blank" rel="noopener noreferrer">
              <mat-icon>facebook</mat-icon>
            </a>
            <a href="https://instagram.com" target="_blank" rel="noopener noreferrer">
              <mat-icon>photo_camera</mat-icon>
            </a>
            <a href="https://youtube.com" target="_blank" rel="noopener noreferrer">
              <mat-icon>play_circle</mat-icon>
            </a>
          </div>
        </div>

        <div class="footer-section">
          <h3>Liên hệ</h3>
          <p>
            <mat-icon>location_on</mat-icon>
            123 Đường ABC, Quận XYZ, TP.HCM
          </p>
          <p>
            <mat-icon>phone</mat-icon>
            1900 1234
          </p>
          <p>
            <mat-icon>email</mat-icon>
            support@shoppee.com
          </p>
        </div>
      </div>

      <div class="footer-bottom">
        <p>&copy; 2025 Shoppee. Tất cả các quyền được bảo lưu.</p>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background-color: #1976d2;
      color: white;
      padding: 48px 0 0;
      margin-top: 48px;
    }

    .footer-container {
      max-width: 1200px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 32px;
      padding: 0 16px;
    }

    .footer-section {
      h3 {
        font-size: 1.1rem;
        font-weight: 600;
        margin-bottom: 16px;
        color: white;
      }

      ul {
        list-style: none;
        padding: 0;
        margin: 0;

        li {
          margin-bottom: 8px;
        }
      }

      a {
        color: rgba(255, 255, 255, 0.8);
        text-decoration: none;
        transition: color 0.2s;

        &:hover {
          color: white;
        }
      }
    }

    .social-links {
      display: flex;
      gap: 16px;

      a {
        color: white;
        
        &:hover {
          color: rgba(255, 255, 255, 0.8);
        }
      }
    }

    .footer-bottom {
      margin-top: 48px;
      padding: 16px;
      text-align: center;
      background: rgba(0, 0, 0, 0.1);
      
      p {
        margin: 0;
        color: rgba(255, 255, 255, 0.8);
      }
    }

    @media (max-width: 768px) {
      .footer-container {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 480px) {
      .footer-container {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class Footer {}
