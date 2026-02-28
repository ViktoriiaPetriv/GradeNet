import { Component, OnInit, Input, Output, EventEmitter, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { OrgService } from '../../../core/services/org.service';
import { Specialty, Degree, EduType, OrganizationShort, OrgType } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-specialty-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './specialty-form.component.html',
  styleUrl: './specialty-form.component.css',
})
export class SpecialtyFormComponent implements OnInit {
  @Input() specialty: Specialty | null = null;

  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  degrees = Object.values(Degree);
  eduTypes = Object.values(EduType);
  departments: OrganizationShort[] = [];
  submitting = false;

  private toastService = inject(ToastService);

  get isEdit(): boolean {
    return !!this.specialty;
  }

  constructor(
    private fb: FormBuilder,
    private specialtyService: SpecialtyService,
    private orgService: OrgService,
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(10)]],
      nameUA: ['', Validators.required],
      nameEN: ['', Validators.required],
      studyProgramUA: ['', Validators.required],
      studyProgramEN: ['', Validators.required],
      eduProgramUA: ['', Validators.required],
      eduProgramEN: ['', Validators.required],
      orgId: ['', Validators.required],
      degree: ['', Validators.required],
      eduType: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: [''],
    });

    if (this.specialty) {
      this.form.patchValue({
        ...this.specialty,
        startDate: this.toDateInput(this.specialty.startDate),
        endDate: this.specialty.endDate ? this.toDateInput(this.specialty.endDate) : '',
      });
    }

    this.orgService.getAllShort(OrgType.DEPARTMENT).subscribe((d) => {
      this.departments = d;
      if (this.specialty) {
        this.form.patchValue({ orgId: this.specialty.orgId });
      }
    });
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const val = this.form.value;
    const request = {
      ...val,
      orgId: +val.orgId,
      startDate: new Date(val.startDate).toISOString(),
      endDate: val.endDate ? new Date(val.endDate).toISOString() : null,
    };

    this.submitting = true;
    const obs = this.isEdit
      ? this.specialtyService.update(this.specialty!.id, request)
      : this.specialtyService.create(request);

    obs.subscribe({
      next: () => {
        this.toastService.success(
          this.isEdit ? 'Спеціальність оновлено' : 'Спеціальність створено',
        );
        this.submitting = false;
        this.saved.emit();
      },
      error: () => {
        this.submitting = false;
      },
    });
  }

  cancel() {
    this.cancelled.emit();
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  degreeLabel(d: string): string {
    const map: Record<string, string> = {
      BACHELOR: 'Бакалавр',
      MASTER: 'Магістр',
      DOCTOR: 'Доктор',
      SPECIALIST: 'Спеціаліст',
    };
    return map[d] || d;
  }

  eduTypeLabel(e: string): string {
    const map: Record<string, string> = {
      FULL_TIME: 'Денна',
      PART_TIME: 'Вечірня',
      CORRESPONDENCE: 'Заочна',
      DISTANCE: 'Дистанційна',
      BLENDED: 'Змішана',
      EXTERN: 'Екстернат',
    };
    return map[e] || e;
  }

  private toDateInput(iso: string): string {
    return iso ? iso.split('T')[0] : '';
  }
}
