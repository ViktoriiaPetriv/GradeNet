import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserProfile, StudentInfo, User } from '../../../models/user.model';
import { Specialty } from '../../../models/org.model';
import { OrgInfo } from '../../../models/org.model';
import { TokenService } from '../../../core/services/token.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ActivatedRoute, Router } from '@angular/router';
import { UserModalComponent } from '../user-modal/user-modal.component';
import { UserService } from '../../../core/services/user.service';
import { forkJoin, of, catchError, switchMap } from 'rxjs';
import { ChangePasswordModalComponent } from '../change-password/change-password.component';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { BadgeComponent } from '../../../shared/badge/badge.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

interface BookWithDetails {
  info: StudentInfo;
  specialty: Specialty | null;
  orgInfo: OrgInfo | null;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    UserModalComponent,
    ChangePasswordModalComponent,
    AvatarComponent,
    BadgeComponent,
    ModalComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  profile = signal<UserProfile | null>(null);
  books = signal<BookWithDetails[]>([]);
  editModalOpen = signal(false);
  editingUser = signal<User | null>(null);
  deleteModalOpen = signal(false);
  changePasswordModalOpen = signal(false);

  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private tokenService = inject(TokenService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authState = inject(AuthStateService);

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) this.loadProfile(+id);
    });
  }

  private loadProfile(id: number) {
    this.profile.set(null);
    this.books.set([]);
    this.userService.getProfile(id).subscribe((p) => this.handleProfile(p));
  }

  private handleProfile(p: UserProfile) {
    this.profile.set(p);

    if (!p.books?.length) {
      this.books.set([]);
      return;
    }

    forkJoin(
      p.books.map((book) =>
        book.specialtyOfferingId
          ? this.specialtyService.getOfferingById(book.specialtyOfferingId).pipe(
              catchError(() => of(null)),
              switchMap((offering) =>
                offering
                  ? forkJoin({
                      specialty: this.specialtyService.getById(offering.specialtyId).pipe(catchError(() => of(null))),
                      orgInfo: this.specialtyService.getOrgInfo(offering.specialtyId).pipe(catchError(() => of(null))),
                    })
                  : of({ specialty: null, orgInfo: null }),
              ),
            )
          : of({ specialty: null, orgInfo: null }),
      ),
    ).subscribe({
      next: (results) => {
        this.books.set(
          p.books.map((book, i) => ({
            info: book,
            specialty: results[i].specialty,
            orgInfo: results[i].orgInfo,
          })),
        );
      },
      error: (err) => console.error('books load error', err),
    });
  }

  goBack() {
    this.router.navigate(['/users']);
  }

  get canGoBack(): boolean {
    return !this.isOwner || this.isAdmin || this.isManager;
  }

  getInitials(): string {
    const p = this.profile();
    return (p?.lastName?.[0] || '') + (p?.firstName?.[0] || '');
  }

  getAvatarColor(): string {
    const id = this.profile()?.id ?? 0;
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  roleLabel(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'Адміністратор',
      MANAGER: 'Менеджер',
      PROFESSOR: 'Викладач',
      STUDENT: 'Студент',
    };
    return map[role] ?? role;
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    const [y, m, day] = d.split('T')[0].split('-');
    return `${day}.${m}.${y}`;
  }

  bookStatusLabel(status: string | null): string {
    if (!status) return '—';
    const map: Record<string, string> = {
      REGISTERED: 'Зареєстрована',
      FILLED: 'Заповнена',
      HANDED: 'Здана',
    };
    return map[status] ?? status;
  }

  get currentUser() {
    return this.tokenService.currentUser();
  }

  get isAdmin(): boolean {
    return this.authState.isAdmin();
  }

  get isManager(): boolean {
    return this.authState.isManager();
  }

  get isOwner(): boolean {
    return this.authState.currentUserId() === this.profile()?.id;
  }

  get canEditFull(): boolean {
    return this.isAdmin || (this.isManager && this.profile()?.role === 'STUDENT');
  }

  get canChangePassword(): boolean {
    return this.isOwner && !this.isAdmin;
  }

  get canDelete(): boolean {
    return this.isAdmin || (this.isManager && this.profile()?.role === 'STUDENT');
  }

  changePassword() {
    this.changePasswordModalOpen.set(true);
  }

  editProfile() {
    const p = this.profile()!;
    this.editingUser.set(p as unknown as User);
    this.editModalOpen.set(true);
  }

  onSaved() {
    this.editModalOpen.set(false);
    this.loadProfile(this.profile()!.id);
  }

  deleteProfile() {
    this.deleteModalOpen.set(true);
  }

  confirmDelete() {
    const id = this.profile()?.id;
    if (!id) return;
    this.userService.delete(id).subscribe(() => {
      this.deleteModalOpen.set(false);
      this.router.navigate(['/users']);
    });
  }
}
