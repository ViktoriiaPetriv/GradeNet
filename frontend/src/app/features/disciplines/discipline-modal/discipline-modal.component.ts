import { Component, OnInit, Input, Output, EventEmitter, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisciplineService } from '../../../core/services/discipline.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { DisciplineDTO } from '../../../models/discipline.model';
import { Specialty, SpecialtyOffering } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/modal/modal.component';

@Component({
  selector: 'app-discipline-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './discipline-modal.component.html',
  styleUrl: './discipline-modal.component.css',
})
export class DisciplineModalComponent implements OnInit {
  @Input() discipline: DisciplineDTO | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  specialties: Specialty[] = [];
  offerings: SpecialtyOffering[] = [];
  submitting = false;

  private fb = inject(FormBuilder);
  private disciplineService = inject(DisciplineService);
  private specialtyService = inject(SpecialtyService);
  private toastService = inject(ToastService);

  get isEdit(): boolean {
    return !!this.discipline;
  }

  ngOnInit() {
    this.form = this.fb.group({
      name: [this.discipline?.name ?? '', [Validators.required, Validators.maxLength(255)]],
      specialtyId: ['', this.isEdit ? [] : [Validators.required]],
      specialtyOfferingId: ['', this.isEdit ? [] : [Validators.required]],
      academicYear: ['', this.isEdit ? [] : [Validators.required, Validators.pattern(/^\d{4}\/\d{4}$/)]],
      ectsCredits: [null, this.isEdit ? [] : [Validators.required, Validators.min(1)]],
      totalHours: [null, this.isEdit ? [] : [Validators.required, Validators.min(1)]],
      classroomHours: [null, this.isEdit ? [] : [Validators.required, Validators.min(0)]],
      lectureHours: [0, Validators.min(0)],
      seminarHours: [0, Validators.min(0)],
      laboratoryHours: [0, Validators.min(0)],
      individualHours: [0, Validators.min(0)],
      selfWorkHours: [0, Validators.min(0)],
    });

    if (!this.isEdit) {
      this.specialtyService.getAll({ size: 100 }).subscribe((page) => {
        this.specialties = page.content;
      });
    }
  }

  onSpecialtyChange() {
    const specialtyId = +this.form.get('specialtyId')!.value;
    this.form.patchValue({ specialtyOfferingId: '' });
    this.offerings = [];
    if (specialtyId) {
      this.specialtyService.getOfferings(specialtyId).subscribe((offs) => {
        this.offerings = offs;
      });
    }
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const v = this.form.value;

    const obs = this.isEdit
      ? this.disciplineService.update(this.discipline!.id, { name: v.name })
      : this.disciplineService.create({
          name: v.name,
          specialtyOfferingId: +v.specialtyOfferingId,
          hours: {
            academicYear: v.academicYear,
            ectsCredits: +v.ectsCredits,
            totalHours: +v.totalHours,
            classroomHours: +v.classroomHours,
            lectureHours: +(v.lectureHours ?? 0),
            seminarHours: +(v.seminarHours ?? 0),
            laboratoryHours: +(v.laboratoryHours ?? 0),
            individualHours: +(v.individualHours ?? 0),
            selfWorkHours: +(v.selfWorkHours ?? 0),
          },
        });

    obs.subscribe({
      next: () => {
        this.toastService.success(this.isEdit ? 'Дисципліну оновлено' : 'Дисципліну створено');
        this.submitting = false;
        this.saved.emit();
      },
      error: () => {
        this.submitting = false;
      },
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }
}
