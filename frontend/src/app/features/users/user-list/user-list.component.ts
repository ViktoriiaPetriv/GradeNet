import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../models/user.model';
import { Role } from '../../../models/role.enum';
import { ToastService } from '../../../core/services/toast.service';
import { OrgService } from '../../../core/services/org.service';
import { OrganizationShort } from '../../../models/org.model';

function universityEmailValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;
  const valid = /^[a-zA-Z0-9._%+\-]+@(pnu|cnu)\.edu\.ua$/.test(value);
  return valid ? null : { universityEmail: true };
}

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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
  faculties = signal<OrganizationShort[]>([]);
  private orgService = inject(OrgService);
  showPassword = signal(false);

  modalOpen = signal(false);
  isEdit = signal(false);
  editingId = signal<number | null>(null);
  step = signal(1);
  form!: FormGroup;

  deleteModalOpen = signal(false);
  userToDelete = signal<User | null>(null);

  private toastService = inject(ToastService);

  constructor(
    private userService: UserService,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.initForm();
    this.loadUsers();
    this.orgService.getAllShort('FACULTY').subscribe((f) => this.faculties.set(f));
  }

  initForm() {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email, universityEmailValidator]],
      password: ['', [Validators.minLength(8)]],
      role: ['', Validators.required],
      firstName: ['', [Validators.maxLength(50)]],
      lastName: ['', [Validators.maxLength(50)]],
      patronymic: ['', Validators.maxLength(50)],
      birthDate: [''],
      orgId: [null],
    });
  }

  loadUsers() {
    this.userService.findAll().subscribe((users) => {
      this.users.set(users);
      this.applyFilter();
    });
  }

  get needsSecondStep(): boolean {
    const role = this.form.get('role')?.value;
    return role && role !== Role.ADMIN;
  }

  nextStep() {
    if (this.isStep1Invalid()) return;

    if (this.needsSecondStep) {
      this.setupStep2Validators();
      this.step.set(2);
    } else {
      this.submit();
    }
  }

  isStep1Invalid(): boolean {
    const fields = ['email', 'role'];
    if (!this.isEdit()) fields.push('password');

    let invalid = false;
    fields.forEach((f) => {
      const ctrl = this.form.get(f);
      ctrl?.markAsTouched();
      if (ctrl?.invalid) invalid = true;
    });
    return invalid;
  }

  setupStep2Validators() {
    const role = this.form.get('role')?.value;
    const fName = this.form.get('firstName');
    const lName = this.form.get('lastName');
    const bDate = this.form.get('birthDate');
    const oId = this.form.get('orgId');

    [fName, lName, bDate, oId].forEach((c) => c?.clearValidators());

    if (role === Role.PROFESSOR || role === Role.STUDENT) {
      fName?.setValidators([Validators.required, Validators.maxLength(50)]);
      lName?.setValidators([Validators.required, Validators.maxLength(50)]);
    }
    if (role === Role.STUDENT) bDate?.setValidators([Validators.required]);
    if (role === Role.MANAGER) oId?.setValidators([Validators.required]);

    [fName, lName, bDate, oId].forEach((c) => c?.updateValueAndValidity());
  }

  openCreate() {
    this.showPassword.set(false);
    this.isEdit.set(false);
    this.editingId.set(null);
    this.step.set(1);
    this.form.reset({ role: '' });
    this.form.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    this.modalOpen.set(true);
  }

  openEdit(user: User) {
    this.showPassword.set(false);
    this.isEdit.set(true);
    this.editingId.set(user.id);
    this.step.set(1);

    this.form.reset();
    this.form.patchValue(user);
    this.form.get('password')?.setValue('');

    this.form.get('password')?.clearValidators();
    this.form.get('password')?.setValidators([Validators.minLength(8)]);

    this.modalOpen.set(true);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.form.value;
    if (!request.password) delete request.password;

    const action$ = this.isEdit()
      ? this.userService.update(this.editingId()!, request)
      : this.userService.create(request);

    action$.subscribe((res) => {
      this.toastService.success(this.isEdit() ? 'Користувача оновлено' : 'Користувача створено');
      this.loadUsers();
      this.closeModal();
    });
  }

  closeModal() {
    this.modalOpen.set(false);
    this.step.set(1);
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
  delete(id: number) {
    if (!confirm('Видалити користувача?')) return;
    this.userService.delete(id).subscribe(() => {
      this.toastService.success('Користувача видалено');
      this.loadUsers();
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  isMaxLengthExceeded(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.errors?.['maxlength'] && c?.dirty);
  }

  isMinLengthNotMet(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.errors?.['minlength'] && c?.dirty);
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

  togglePassword() {
    this.showPassword.update((v) => !v);
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
}
