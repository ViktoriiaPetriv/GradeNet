import { Component, inject, input, OnChanges, output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { UserProfile } from '../../../models/user.model';

function nameValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;
  return /^[\p{L}'\- ]+$/u.test(value) ? null : { invalidName: true };
}

@Component({
  selector: 'app-edit-profile-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-profile-modal.component.html',
})
export class EditProfileModalComponent implements OnChanges {
  profile = input.required<UserProfile>();
  saved = output<void>();
  cancelled = output<void>();

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private toastService = inject(ToastService);

  form = this.fb.group({
    firstName: ['', [Validators.maxLength(50), nameValidator]],
    lastName: ['', [Validators.maxLength(50), nameValidator]],
    patronymic: ['', [Validators.maxLength(50), nameValidator]],
    birthDate: [''],
  });

  ngOnChanges(changes: SimpleChanges) {
    if (changes['profile']) {
      const p = this.profile();
      this.form.reset({
        firstName: p.firstName ?? '',
        lastName: p.lastName ?? '',
        patronymic: p.patronymic ?? '',
        birthDate: p.birthDate ? p.birthDate.split('T')[0] : '',
      });
    }
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const val = this.form.value;
    const request: Record<string, string> = {};
    if (val.firstName !== null && val.firstName !== undefined) request['firstName'] = val.firstName;
    if (val.lastName !== null && val.lastName !== undefined) request['lastName'] = val.lastName;
    if (val.patronymic !== null && val.patronymic !== undefined) request['patronymic'] = val.patronymic;
    if (val.birthDate) request['birthDate'] = val.birthDate;

    this.userService.updateSelf(this.profile().id, request).subscribe(() => {
      this.toastService.success('Профіль оновлено');
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

  isNameInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.errors?.['invalidName'] && c?.dirty);
  }
}
