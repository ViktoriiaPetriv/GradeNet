import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../models/user.model';
import { Role } from '../../../models/role.enum';
import { ToastService } from '../../../core/services/toast.service';
import { Router } from '@angular/router';
import { UserModalComponent } from '../user-modal/user-modal.component';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { BadgeComponent } from '../../../shared/badge/badge.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    UserModalComponent,
    PaginationComponent,
    PageHeaderComponent,
    AvatarComponent,
    BadgeComponent,
    ModalComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.css',
})
export class UserListComponent implements OnInit {
  users = signal<User[]>([]);
  filtered = signal<User[]>([]);
  search = signal('');
  roleFilter = signal('');
  currentPage = signal(1);
  perPage = signal(10);
  perPageOptions = [2, 5, 10, 25, 50];
  roles = Object.values(Role);
  editingUser = signal<User | null>(null);
  modalOpen = signal(false);
  isEdit = signal(false);
  deleteModalOpen = signal(false);
  userToDelete = signal<User | null>(null);

  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private authState = inject(AuthStateService);

  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;

  totalPages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.perPage())));

  paginated = computed(() => {
    const start = (this.currentPage() - 1) * this.perPage();
    return this.filtered().slice(start, start + this.perPage());
  });

  paginationInfo = computed(() => {
    const total = this.filtered().length;
    if (total === 0) return 'Немає записів';
    const start = (this.currentPage() - 1) * this.perPage() + 1;
    const end = Math.min(this.currentPage() * this.perPage(), total);
    return `Показано ${start}–${end} з ${total}`;
  });

  onPageChange(page: number) {
    this.currentPage.set(page);
  }

  onPerPageChange(size: number) {
    this.perPage.set(size);
    this.currentPage.set(1);
  }

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.userService.findAll().subscribe((users) => {
      this.users.set(users);
      this.applyFilter();
    });
  }

  openCreate() {
    this.editingUser.set(null);
    this.isEdit.set(false);
    this.modalOpen.set(true);
  }

  openEdit(user: User) {
    this.editingUser.set(user);
    this.isEdit.set(true);
    this.modalOpen.set(true);
  }

  onSaved() {
    this.loadUsers();
    this.closeModal();
  }

  closeModal() {
    this.modalOpen.set(false);
  }

  openDeleteModal(user: User) {
    this.userToDelete.set(user);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.userToDelete.set(null);
  }

  confirmDelete() {
    const user = this.userToDelete();
    if (!user) return;
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.toastService.success('Користувача видалено');
        this.loadUsers();
        this.closeDeleteModal();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Помилка при видаленні користувача';
        this.toastService.error(msg);
        this.closeDeleteModal();
      },
    });
  }

  viewProfile(id: number) {
    this.router.navigate(['/profile', id]);
  }

  applyFilter() {
    const s = this.search().toLowerCase();
    const r = this.roleFilter();
    this.filtered.set(
      this.users().filter((u) => {
        const name = `${u.lastName} ${u.firstName} ${u.email}`.toLowerCase();
        return (!s || name.includes(s)) && (!r || u.role === r);
      }),
    );
    this.currentPage.set(1);
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
    this.applyFilter();
  }

  onRoleFilter(e: Event) {
    this.roleFilter.set((e.target as HTMLSelectElement).value);
    this.applyFilter();
  }

  getInitials(u: User): string {
    return (u.lastName?.[0] || '') + (u.firstName?.[0] || '');
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  roleLabel(role: string): string {
    const map: any = {
      ADMIN: 'Admin',
      MANAGER: 'Manager',
      PROFESSOR: 'Professor',
      STUDENT: 'Student',
    };
    return map[role] || role;
  }

  formatDate(d: string): string {
    if (!d) return '—';
    const [y, m, day] = d.split('-');
    return `${day}.${m}.${y}`;
  }
}
