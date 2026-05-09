import { Component, OnInit, input, output, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GroupService } from '../../../core/services/group.service';
import { StudentGroup } from '../../../models/group.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-group-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './group-modal.component.html',
  styleUrl: './group-modal.component.css',
})
export class GroupModalComponent implements OnInit {
  group = input<StudentGroup | null>(null);
  mode = input<'create' | 'edit'>('create');

  saved = output<void>();
  cancelled = output<void>();

  form!: FormGroup;

  private fb = inject(FormBuilder);
  private groupService = inject(GroupService);
  private toastService = inject(ToastService);

  get isEdit() {
    return this.mode() === 'edit';
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  ngOnInit() {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(20)]],
    });

    const g = this.group();
    if (g) {
      this.form.patchValue({ name: g.name });
    }
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.form.value;

    if (this.isEdit) {
      this.groupService.update(this.group()!.id, request).subscribe(() => {
        this.toastService.success('Групу оновлено');
        this.saved.emit();
      });
    } else {
      this.groupService.create(request).subscribe(() => {
        this.toastService.success('Групу створено');
        this.saved.emit();
      });
    }
  }
}
