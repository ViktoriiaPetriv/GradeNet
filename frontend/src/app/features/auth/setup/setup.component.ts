import { Component, signal, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthApiService } from '../../../core/services/auth-api.service';
import { SetupService } from '../../../core/services/setup.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-setup',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './setup.component.html',
  styleUrl: './setup.component.css',
})
export class SetupComponent {
  form: FormGroup;
  loading = signal(false);
  error = signal('');
  showPassword = signal(false);
  showConfirm = signal(false);

  private authApi = inject(AuthApiService);
  private setupService = inject(SetupService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group(
      {
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator },
    );
  }

  private passwordMatchValidator(group: FormGroup) {
    const pw = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return pw === confirm ? null : { mismatch: true };
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  isConfirmMismatch(): boolean {
    return !!(
      this.form.hasError('mismatch') && this.form.get('confirmPassword')?.touched
    );
  }

  togglePassword() {
    this.showPassword.update((v) => !v);
  }

  toggleConfirm() {
    this.showConfirm.update((v) => !v);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const { email, password } = this.form.value;

    this.authApi.setupAdmin({ email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.setupService.clearCache();
        this.toastService.success('Адміністратора успішно зареєстровано!');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 400 || err.status === 409) {
          this.error.set(err.error?.message ?? 'Помилка реєстрації. Перевірте дані.');
        } else {
          this.error.set('Помилка сервера. Спробуйте пізніше.');
        }
      },
    });
  }
}
