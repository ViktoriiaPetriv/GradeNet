import { Component, OnInit, Input, Output, EventEmitter, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { switchMap, forkJoin } from 'rxjs';
import { DisciplineService } from '../../../core/services/discipline.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Specialty, SpecialtyOffering } from '../../../models/org.model';
import { SpecialtyDisciplineDTO } from '../../../models/discipline.model';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/modal/modal.component';

@Component({
  selector: 'app-specialty-discipline-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './specialty-discipline-modal.component.html',
  styleUrl: './specialty-discipline-modal.component.css',
})
export class SpecialtyDisciplineModalComponent implements OnInit {
  @Input() disciplineId!: number;
  @Input() existingSpecialtyOfferingIds: number[] = [];
  @Output() saved = new EventEmitter<SpecialtyDisciplineDTO>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  specialties: Specialty[] = [];
  offerings: SpecialtyOffering[] = [];
  submitting = false;

  private fb = inject(FormBuilder);
  private disciplineService = inject(DisciplineService);
  private specialtyService = inject(SpecialtyService);
  private toastService = inject(ToastService);

  ngOnInit() {
    this.form = this.fb.group({
      specialtyId: ['', Validators.required],
      specialtyOfferingId: ['', Validators.required],
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

    this.specialtyService.getAll({ size: 200 }).subscribe((page) => {
      this.specialties = page.content;
    });
  }

  onSpecialtyChange() {
    const specialtyId = +this.form.get('specialtyId')!.value;
    this.form.patchValue({ specialtyOfferingId: '' });
    this.offerings = [];
    if (specialtyId) {
      this.specialtyService.getOfferings(specialtyId).subscribe((offs) => {
        this.offerings = offs.filter((o) => !this.existingSpecialtyOfferingIds.includes(o.id));
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

    this.disciplineService
      .addSpecialtyDiscipline(+v.specialtyOfferingId, this.disciplineId)
      .pipe(
        switchMap((sd) =>
          this.disciplineService.addHours(sd.id, {
            academicYear: v.academicYear,
            ectsCredits: +v.ectsCredits,
            totalHours: +v.totalHours,
            classroomHours: +v.classroomHours,
            lectureHours: +(v.lectureHours ?? 0),
            seminarHours: +(v.seminarHours ?? 0),
            laboratoryHours: +(v.laboratoryHours ?? 0),
            individualHours: +(v.individualHours ?? 0),
            selfWorkHours: +(v.selfWorkHours ?? 0),
          }).pipe(
            switchMap(() =>
              this.disciplineService.getSpecialtyDisciplines(this.disciplineId)
            ),
          ),
        ),
      )
      .subscribe({
        next: (sdList) => {
          const created = sdList.find((sd) => sd.specialtyOfferingId === +v.specialtyOfferingId);
          this.toastService.success('Набір додано до дисципліни');
          this.submitting = false;
          this.saved.emit(created);
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
