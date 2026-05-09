import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { GradeBookEntryDTO, GradeDTO } from '../../../models/grade.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { GradeModalComponent } from '../grade-modal/grade-modal.component';

@Component({
  selector: 'app-entry-detail',
  standalone: true,
  imports: [ConfirmDialogComponent, ModalComponent, GradeModalComponent],
  templateUrl: './entry-detail.component.html',
  styleUrl: './entry-detail.component.css',
})
export class EntryDetailComponent implements OnInit {
  entry = signal<GradeBookEntryDTO | null>(null);
  grades = signal<GradeDTO[]>([]);

  gradeModalOpen = signal(false);
  editingGrade = signal<GradeDTO | null>(null);

  deleteGradeModalOpen = signal(false);
  gradeToDelete = signal<GradeDTO | null>(null);

  closeEntryModalOpen = signal(false);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gradeService = inject(GradeService);
  private toastService = inject(ToastService);
  private authState = inject(AuthStateService);

  isAdmin = this.authState.isAdmin;

  isReportDatePast = computed(() => {
    const e = this.entry();
    if (!e?.reportDate) return false;
    return new Date(e.reportDate) < new Date(new Date().toDateString());
  });

  canEditGrades = computed(() => {
    const e = this.entry();
    if (!e) return false;
    if (e.status === 'COMPLETED') return false;
    if (this.isReportDatePast() && !this.isAdmin()) return false;
    return true;
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    this.loadEntry(id);
  }

  private loadEntry(id: number) {
    this.gradeService.getEntryById(id).subscribe((e) => {
      this.entry.set(e);
      this.loadGrades(id);
    });
  }

  private loadGrades(entryId: number) {
    this.gradeService.getGradesByEntry(entryId).subscribe((g) => this.grades.set(g));
  }

  openAddGrade() {
    this.editingGrade.set(null);
    this.gradeModalOpen.set(true);
  }

  openEditGrade(grade: GradeDTO) {
    this.editingGrade.set(grade);
    this.gradeModalOpen.set(true);
  }

  closeGradeModal() {
    this.gradeModalOpen.set(false);
    this.editingGrade.set(null);
  }

  onGradeSaved() {
    this.closeGradeModal();
    this.loadGrades(this.entry()!.id);
  }

  openDeleteGrade(grade: GradeDTO) {
    this.gradeToDelete.set(grade);
    this.deleteGradeModalOpen.set(true);
  }

  confirmDeleteGrade() {
    const g = this.gradeToDelete();
    if (!g) return;
    this.gradeService.deleteGrade(g.id).subscribe({
      next: () => {
        this.toastService.success('Оцінку видалено');
        this.gradeToDelete.set(null);
        this.deleteGradeModalOpen.set(false);
        this.loadGrades(this.entry()!.id);
      },
      error: (err) => this.toastService.error(err?.error?.message || 'Помилка видалення'),
    });
  }

  confirmCloseEntry() {
    const e = this.entry();
    if (!e) return;
    this.gradeService.closeEntries([e.id]).subscribe({
      next: () => {
        this.toastService.success('Запис закрито');
        this.closeEntryModalOpen.set(false);
        this.loadEntry(e.id);
      },
      error: (err) => this.toastService.error(err?.error?.message || 'Помилка закриття'),
    });
  }

  goBack() {
    this.router.navigate(['/grades']);
  }

  statusLabel(s: string): string {
    return s === 'IN_PROGRESS' ? 'Відкрито' : 'Закрито';
  }

  resultLabel(r: string | null): string {
    if (!r) return '—';
    return r === 'PASSED' ? 'Зараховано' : 'Не зараховано';
  }

  typeLabel(t: string): string {
    return t === 'EXAM' ? 'Іспит' : 'Залік';
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  formatDateTime(d: string): string {
    return new Date(d).toLocaleString('uk-UA');
  }
}
