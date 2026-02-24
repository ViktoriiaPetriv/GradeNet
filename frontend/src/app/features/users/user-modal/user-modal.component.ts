import { Component, input, output, signal, inject, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
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
  selector: 'app-user-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-modal.component.html',
  styleUrl: './user-modal.component.css',
})
export class UserModalComponent implements OnChanges {
  // Inputs
  user = input<User | null>(null);
  mode = input<'create' | 'edit'>('create');

  // Outputs
  saved = output<void>();
  cancelled = output<void>();

  roles = Object.values(Role);
  faculties = signal<OrganizationShort[]>([]);
  showPassword = signal(false);
  step = signal(1);
  form!: FormGroup;

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private orgService = inject(OrgService);
  private toastService = inject(ToastService);

  constructor() {
    this.initForm();
    this.orgService.getAllShort('FACULTY').subscribe((f) => this.faculties.set(f));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['user'] || changes['mode']) {
      this.step.set(1);
      this.showPassword.set(false);
      const u = this.user();

      if (this.mode() === 'edit' && u) {
        this.form.reset();
        this.form.patchValue(u);
        this.form.get('password')?.setValue('');
        this.form.get('password')?.clearValidators();
        this.form.get('password')?.setValidators([Validators.minLength(8)]);
        this.form.get('password')?.updateValueAndValidity();
      } else {
        this.form.reset({ role: '' });
        this.form.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
        this.form.get('password')?.updateValueAndValidity();
      }
    }
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

  get isEdit(): boolean {
    return this.mode() === 'edit';
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
    if (!this.isEdit) fields.push('password');
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

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = { ...this.form.value };
    if (!request.password) delete request.password;

    const action$ = this.isEdit
      ? this.userService.update(this.user()!.id, request)
      : this.userService.create(request);

    action$.subscribe(() => {
      this.toastService.success(this.isEdit ? 'Користувача оновлено' : 'Користувача створено');
      this.saved.emit();
    });
  }

  cancel() {
    this.cancelled.emit();
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

  togglePassword() {
    this.showPassword.update((v) => !v);
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
}
