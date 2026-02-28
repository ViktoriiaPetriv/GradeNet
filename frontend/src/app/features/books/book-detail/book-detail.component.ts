import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BookService } from '../../../core/services/book.service';
import { BookNumber, BookNumberStatus } from '../../../models/book.model';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { UserService } from '../../../core/services/user.service';
import { OrgInfo, Specialty } from '../../../models/org.model';
import { User } from '../../../models/user.model';
import { ToastService } from '../../../core/services/toast.service';
import { BookModalComponent } from '../book-modal/book-modal.component';

@Component({
  selector: 'app-book-detail',
  standalone: true,
  imports: [CommonModule, BookModalComponent],
  templateUrl: './book-detail.component.html',
  styleUrl: './book-detail.component.css',
})
export class BookDetailComponent implements OnInit {
  book = signal<BookNumber | null>(null);
  student = signal<User | null>(null);
  specialty = signal<Specialty | null>(null);
  orgInfo = signal<OrgInfo | null>(null);

  editModalOpen = signal(false);
  fillModalOpen = signal(false);
  handModalOpen = signal(false);
  deleteModalOpen = signal(false);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private bookService = inject(BookService);
  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private toastService = inject(ToastService);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    this.load(id);
  }

  private load(id: number) {
    this.bookService.findById(id).subscribe((book) => {
      this.book.set(book);
      this.userService.findById(book.studentId).subscribe((u) => this.student.set(u));
      if (book.specialtyId) {
        this.specialtyService.getById(book.specialtyId).subscribe((s) => this.specialty.set(s));
        this.specialtyService.getOrgInfo(book.specialtyId).subscribe((o) => this.orgInfo.set(o));
      }
    });
  }

  confirmDelete() {
    const b = this.book();
    if (!b) return;
    this.bookService.delete(b.id).subscribe(() => {
      this.toastService.success('Заліковку видалено');
      this.router.navigate(['/books']);
    });
  }

  confirmFill() {
    const b = this.book();
    if (!b) return;
    this.bookService.markAsFilled(b.id).subscribe((updated) => {
      this.book.set(updated);
      this.toastService.success('Заліковку заповнено');
      this.fillModalOpen.set(false);
    });
  }

  confirmHand() {
    const b = this.book();
    if (!b) return;
    this.bookService.markAsHanded(b.id).subscribe((updated) => {
      this.book.set(updated);
      this.toastService.success('Заліковку видано студенту');
      this.handModalOpen.set(false);
    });
  }

  onSaved() {
    this.editModalOpen.set(false);
    this.load(this.book()!.id);
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

  getStudentName(): string {
    const u = this.student();
    if (!u) return '—';
    return [u.lastName, u.firstName, u.patronymic].filter(Boolean).join(' ');
  }

  goBack() {
    this.router.navigate(['/books']);
  }
}
