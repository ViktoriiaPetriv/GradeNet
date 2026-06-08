import { Component, OnInit, input, output, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommissionService } from '../../../core/services/commission.service';
import { Commission } from '../../../models/commission.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-commission-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './commission-modal.component.html',
  styleUrl: './commission-modal.component.css',
})
export class CommissionModalComponent implements OnInit {
  commission = input<Commission | null>(null);
  mode = input<'create' | 'edit'>('create');

  saved = output<void>();
  cancelled = output<void>();

  form!: FormGroup;

  private fb = inject(FormBuilder);
  private commissionService = inject(CommissionService);
  private toastService = inject(ToastService);

  get isEdit() {
    return this.mode() === 'edit';
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  ngOnInit() {
    const c = this.commission();
    this.form = this.fb.group({
      startDate: [c?.startDate ?? '', Validators.required],
      endDate: [c?.endDate ?? ''],
    });
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.value;
    const request = { ...raw, endDate: raw.endDate || null };

    if (this.isEdit) {
      this.commissionService.update(this.commission()!.id, request).subscribe(() => {
        this.toastService.success('Комісію оновлено');
        this.saved.emit();
      });
    } else {
      this.commissionService.create(request).subscribe(() => {
        this.toastService.success('Комісію створено');
        this.saved.emit();
      });
    }
  }
}
