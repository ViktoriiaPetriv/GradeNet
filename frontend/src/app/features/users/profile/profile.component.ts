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
import { forkJoin, of } from 'rxjs';

interface BookWithDetails {
  info: StudentInfo;
  specialty: Specialty | null;
  orgInfo: OrgInfo | null;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, UserModalComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  profile = signal<UserProfile | null>(null);
  books = signal<BookWithDetails[]>([]);
  editModalOpen = signal(false);
  editingUser = signal<User | null>(null);
  deleteModalOpen = signal(false);

  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private tokenService = inject(TokenService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id === 'me') {
        this.userService.getMyProfile().subscribe((p) => this.handleProfile(p));
      } else if (id) {
        this.loadProfile(+id);
      }
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
        book.specialtyId
          ? forkJoin({
              specialty: this.specialtyService.getById(book.specialtyId),
              orgInfo: this.specialtyService.getOrgInfo(book.specialtyId),
            })
          : of({ specialty: null, orgInfo: null }),
      ),
    ).subscribe((results) => {
      const booksWithDetails: BookWithDetails[] = p.books.map((book, i) => ({
        info: book,
        specialty: results[i].specialty,
        orgInfo: results[i].orgInfo,
      }));
      this.books.set(booksWithDetails);
    });
  }

  goBack() {
    this.router.navigate(['/users']);
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
    return this.currentUser?.role === 'ADMIN';
  }

  get isOwner(): boolean {
    return this.currentUser?.id === this.profile()?.id;
  }

  get canEdit(): boolean {
    return this.isAdmin || this.isOwner;
  }

  get canDelete(): boolean {
    return this.isAdmin;
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
