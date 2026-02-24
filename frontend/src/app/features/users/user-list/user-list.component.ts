import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../models/user.model';
import { Role } from '../../../models/role.enum';
import { ToastService } from '../../../core/services/toast.service';
import { Router } from '@angular/router';
import { UserModalComponent } from '../user-modal/user-modal.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, UserModalComponent],
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
  pageSizeOptions = [2, 10, 20, 50, 100];
  roles = Object.values(Role);
  editingUser = signal<User | null>(null);
  modalOpen = signal(false);
  isEdit = signal(false);
  deleteModalOpen = signal(false);
  userToDelete = signal<User | null>(null);

  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private router = inject(Router);

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
    this.userService.delete(user.id).subscribe(() => {
      this.toastService.success('Користувача видалено');
      this.loadUsers();
      this.closeDeleteModal();
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

  setPageSize(size: number) {
    this.perPage.set(size);
    this.currentPage.set(1);
  }

  setPage(page: number) {
    this.currentPage.set(page);
  }

  get paginated(): User[] {
    const start = (this.currentPage() - 1) * this.perPage();
    return this.filtered().slice(start, start + this.perPage());
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filtered().length / this.perPage()));
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get paginationInfo(): string {
    const total = this.filtered().length;
    if (total === 0) return 'Показано 0 з 0';

    const start = (this.currentPage() - 1) * this.perPage() + 1;
    const end = Math.min(this.currentPage() * this.perPage(), total);

    return `Показано ${start}–${end} з ${total}`;
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

  onPageSizeChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.setPageSize(+value);
  }
}
