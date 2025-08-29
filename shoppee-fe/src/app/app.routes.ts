import { Routes } from '@angular/router';
import { Layout } from './components/layout/layout';

export const routes: Routes = [
  {
    path: '',
    component: Layout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./modules/home/pages/home/home').then(m => m.HomeComponent),
        title: 'Trang chủ - Shoppee'
      },
      {
        path: 'cart',
        loadComponent: () =>
          import('./modules/cart/pages/cart/cart').then(m => m.CartPage),
        title: 'Giỏ hàng - Shoppee'
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./modules/auth/pages/profile/profile').then(m => m.ProfilePage),
        title: 'Hồ sơ - Shoppee'
      },
      {
        path: 'product',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./modules/product/pages/product-list/product-list').then(m => m.ProductList),
            title: 'Sản phẩm - Shoppee'
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./modules/product/pages/product-detail/product-detail').then(m => m.ProductDetail),
            title: 'Chi tiết sản phẩm - Shoppee'
          }
        ]
      },
      {
        path: 'order',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./modules/order/pages/order-list/order-list').then(m => m.OrderListPage),
            title: 'Đơn hàng - Shoppee'
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./modules/order/pages/order-detail/order-detail').then(m => m.OrderDetailPage),
            title: 'Chi tiết đơn hàng - Shoppee'
          }
        ]
      }
      ,
      {
        path: 'payment',
        children: [
          {
            path: 'result',
            loadComponent: () => import('./modules/payment/pages/payment-result/payment-result').then(m => m.PaymentResultPage),
            title: 'Kết quả thanh toán - Shoppee'
          }
        ]
      }
    ]
  },
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./modules/auth/pages/login/login').then(m => m.Login),
        title: 'Đăng nhập - Shoppee'
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./modules/auth/pages/register/register').then(m => m.RegisterPage),
        title: 'Đăng ký - Shoppee'
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [() => import('./guards/admin-guard').then(m => m.adminGuard)],
    loadChildren: () => import('./modules/admin/admin-routing.module').then(m => m.routes),
    title: 'Quản trị - Shoppee'
  }
];
