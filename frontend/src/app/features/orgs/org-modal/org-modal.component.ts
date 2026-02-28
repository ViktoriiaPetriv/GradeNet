import { Component, OnInit, input, output, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { OrgService } from '../../../core/services/org.service';
import { Organization, OrgType } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-org-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './org-modal.component.html',
  styleUrl: './org-modal.component.css',
})
export class OrgModalComponent implements OnInit {
  org = input<Organization | null>(null);
  mode = input<'create' | 'edit'>('create');

  saved = output<void>();
  cancelled = output<void>();

  form!: FormGroup;
  orgTypes = Object.values(OrgType);
  faculties: Organization[] = [];

  private fb = inject(FormBuilder);
  private orgService = inject(OrgService);
  private toastService = inject(ToastService);

  get isEdit() {
    return this.mode() === 'edit';
  }
  get isDepartment() {
    return this.form?.get('orgType')?.value === OrgType.DEPARTMENT;
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  ngOnInit() {
    this.form = this.fb.group({
      name: ['', Validators.required],
      orgType: ['', Validators.required],
      parentId: [null],
    });

    const o = this.org();

    this.orgService.getAllShort(OrgType.FACULTY).subscribe((list) => {
      this.faculties = list as unknown as Organization[];

      if (o) {
        this.form.patchValue({
          name: o.name,
          orgType: o.orgType,
          parentId: o.parentId ?? null,
        });
      }
    });

    this.form.get('orgType')?.valueChanges.subscribe((type) => {
      const parentControl = this.form.get('parentId');
      if (type === OrgType.DEPARTMENT) {
        parentControl?.setValidators(Validators.required);
      } else {
        parentControl?.clearValidators();
        parentControl?.setValue(null);
      }
      parentControl?.updateValueAndValidity();
    });
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.value;
    const request = { ...val, parentId: val.parentId || null };

    if (this.isEdit) {
      this.orgService.update(this.org()!.id, request).subscribe(() => {
        this.toastService.success('Організацію оновлено');
        this.saved.emit();
      });
    } else {
      this.orgService.create(request).subscribe(() => {
        this.toastService.success('Організацію створено');
        this.saved.emit();
      });
    }
  }

  orgTypeLabel(t: string): string {
    return t === 'FACULTY' ? 'Факультет' : 'Кафедра';
  }
}
