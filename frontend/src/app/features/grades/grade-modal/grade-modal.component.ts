import { Component, Input, Output, EventEmitter, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GradeDTO, GradeCreateRequest, GradeUpdateRequest, AssessmentType } from '../../../models/grade.model';
import { GradeService } from '../../../core/services/grade.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-grade-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './grade-modal.component.html',
  styleUrl: './grade-modal.component.css',
})
export class GradeModalComponent implements OnInit {
  @Input() entryId!: number;
  @Input() grade: GradeDTO | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  assessmentDate = signal('');
  universityGrade = signal<number | null>(null);
  assessmentType = signal<AssessmentType>('EXAM');

  constructor(private gradeService: GradeService, private toastService: ToastService) {}

  ngOnInit() {
    if (this.grade) {
      const d = new Date(this.grade.assessmentDate);
      this.assessmentDate.set(d.toISOString().slice(0, 16));
      this.universityGrade.set(this.grade.universityGrade);
      this.assessmentType.set(this.grade.assessmentType);
    } else {
      this.assessmentDate.set(new Date().toISOString().slice(0, 16));
    }
  }

  submit() {
    const grade = this.universityGrade();
    if (grade === null || grade < 0 || grade > 100 || !this.assessmentDate()) return;

    if (this.grade) {
      const req: GradeUpdateRequest = {
        assessmentDate: this.assessmentDate(),
        universityGrade: grade,
        assessmentType: this.assessmentType(),
      };
      this.gradeService.updateGrade(this.grade.id, req).subscribe({
        next: () => { this.toastService.success('Оцінку оновлено'); this.saved.emit(); },
        error: (err) => this.toastService.error(err?.error?.message || 'Помилка збереження'),
      });
    } else {
      const req: GradeCreateRequest = {
        entryId: this.entryId,
        assessmentDate: this.assessmentDate(),
        universityGrade: grade,
        assessmentType: this.assessmentType(),
      };
      this.gradeService.createGrade(req).subscribe({
        next: () => { this.toastService.success('Оцінку додано'); this.saved.emit(); },
        error: (err) => this.toastService.error(err?.error?.message || 'Помилка збереження'),
      });
    }
  }
}
