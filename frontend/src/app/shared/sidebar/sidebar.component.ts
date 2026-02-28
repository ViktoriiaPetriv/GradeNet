import { Component, signal, inject } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  isOpen = signal(false);
  private authService = inject(AuthService);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  currentUser = this.tokenService.currentUser;

  toggle() {
    this.isOpen.update((v) => !v);
  }
  close() {
    this.isOpen.set(false);
  }

  logout() {
    this.authService.logout();
  }

  goToProfile() {
    this.router.navigate(['/profile', 'me']);
    this.close();
  }

  getInitials(u: User | null): string {
    if (!u) return '';
    return (u.lastName?.[0] || '') + (u.firstName?.[0] || '');
  }

  roleLabel(role: string | undefined): string {
    const map: Record<string, string> = {
      ADMIN: 'Admin',
      MANAGER: 'Manager',
      PROFESSOR: 'Professor',
      STUDENT: 'Student',
    };
    return role ? map[role] || role : '';
  }
}
