import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { nationalGradeLabel } from '../../../shared/grade-labels';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { GradeBookEntryDTO, GradeDTO } from '../../../models/grade.model';
import { BookService } from '../../../core/services/book.service';
import { BookNumber } from '../../../models/book.model';
import { DisciplineService } from '../../../core/services/discipline.service';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../models/user.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { GradeModalComponent } from '../grade-modal/grade-modal.component';

@Component({
  selector: 'app-entry-detail',
  standalone: true,
  imports: [RouterLink, ConfirmDialogComponent, ModalComponent, GradeModalComponent],
  templateUrl: './entry-detail.component.html',
  styleUrl: './entry-detail.component.css',
})
export class EntryDetailComponent implements OnInit {
  entry = signal<GradeBookEntryDTO | null>(null);
  grades = signal<GradeDTO[]>([]);
  bookNumber = signal<BookNumber | null>(null);
  disciplineId = signal<number | null>(null);
  professor = signal<User | null>(null);

  gradeModalOpen = signal(false);
  editingGrade = signal<GradeDTO | null>(null);

  deleteGradeModalOpen = signal(false);
  gradeToDelete = signal<GradeDTO | null>(null);

  closeEntryModalOpen = signal(false);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gradeService = inject(GradeService);
  private bookService = inject(BookService);
  private disciplineService = inject(DisciplineService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private authState = inject(AuthStateService);

  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;
  isProfessor = this.authState.isProfessor;
  currentUserId = this.authState.currentUserId;

  isReportDatePast = computed(() => {
    const e = this.entry();
    if (!e?.reportDate) return false;
    return new Date(e.reportDate) < new Date(new Date().toDateString());
  });

  isOwnEntry = computed(() => {
    const e = this.entry();
    return !!e && e.professorId === this.currentUserId();
  });

  canEditGrades = computed(() => {
    const e = this.entry();
    if (!e) return false;
    if (e.status === 'COMPLETED') return false;
    if (this.isReportDatePast() && !this.isAdmin()) return false;
    return true;
  });

  canCloseEntry = computed(() => {
    const e = this.entry();
    if (!e || e.status !== 'IN_PROGRESS' || this.grades().length === 0) return false;
    return this.isAdmin() || (this.isProfessor() && this.isOwnEntry());
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
      this.bookService.findById(e.bookNumberId).subscribe((b) => this.bookNumber.set(b));
      this.disciplineService.getSpecialtyDisciplineById(e.specialtyDisciplineId).subscribe((sd) => this.disciplineId.set(sd.discipline.id));
      this.userService.findById(e.professorId).subscribe((u) => this.professor.set(u));
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

  goToBook() {
    const e = this.entry();
    if (e) this.router.navigate(['/books', e.bookNumberId]);
  }

  goToStudentGrades() {
    const e = this.entry();
    if (e) this.router.navigate(['/grades/student', e.bookNumberId]);
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

  nationalGradeLabel = nationalGradeLabel;

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  formatDateTime(d: string): string {
    return new Date(d).toLocaleString('uk-UA');
  }
}
