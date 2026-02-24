import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserProfile, StudentInfo, User } from '../../../models/user.model';
import { Specialty, Organization } from '../../../models/org.model';
import { TokenService } from '../../../core/services/token.service';
import { OrgService } from '../../../core/services/org.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ActivatedRoute } from '@angular/router';
import { OrgInfo } from '../../../models/org.model';
import { Router } from '@angular/router';
import { UserModalComponent } from '../user-modal/user-modal.component';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, UserModalComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  profile = signal<UserProfile | null>(null);
  specialty = signal<Specialty | null>(null);
  org = signal<Organization | null>(null);
  orgInfo = signal<OrgInfo | null>(null);
  editModalOpen = signal(false);
  editingUser = signal<User | null>(null);
  deleteModalOpen = signal(false);

  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private orgService = inject(OrgService);
  private tokenService = inject(TokenService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');

      if (id === 'me') {
        this.userService.getMyProfile().subscribe((profile) => {
          this.profile.set(profile);

          if (profile.studentInfo?.specialtyId) {
            this.specialtyService
              .getById(profile.studentInfo.specialtyId)
              .subscribe((s) => this.specialty.set(s));

            this.specialtyService
              .getOrgInfo(profile.studentInfo.specialtyId)
              .subscribe((info) => this.orgInfo.set(info));
          }
        });
      } else if (id) {
        this.loadProfile(+id);
      }
    });
  }

  private loadProfile(id: number) {
    this.profile.set(null);
    this.specialty.set(null);
    this.orgInfo.set(null);

    this.userService.getProfile(id).subscribe((profile) => {
      this.profile.set(profile);

      if (profile.studentInfo?.specialtyId) {
        this.specialtyService
          .getById(profile.studentInfo.specialtyId)
          .subscribe((s) => this.specialty.set(s));
        this.specialtyService
          .getOrgInfo(profile.studentInfo.specialtyId)
          .subscribe((info) => this.orgInfo.set(info));
      }
    });
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
    return map[role] || role;
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    const [y, m, day] = d.split('T')[0].split('-');
    return `${day}.${m}.${y}`;
  }

  bookStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Активна',
      INACTIVE: 'Неактивна',
      HANDED: 'Здана',
    };
    return map[status] || status;
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
    // конвертуй UserProfile → User
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
