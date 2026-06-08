import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { GradeService } from '../../../core/services/grade.service';
import { DisciplineService } from '../../../core/services/discipline.service';
import { GroupService } from '../../../core/services/group.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SpecialtyDisciplineDTO } from '../../../models/discipline.model';
import { BulkGradeEntryDTO } from '../../../models/grade.model';
import { StudentGroup, StudentGroupMember } from '../../../models/group.model';
import { nationalGradeLabel } from '../../../shared/grade-labels';

interface ReportRow {
  member: StudentGroupMember;
  entry: BulkGradeEntryDTO | null;
}

@Component({
  selector: 'app-group-report',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent],
  templateUrl: './group-report.component.html',
  styleUrl: './group-report.component.css',
})
export class GroupReportComponent implements OnInit {
  groups = signal<StudentGroup[]>([]);
  specialtyDisciplines = signal<SpecialtyDisciplineDTO[]>([]);

  selectedGroupId = signal<number | null>(null);
  selectedDisciplineId = signal<number | null>(null);
  academicYear = signal('');

  rows = signal<ReportRow[]>([]);
  loading = signal(false);

  private gradeService = inject(GradeService);
  private disciplineService = inject(DisciplineService);
  private groupService = inject(GroupService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  hasRows = computed(() => this.rows().length > 0);

  passedCount = computed(() => this.rows().filter((r) => r.entry?.result === 'PASSED').length);
  failedCount = computed(() => this.rows().filter((r) => r.entry?.result === 'FAILED').length);
  inProgressCount = computed(() => this.rows().filter((r) => r.entry?.status === 'IN_PROGRESS').length);
  noEntryCount = computed(() => this.rows().filter((r) => !r.entry).length);

  ngOnInit() {
    forkJoin({
      groups: this.groupService.getAll({ size: 200 }),
      disciplines: this.disciplineService.getAllSpecialtyDisciplines(),
    }).subscribe({
      next: ({ groups, disciplines }) => {
        this.groups.set(groups.content ?? []);
        this.specialtyDisciplines.set(disciplines);
      },
      error: () => this.toastService.error('Помилка завантаження даних'),
    });
  }

  load() {
    const groupId = this.selectedGroupId();
    const sdId = this.selectedDisciplineId();
    const year = this.academicYear().trim();

    if (!groupId || !sdId || !year) {
      this.toastService.error('Виберіть групу, дисципліну та навчальний рік');
      return;
    }

    this.loading.set(true);
    forkJoin({
      members: this.groupService.getMembers(groupId),
      entries: this.gradeService.getGroupReport(sdId, year),
    }).subscribe({
      next: ({ members, entries }) => {
        const entryMap = new Map<number, BulkGradeEntryDTO>(
          entries.map((e) => [e.bookNumberId, e]),
        );
        this.rows.set(
          members.map((m) => ({
            member: m,
            entry: entryMap.get(m.bookNumberId) ?? null,
          })),
        );
        this.loading.set(false);
        if (members.length === 0) {
          this.toastService.error('У цій групі немає студентів');
        }
      },
      error: () => {
        this.toastService.error('Помилка завантаження даних');
        this.loading.set(false);
      },
    });
  }

  disciplineLabel(sd: SpecialtyDisciplineDTO): string {
    return sd.discipline.name;
  }

  gradeLabel(row: ReportRow): string {
    if (!row.entry) return '—';
    if (!row.entry.latestGrade) return '—';
    return String(row.entry.latestGrade.universityGrade);
  }

  ectsLabel(row: ReportRow): string {
    return row.entry?.latestGrade?.ectsGrade ?? '—';
  }

  nationalLabel(row: ReportRow): string {
    return nationalGradeLabel(row.entry?.latestGrade?.nationalGrade);
  }

  statusLabel(row: ReportRow): string {
    if (!row.entry) return 'Не внесено';
    return row.entry.status === 'IN_PROGRESS' ? 'Відкрито' : 'Закрито';
  }

  resultLabel(row: ReportRow): string {
    if (!row.entry) return '—';
    if (!row.entry.result) return '—';
    return row.entry.result === 'PASSED' ? 'Зараховано' : 'Не зараховано';
  }

  rowClass(row: ReportRow): string {
    if (!row.entry) return 'row-no-entry';
    if (row.entry.result === 'PASSED') return 'row-passed';
    if (row.entry.result === 'FAILED') return 'row-failed';
    return '';
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  goBack() {
    this.router.navigate(['/grades']);
  }
}
