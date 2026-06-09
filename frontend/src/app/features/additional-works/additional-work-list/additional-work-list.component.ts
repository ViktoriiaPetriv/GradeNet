import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AdditionalWorkService } from '../../../core/services/additional-work.service';
import { CommissionService } from '../../../core/services/commission.service';
import { BookService } from '../../../core/services/book.service';
import { AdditionalWork, WorkType } from '../../../models/additional-work.model';
import { Commission } from '../../../models/commission.model';
import { BookNumber } from '../../../models/book.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { AdditionalWorkModalComponent } from '../additional-work-modal/additional-work-modal.component';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-additional-work-list',
  standalone: true,
  imports: [SlicePipe, PageHeaderComponent, ModalComponent, ConfirmDialogComponent, AdditionalWorkModalComponent, PaginationComponent],
  templateUrl: './additional-work-list.component.html',
  styleUrl: './additional-work-list.component.css',
})
export class AdditionalWorkListComponent implements OnInit {
  works = signal<AdditionalWork[]>([]);
  commissions = signal<Commission[]>([]);
  bookMap = signal<Map<number, BookNumber>>(new Map());

  filterType = signal<WorkType | ''>('');
  filterCommissionId = signal<number | null>(null);
  sortBy = signal<string | null>(null);
  sortDir = signal<'asc' | 'desc'>('asc');

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];

  modalOpen = signal(false);
  isEdit = signal(false);
  editingWork = signal<AdditionalWork | null>(null);

  deleteModalOpen = signal(false);
  workToDelete = signal<AdditionalWork | null>(null);

  private workService = inject(AdditionalWorkService);
  private commissionService = inject(CommissionService);
  private bookService = inject(BookService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authState = inject(AuthStateService);

  isAdminOrManager = this.authState.isAdminOrManager;

  currentPageUi = computed(() => this.currentPage() + 1);

  paginationInfo = computed(() => {
    const total = this.totalPages();
    if (total === 0) return 'Немає записів';
    return `Сторінка ${this.currentPageUi()} з ${total}`;
  });

  toggleSort(column: string) {
    if (column === 'studentName') {
      if (this.sortBy() === column) {
        if (this.sortDir() === 'asc') {
          this.sortDir.set('desc');
        } else {
          this.sortBy.set(null);
          this.sortDir.set('asc');
        }
      } else {
        this.sortBy.set(column);
        this.sortDir.set('asc');
      }
      return;
    }
    if (this.sortBy() === column) {
      if (this.sortDir() === 'asc') {
        this.sortDir.set('desc');
      } else {
        this.sortBy.set(null);
        this.sortDir.set('asc');
      }
    } else {
      this.sortBy.set(column);
      this.sortDir.set('asc');
    }
    this.currentPage.set(0);
    this.load();
  }

  sortedWorks = computed(() => {
    const col = this.sortBy();
    const dir = this.sortDir();
    if (col !== 'studentName') return this.works();
    return [...this.works()].sort((a, b) => {
      const aVal = this.studentName(a.bookNumberId).toLowerCase();
      const bVal = this.studentName(b.bookNumberId).toLowerCase();
      if (aVal < bVal) return dir === 'asc' ? -1 : 1;
      if (aVal > bVal) return dir === 'asc' ? 1 : -1;
      return 0;
    });
  });

  ngOnInit() {
    const commissionId = this.route.snapshot.queryParamMap.get('commissionId');
    if (commissionId) this.filterCommissionId.set(Number(commissionId));
    this.load();
    this.commissionService.getAll().subscribe(data => this.commissions.set(data));
  }

  load() {
    const sortBy = this.sortBy();
    const serverSortBy = (sortBy && sortBy !== 'studentName') ? sortBy : undefined;
    this.workService.getPage(
      this.currentPage(),
      this.perPage(),
      this.filterType() || undefined,
      this.filterCommissionId() ?? undefined,
      serverSortBy,
      serverSortBy ? this.sortDir() : undefined
    ).subscribe(r => {
      this.works.set(r.content);
      this.totalPages.set(r.totalPages);
      this.totalElements.set(r.totalElements);
      this.loadBookMap(r.content);
    });
  }

  private loadBookMap(works: AdditionalWork[]) {
    const ids = [...new Set(works.map(w => w.bookNumberId))];
    if (!ids.length) return;
    forkJoin(ids.map(id => this.bookService.findById(id))).subscribe(books => {
      const map = new Map<number, BookNumber>();
      books.forEach(b => map.set(b.id, b));
      this.bookMap.set(map);
    });
  }

  studentName(bookNumberId: number): string {
    const b = this.bookMap().get(bookNumberId);
    if (!b) return '—';
    return `${b.studentLastName ?? ''} ${b.studentFirstName ?? ''}`.trim();
  }

  onFilterType(e: Event) {
    this.filterType.set((e.target as HTMLSelectElement).value as WorkType | '');
    this.currentPage.set(0);
    this.load();
  }

  onFilterCommission(e: Event) {
    const val = (e.target as HTMLSelectElement).value;
    this.filterCommissionId.set(val ? Number(val) : null);
    this.currentPage.set(0);
    this.load();
  }

  onPageChange(page: number) {
    this.currentPage.set(page - 1);
    this.load();
  }

  onPerPageChange(size: number) {
    this.perPage.set(size);
    this.currentPage.set(0);
    this.load();
  }

  openCreate() {
    this.editingWork.set(null);
    this.isEdit.set(false);
    this.modalOpen.set(true);
  }

  openEdit(work: AdditionalWork) {
    this.editingWork.set(work);
    this.isEdit.set(true);
    this.modalOpen.set(true);
  }

  closeModal() { this.modalOpen.set(false); }

  onSaved() {
    this.load();
    this.closeModal();
  }

  openDeleteModal(work: AdditionalWork) {
    this.workToDelete.set(work);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.workToDelete.set(null);
  }

  confirmDelete() {
    const w = this.workToDelete();
    if (!w) return;
    this.workService.delete(w.id).subscribe(() => {
      this.toastService.success('Роботу видалено');
      this.load();
      this.closeDeleteModal();
    });
  }

  viewWork(id: number) {
    this.router.navigate(['/additional-works', id]);
  }

  typeLabel(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: 'Курсова',
      EDUCATIONAL_PRACTICE: 'Навчальна практика',
      PRODUCTION_PRACTICE: 'Виробнича практика',
      QUALIFICATION: 'Кваліфікаційна',
      COMPREHENSIVE_EXAM: 'Комплексний екзамен',
    };
    return map[type] ?? type;
  }

  typeClass(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: 'badge-course',
      EDUCATIONAL_PRACTICE: 'badge-practice',
      PRODUCTION_PRACTICE: 'badge-practice',
      QUALIFICATION: 'badge-qualification',
      COMPREHENSIVE_EXAM: 'badge-qualification',
    };
    return map[type] ?? '';
  }

  isCommissionActive(c: Commission): boolean {
    if (!c.endDate) return true;
    return new Date(c.endDate) >= new Date(new Date().toDateString());
  }

  sortedCommissions = computed(() => {
    const all = this.commissions();
    return [
      ...all.filter(c => this.isCommissionActive(c)),
      ...all.filter(c => !this.isCommissionActive(c)),
    ];
  });

  commissionLabel(commissionId: number): string {
    const c = this.commissions().find(c => c.id === commissionId);
    if (!c) return `#${commissionId}`;
    return `${this.formatDate(c.startDate)} — ${c.endDate ? this.formatDate(c.endDate) : '...'}`;
  }

  formatDate(date: string): string {
    if (!date) return '—';
    const [y, m, d] = date.split('-');
    return `${d}.${m}.${y}`;
  }
}
