import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisciplineService } from '../../../core/services/discipline.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { HoursDTO } from '../../../models/discipline.model';

@Component({
  selector: 'app-add-hours-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './add-hours-modal.component.html',
  styleUrl: './add-hours-modal.component.css',
})
export class AddHoursModalComponent implements OnInit {
  @Input() specialtyDisciplineId!: number;
  @Input() specialtyName: string = '';
  @Input() hoursToEdit: HoursDTO | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  submitting = false;

  get isEditMode(): boolean {
    return this.hoursToEdit !== null;
  }

  private fb = inject(FormBuilder);
  private disciplineService = inject(DisciplineService);
  private toastService = inject(ToastService);

  form: FormGroup = this.fb.group({
    academicYear: ['', [Validators.required, Validators.pattern(/^\d{4}\/\d{4}$/)]],
    ectsCredits: [null, [Validators.required, Validators.min(1)]],
    totalHours: [null, [Validators.required, Validators.min(1)]],
    classroomHours: [null, [Validators.required, Validators.min(0)]],
    lectureHours: [0, Validators.min(0)],
    seminarHours: [0, Validators.min(0)],
    laboratoryHours: [0, Validators.min(0)],
    individualHours: [0, Validators.min(0)],
    selfWorkHours: [0, Validators.min(0)],
  });

  ngOnInit() {
    if (this.hoursToEdit) {
      const h = this.hoursToEdit;
      this.form.patchValue({
        academicYear: h.academicYear,
        ectsCredits: h.ectsCredits,
        totalHours: h.totalHours,
        classroomHours: h.classroomHours,
        lectureHours: h.lectureHours ?? 0,
        seminarHours: h.seminarHours ?? 0,
        laboratoryHours: h.laboratoryHours ?? 0,
        individualHours: h.individualHours ?? 0,
        selfWorkHours: h.selfWorkHours ?? 0,
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
    const request = {
      academicYear: v.academicYear,
      ectsCredits: +v.ectsCredits,
      totalHours: +v.totalHours,
      classroomHours: +v.classroomHours,
      lectureHours: +(v.lectureHours ?? 0),
      seminarHours: +(v.seminarHours ?? 0),
      laboratoryHours: +(v.laboratoryHours ?? 0),
      individualHours: +(v.individualHours ?? 0),
      selfWorkHours: +(v.selfWorkHours ?? 0),
    };

    const call$ = this.isEditMode
      ? this.disciplineService.updateHours(this.hoursToEdit!.id, request)
      : this.disciplineService.addHours(this.specialtyDisciplineId, request);

    call$.subscribe({
      next: () => {
        this.toastService.success(this.isEditMode ? 'Години оновлено' : 'Години додано');
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
