import { Component, signal, inject, OnInit } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { UserService } from '../../core/services/user.service';
import { User } from '../../models/user.model';
import { AuthStateService } from '../../core/services/auth-state.service';
import {AvatarComponent} from '../avatar/avatar.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule, AvatarComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent implements OnInit {
  isOpen = signal(false);
  studentBookId = signal<number | null>(null);

  private authService = inject(AuthService);
  private tokenService = inject(TokenService);
  private userService = inject(UserService);
  private router = inject(Router);

  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;
  isProfessor = this.authState.isProfessor;
  isStudent = this.authState.isStudent;

  currentUser = this.tokenService.currentUser;

  ngOnInit() {
    if (this.authState.isStudent()) {
      this.userService.getMyProfile().subscribe({
        next: (p) => {
          const book = p.books?.[0];
          if (book) this.studentBookId.set(book.bookId);
        },
        error: () => {},
      });
    }
  }

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
    const id = this.tokenService.currentUser()?.id;
    if (id) this.router.navigate(['/profile', id]);
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
