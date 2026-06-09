import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommissionService } from '../../../core/services/commission.service';
import { Commission } from '../../../models/commission.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CommissionModalComponent } from '../commission-modal/commission-modal.component';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-commission-list',
  standalone: true,
  imports: [PageHeaderComponent, ModalComponent, ConfirmDialogComponent, CommissionModalComponent, PaginationComponent],
  templateUrl: './commission-list.component.html',
  styleUrl: './commission-list.component.css',
})
export class CommissionListComponent implements OnInit {
  commissions = signal<Commission[]>([]);
  statusFilter = signal<'all' | 'active' | 'inactive'>('all');
  sortBy = signal<string | null>(null);
  sortDir = signal<'asc' | 'desc'>('asc');

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];

  modalOpen = signal(false);
  isEdit = signal(false);
  editingCommission = signal<Commission | null>(null);

  deleteModalOpen = signal(false);
  commissionToDelete = signal<Commission | null>(null);

  private commissionService = inject(CommissionService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private authState = inject(AuthStateService);

  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;

  currentPageUi = computed(() => this.currentPage() + 1);

  paginationInfo = computed(() => {
    const total = this.totalPages();
    if (total === 0) return 'Немає записів';
    return `Сторінка ${this.currentPageUi()} з ${total}`;
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
    this.currentPage.set(0);
    this.load();
  }

  isActive(c: Commission): boolean {
    if (!c.endDate) return true;
    return new Date(c.endDate) >= new Date(new Date().toDateString());
  }

  ngOnInit() {
    this.load();
  }

  load() {
    const sortBy = this.sortBy();
    this.commissionService.getPage(
      this.currentPage(),
      this.perPage(),
      this.statusFilter(),
      sortBy ?? undefined,
      sortBy ? this.sortDir() : undefined
    ).subscribe(r => {
      this.commissions.set(r.content);
      this.totalPages.set(r.totalPages);
      this.totalElements.set(r.totalElements);
    });
  }

  onStatusFilter(status: 'all' | 'active' | 'inactive') {
    this.statusFilter.set(status);
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
    this.editingCommission.set(null);
    this.isEdit.set(false);
    this.modalOpen.set(true);
  }

  openEdit(commission: Commission) {
    this.editingCommission.set(commission);
    this.isEdit.set(true);
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
  }

  onSaved() {
    this.load();
    this.closeModal();
  }

  openDeleteModal(commission: Commission) {
    this.commissionToDelete.set(commission);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.commissionToDelete.set(null);
  }

  confirmDelete() {
    const c = this.commissionToDelete();
    if (!c) return;
    this.commissionService.delete(c.id).subscribe(() => {
      this.toastService.success('Комісію видалено');
      this.load();
      this.closeDeleteModal();
    });
  }

  viewCommission(id: number) {
    this.router.navigate(['/commissions', id]);
  }

  formatDate(date: string): string {
    if (!date) return '—';
    const [y, m, d] = date.split('-');
    return `${d}.${m}.${y}`;
  }
}
