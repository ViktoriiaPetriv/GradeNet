import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BookService } from '../../../core/services/book.service';
import { BookNumber, BookNumberStatus } from '../../../models/book.model';
import { ToastService } from '../../../core/services/toast.service';
import { BookModalComponent } from '../book-modal/book-modal.component';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [CommonModule, BookModalComponent],
  templateUrl: './book-list.component.html',
  styleUrl: './book-list.component.css',
})
export class BookListComponent implements OnInit {
  books = signal<BookNumber[]>([]);
  search = signal('');
  currentPage = signal(0);
  totalPages = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];

  modalOpen = signal(false);
  isEdit = signal(false);
  editingBook = signal<BookNumber | null>(null);

  deleteModalOpen = signal(false);
  bookToDelete = signal<BookNumber | null>(null);

  fillModalOpen = signal(false);
  handModalOpen = signal(false);
  bookToAction = signal<BookNumber | null>(null);

  private bookService = inject(BookService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private authState = inject(AuthStateService);

  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;

  currentPageUi = computed(() => this.currentPage() + 1);

  pages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPageUi();

    if (total <= 5) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }

    const range: number[] = [];
    range.push(1);

    const leftBound = Math.max(2, current - 1);
    const rightBound = Math.min(total - 1, current + 1);

    if (leftBound > 2) range.push(-1);
    for (let i = leftBound; i <= rightBound; i++) range.push(i);
    if (rightBound < total - 1) range.push(-1);

    range.push(total);
    return range;
  });

  paginationInfo = computed(() => {
    const total = this.totalPages();
    if (total === 0) return 'Немає записів';
    return `Сторінка ${this.currentPageUi()} з ${total}`;
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.bookService
      .findAll(this.search() || undefined, this.currentPage(), this.perPage())
      .subscribe((r) => {
        this.books.set(r.content);
        this.totalPages.set(r.totalPages);
      });
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
    this.currentPage.set(0);
    this.load();
  }

  onPerPageChange(e: Event) {
    this.perPage.set(Number((e.target as HTMLSelectElement).value));
    this.currentPage.set(0);
    this.load();
  }

  setPage(p: number) {
    const zero = p - 1;
    if (zero < 0 || zero >= this.totalPages()) return;
    this.currentPage.set(zero);
    this.load();
  }

  goFirst() {
    this.setPage(1);
  }
  goLast() {
    this.setPage(this.totalPages());
  }
  goPrev() {
    this.setPage(this.currentPageUi() - 1);
  }
  goNext() {
    this.setPage(this.currentPageUi() + 1);
  }

  openCreate() {
    this.editingBook.set(null);
    this.isEdit.set(false);
    this.modalOpen.set(true);
  }

  openEdit(book: BookNumber) {
    this.editingBook.set(book);
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

  openDeleteModal(book: BookNumber) {
    this.bookToDelete.set(book);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.bookToDelete.set(null);
  }

  confirmDelete() {
    const b = this.bookToDelete();
    if (!b) return;
    this.bookService.delete(b.id).subscribe(() => {
      this.toastService.success('Заліковку видалено');
      this.load();
      this.closeDeleteModal();
    });
  }

  openFillModal(book: BookNumber) {
    this.bookToAction.set(book);
    this.fillModalOpen.set(true);
  }

  closeFillModal() {
    this.fillModalOpen.set(false);
    this.bookToAction.set(null);
  }

  confirmFill() {
    const b = this.bookToAction();
    if (!b) return;
    this.bookService.markAsFilled(b.id).subscribe(() => {
      this.toastService.success('Заліковку заповнено');
      this.load();
      this.closeFillModal();
    });
  }

  openHandModal(book: BookNumber) {
    this.bookToAction.set(book);
    this.handModalOpen.set(true);
  }

  closeHandModal() {
    this.handModalOpen.set(false);
    this.bookToAction.set(null);
  }

  confirmHand() {
    const b = this.bookToAction();
    if (!b) return;
    this.bookService.markAsHanded(b.id).subscribe(() => {
      this.toastService.success('Заліковку видано студенту');
      this.load();
      this.closeHandModal();
    });
  }

  viewBook(id: number) {
    this.router.navigate(['/books', id]);
  }

  statusLabel(status: BookNumberStatus): string {
    const map: Record<BookNumberStatus, string> = {
      REGISTERED: 'Зареєстровано',
      FILLED: 'Заповнено',
      HANDED: 'Видано',
    };
    return map[status] || status;
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }
}
