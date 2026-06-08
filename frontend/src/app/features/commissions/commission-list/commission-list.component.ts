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

@Component({
  selector: 'app-commission-list',
  standalone: true,
  imports: [PageHeaderComponent, ModalComponent, ConfirmDialogComponent, CommissionModalComponent],
  templateUrl: './commission-list.component.html',
  styleUrl: './commission-list.component.css',
})
export class CommissionListComponent implements OnInit {
  commissions = signal<Commission[]>([]);
  search = signal('');
  statusFilter = signal<'all' | 'active' | 'inactive'>('all');
  sortBy = signal<string | null>(null);
  sortDir = signal<'asc' | 'desc'>('asc');

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

  filtered = computed(() => {
    const s = this.search().toLowerCase();
    const sf = this.statusFilter();
    let list = this.commissions().filter(c => {
      if (s && !this.formatDate(c.startDate).includes(s) && !this.formatDate(c.endDate ?? '').includes(s)) return false;
      if (sf === 'active') return this.isActive(c);
      if (sf === 'inactive') return !this.isActive(c);
      return true;
    });

    const col = this.sortBy();
    const dir = this.sortDir();
    if (col) {
      list = [...list].sort((a, b) => {
        let aVal: any;
        let bVal: any;
        if (col === 'membersCount') {
          aVal = a.members.length;
          bVal = b.members.length;
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

  isActive(c: Commission): boolean {
    if (!c.endDate) return true;
    return new Date(c.endDate) >= new Date(new Date().toDateString());
  }

  ngOnInit() {
    this.load();
  }

  load() {
    this.commissionService.getAll().subscribe(data => this.commissions.set(data));
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
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
