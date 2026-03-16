import { Injectable, inject, computed } from '@angular/core';
import { TokenService } from './token.service';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private tokenService = inject(TokenService);

  currentUser = computed(() => this.tokenService.currentUser());
  role = computed(() => this.currentUser()?.role);

  isAdmin = computed(() => this.role() === 'ADMIN');
  isManager = computed(() => this.role() === 'MANAGER');
  isProfessor = computed(() => this.role() === 'PROFESSOR');
  isStudent = computed(() => this.role() === 'STUDENT');

  isAdminOrManager = computed(() => this.isAdmin() || this.isManager());
  currentUserId = computed(() => this.currentUser()?.id);

  managerFacultyId = computed(() => this.currentUser()?.orgId ?? null);
}
