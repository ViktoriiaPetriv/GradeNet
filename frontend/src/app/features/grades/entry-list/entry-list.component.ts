import { Component, OnInit, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { GradeBookEntryDTO, GradeBookEntryFilter, EntryStatus } from '../../../models/grade.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { EntryCreateWizardComponent } from '../entry-create-wizard/entry-create-wizard.component';

@Component({
  selector: 'app-entry-list',
  standalone: true,
  imports: [PageHeaderComponent, ModalComponent, EntryCreateWizardComponent],
  templateUrl: './entry-list.component.html',
  styleUrl: './entry-list.component.css',
})
export class EntryListComponent implements OnInit {
  entries = signal<GradeBookEntryDTO[]>([]);
  statusFilter = signal<EntryStatus | ''>('');
  academicYearFilter = signal('');
  createWizardOpen = signal(false);

  private gradeService = inject(GradeService);
  private router = inject(Router);
  private authState = inject(AuthStateService);

  isAdmin = this.authState.isAdmin;
  isProfessor = this.authState.isProfessor;
  currentUserId = this.authState.currentUserId;

  ngOnInit() {
    this.load();
  }

  load() {
    const filter: GradeBookEntryFilter = {};
    const status = this.statusFilter();
    if (status) filter.status = status;
    const year = this.academicYearFilter();
    if (year) filter.academicYear = year;
    if (this.isProfessor()) {
      const uid = this.currentUserId();
      if (uid) filter.professorId = uid;
    }
    this.gradeService.getEntries(filter).subscribe((data) => this.entries.set(data));
  }

  onStatusChange(e: Event) {
    this.statusFilter.set((e.target as HTMLSelectElement).value as EntryStatus | '');
    this.load();
  }

  onYearChange(e: Event) {
    this.academicYearFilter.set((e.target as HTMLInputElement).value);
    this.load();
  }

  onWizardSaved() {
    this.createWizardOpen.set(false);
    this.load();
  }

  viewEntry(id: number) {
    this.router.navigate(['/grades', id]);
  }

  openBulkGrade() {
    this.router.navigate(['/grades/bulk']);
  }

  openGroupReport() {
    this.router.navigate(['/grades/group-report']);
  }

  statusLabel(s: EntryStatus): string {
    return s === 'IN_PROGRESS' ? 'Відкрито' : 'Закрито';
  }

  resultLabel(r: string | null): string {
    if (!r) return '—';
    return r === 'PASSED' ? 'Зараховано' : 'Не зараховано';
  }

  isReportDatePast(entry: GradeBookEntryDTO): boolean {
    if (!entry.reportDate) return false;
    return new Date(entry.reportDate) < new Date(new Date().toDateString());
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }
}
