import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { ChangePasswordRequest } from '../../../models/user.model';

function strongPasswordValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;
  const valid = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/.test(value);
  return valid ? null : { weakPassword: true };
}

@Component({
  selector: 'app-change-password-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html',
})
export class ChangePasswordModalComponent {
  userId = input.required<number>();
  saved = output<void>();
  cancelled = output<void>();

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private toastService = inject(ToastService);

  showNewPassword = signal(false);

  form = this.fb.group({
    newPassword: ['', [Validators.required, strongPasswordValidator]],
  });

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.userService
      .changePassword(this.userId(), this.form.value as ChangePasswordRequest)
      .subscribe(() => {
        this.toastService.success('Пароль змінено');
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
}
