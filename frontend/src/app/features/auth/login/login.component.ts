import { Component, signal, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  form: FormGroup;
  loading = signal(false);
  error = signal('');
  showPassword = signal(false);

  private toastService = inject(ToastService);
  private authState = inject(AuthStateService);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  togglePassword() {
    this.showPassword.update((v) => !v);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.toastService.success('Ласкаво просимо!');

        if (this.authState.isAdminOrManager()) {
          this.router.navigate(['/users']);
        } else {
          this.router.navigate(['/profile', this.authState.currentUserId()]);
        }
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 400) {
          this.error.set('Невірний email або пароль');
        } else {
          this.error.set('Помилка сервера. Спробуйте пізніше.');
        }
      },
    });
  }
}
