import { Component, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { SidebarComponent } from './shared/sidebar/sidebar.component';
import { CommonModule } from '@angular/common';
import { filter, map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, CommonModule, ToastModule],
  template: `
    @if (showSidebar()) {
      <app-sidebar />
    }
    <router-outlet />
    <p-toast position="top-right" />
  `,
  styleUrl: './app.css',
})
export class App {
  private router = inject(Router);

  showSidebar = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map((e) => (e as NavigationEnd).urlAfterRedirects !== '/login'),
    ),
    { initialValue: false },
  );
}
