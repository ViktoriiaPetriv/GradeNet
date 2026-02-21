import { Component, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../models/user.model';
import { Role } from '../../../models/role.enum';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.css',
})
export class UserListComponent implements OnInit {
  users = signal<User[]>([]);
  filtered = signal<User[]>([]);
  search = signal('');
  roleFilter = signal('');
  currentPage = signal(1);
  perPage = 6;
  roles = Object.values(Role);

  // Modal
  modalOpen = signal(false);
  isEdit = signal(false);
  editingId = signal<number | null>(null);
  form!: FormGroup;

  constructor(
    private userService: UserService,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      patronymic: ['', Validators.maxLength(100)],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      birthDate: ['', Validators.required],
      role: ['', Validators.required],
    });

    this.userService.findAll().subscribe((users) => {
      this.users.set(users);
      this.applyFilter();
    });
  }

  applyFilter() {
    const s = this.search().toLowerCase();
    const r = this.roleFilter();
    const result = this.users().filter((u) => {
      const name = `${u.lastName} ${u.firstName} ${u.email}`.toLowerCase();
      return (!s || name.includes(s)) && (!r || u.role === r);
    });
    this.filtered.set(result);
    this.currentPage.set(1);
  }

  get paginated(): User[] {
    const start = (this.currentPage() - 1) * this.perPage;
    return this.filtered().slice(start, start + this.perPage);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filtered().length / this.perPage));
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get paginationInfo(): string {
    const start = (this.currentPage() - 1) * this.perPage + 1;
    const end = Math.min(this.currentPage() * this.perPage, this.filtered().length);
    return `Показано ${Math.min(start, this.filtered().length)}–${end} з ${this.filtered().length}`;
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
    this.applyFilter();
  }

  onRoleFilter(e: Event) {
    this.roleFilter.set((e.target as HTMLSelectElement).value);
    this.applyFilter();
  }

  setPage(p: number) {
    this.currentPage.set(p);
  }

  openCreate() {
    this.isEdit.set(false);
    this.editingId.set(null);
    this.form.reset();
    this.form.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.form.get('password')?.updateValueAndValidity();
    this.modalOpen.set(true);
  }

  openEdit(user: User) {
    this.isEdit.set(true);
    this.editingId.set(user.id);
    this.form.patchValue(user);
    this.form.get('password')?.clearValidators();
    this.form.get('password')?.updateValueAndValidity();
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
    this.form.reset();
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request = this.form.value;
    if (this.isEdit() && this.editingId()) {
      this.userService.update(this.editingId()!, request).subscribe((updated) => {
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.applyFilter();
        this.closeModal();
      });
    } else {
      this.userService.create(request).subscribe((created) => {
        this.users.update((list) => [...list, created]);
        this.applyFilter();
        this.closeModal();
      });
    }
  }

  delete(id: number) {
    if (!confirm('Видалити користувача?')) return;
    this.userService.delete(id).subscribe(() => {
      this.users.update((list) => list.filter((u) => u.id !== id));
      this.applyFilter();
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  getInitials(u: User): string {
    return (u.lastName[0] || '') + (u.firstName[0] || '');
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  roleLabel(role: string): string {
    const map: Record<string, string> = {
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
