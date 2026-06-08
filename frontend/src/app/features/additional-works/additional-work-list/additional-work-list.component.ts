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

@Component({
  selector: 'app-additional-work-list',
  standalone: true,
  imports: [SlicePipe, PageHeaderComponent, ModalComponent, ConfirmDialogComponent, AdditionalWorkModalComponent],
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

  filtered = computed(() => {
    let list = this.works();
    const t = this.filterType();
    const cId = this.filterCommissionId();
    if (t) list = list.filter(w => w.type === t);
    if (cId) list = list.filter(w => w.commissionId === cId);

    const col = this.sortBy();
    const dir = this.sortDir();
    if (col) {
      list = [...list].sort((a, b) => {
        let aVal: any;
        let bVal: any;
        if (col === 'studentName') {
          aVal = this.studentName(a.bookNumberId).toLowerCase();
          bVal = this.studentName(b.bookNumberId).toLowerCase();
        } else {
          aVal = (a as any)[col] ?? '';
          bVal = (b as any)[col] ?? '';
          if (typeof aVal === 'string') aVal = aVal.toLowerCase();
          if (typeof bVal === 'string') bVal = bVal.toLowerCase();
        }
        if (aVal < bVal) return dir === 'asc' ? -1 : 1;
        if (aVal > bVal) return dir === 'asc' ? 1 : -1;
        return 0;
      });
    }
    return list;
  });

  toggleSort(column: string) {
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
  }

  ngOnInit() {
    const commissionId = this.route.snapshot.queryParamMap.get('commissionId');
    if (commissionId) this.filterCommissionId.set(Number(commissionId));
    this.load();
    this.commissionService.getAll().subscribe(data => this.commissions.set(data));
  }

  load() {
    this.workService.getAll().subscribe(data => {
      this.works.set(data);
      this.loadBookMap(data);
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
  }

  onFilterCommission(e: Event) {
    const val = (e.target as HTMLSelectElement).value;
    this.filterCommissionId.set(val ? Number(val) : null);
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
