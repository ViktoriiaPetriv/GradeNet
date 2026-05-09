import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { DisciplineService } from '../../../core/services/discipline.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SpecialtyDisciplineDTO } from '../../../models/discipline.model';
import { BulkGradeEntryDTO, BulkGradeItem, AssessmentType } from '../../../models/grade.model';

interface GradeRow {
  entry: BulkGradeEntryDTO;
  universityGrade: number | null;
  assessmentType: AssessmentType;
  assessmentDate: string;
  skip: boolean;
}

@Component({
  selector: 'app-bulk-grade',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent],
  templateUrl: './bulk-grade.component.html',
  styleUrl: './bulk-grade.component.css',
})
export class BulkGradeComponent implements OnInit {
  specialtyDisciplines = signal<SpecialtyDisciplineDTO[]>([]);
  selectedDisciplineId = signal<number | null>(null);
  academicYear = signal('');

  rows = signal<GradeRow[]>([]);
  loading = signal(false);
  saving = signal(false);

  private gradeService = inject(GradeService);
  private disciplineService = inject(DisciplineService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  defaultDate = new Date().toISOString().slice(0, 16);

  hasRows = computed(() => this.rows().length > 0);
  activeRows = computed(() => this.rows().filter((r) => !r.skip));

  ngOnInit() {
    this.disciplineService.getAllSpecialtyDisciplines().subscribe({
      next: (list) => this.specialtyDisciplines.set(list),
      error: () => this.toastService.error('Помилка завантаження дисциплін'),
    });
  }

  load() {
    const sdId = this.selectedDisciplineId();
    const year = this.academicYear().trim();
    if (!sdId || !year) {
      this.toastService.error('Виберіть дисципліну та вкажіть навчальний рік');
      return;
    }
    this.loading.set(true);
    this.gradeService.getBulkEntries(sdId, year).subscribe({
      next: (entries) => {
        this.rows.set(
          entries.map((e) => ({
            entry: e,
            universityGrade: e.latestGrade?.universityGrade ?? null,
            assessmentType: e.latestGrade?.assessmentType ?? 'EXAM',
            assessmentDate: this.defaultDate,
            skip: false,
          })),
        );
        this.loading.set(false);
        if (entries.length === 0) {
          this.toastService.error('Відкритих записів для цієї дисципліни не знайдено');
        }
      },
      error: () => {
        this.toastService.error('Помилка завантаження студентів');
        this.loading.set(false);
      },
    });
  }

  save() {
    const active = this.activeRows();
    const invalid = active.filter((r) => r.universityGrade == null);
    if (invalid.length > 0) {
      this.toastService.error(`Заповніть оцінки для всіх студентів або позначте їх як "пропустити"`);
      return;
    }

    const grades: BulkGradeItem[] = active.map((r) => ({
      entryId: r.entry.entryId,
      universityGrade: r.universityGrade!,
      assessmentType: r.assessmentType,
      assessmentDate: new Date(r.assessmentDate).toISOString(),
    }));

    this.saving.set(true);
    this.gradeService.createBulkGrades({ grades }).subscribe({
      next: () => {
        this.toastService.success(`Збережено оцінки для ${grades.length} студентів`);
        this.rows.set([]);
        this.saving.set(false);
      },
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Помилка збереження');
        this.saving.set(false);
      },
    });
  }

  disciplineLabel(sd: SpecialtyDisciplineDTO): string {
    return sd.discipline.name;
  }

  formatGrade(row: GradeRow): string {
    if (!row.entry.latestGrade) return '—';
    return `${row.entry.latestGrade.universityGrade} (${row.entry.latestGrade.ectsGrade})`;
  }

  isReportDatePast(row: GradeRow): boolean {
    if (!row.entry.reportDate) return false;
    return new Date(row.entry.reportDate) < new Date(new Date().toDateString());
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  goBack() {
    this.router.navigate(['/grades']);
  }
}
