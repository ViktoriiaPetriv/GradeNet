import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisciplineService } from '../../../core/services/discipline.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/modal/modal.component';

@Component({
  selector: 'app-add-hours-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './add-hours-modal.component.html',
  styleUrl: './add-hours-modal.component.css',
})
export class AddHoursModalComponent {
  @Input() specialtyDisciplineId!: number;
  @Input() specialtyName: string = '';
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  submitting = false;

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

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const v = this.form.value;

    this.disciplineService
      .addHours(this.specialtyDisciplineId, {
        academicYear: v.academicYear,
        ectsCredits: +v.ectsCredits,
        totalHours: +v.totalHours,
        classroomHours: +v.classroomHours,
        lectureHours: +(v.lectureHours ?? 0),
        seminarHours: +(v.seminarHours ?? 0),
        laboratoryHours: +(v.laboratoryHours ?? 0),
        individualHours: +(v.individualHours ?? 0),
        selfWorkHours: +(v.selfWorkHours ?? 0),
      })
      .subscribe({
        next: () => {
          this.toastService.success('Години додано');
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
