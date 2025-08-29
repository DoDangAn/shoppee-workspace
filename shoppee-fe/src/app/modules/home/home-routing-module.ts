import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home';   // ✅ import đúng tên

const routes: Routes = [
  { path: '', component: HomeComponent }  // ✅ dùng đúng class
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HomeRoutingModule {}
