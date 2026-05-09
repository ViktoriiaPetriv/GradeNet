import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupService } from '../../../core/services/group.service';
import { StudentGroup, StudentGroupMember } from '../../../models/group.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../models/user.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { HeroCardComponent } from '../../../shared/hero-card/hero-card.component';
import { InfoCardComponent } from '../../../shared/info-card/info-card.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { GroupModalComponent } from '../group-modal/group-modal.component';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-group-detail',
  standalone: true,
  imports: [
    CommonModule,
    PageHeaderComponent,
    HeroCardComponent,
    InfoCardComponent,
    ModalComponent,
    ConfirmDialogComponent,
    GroupModalComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './group-detail.component.html',
  styleUrl: './group-detail.component.css',
})
export class GroupDetailComponent implements OnInit {
  group = signal<StudentGroup | null>(null);
  members = signal<StudentGroupMember[]>([]);
  loading = signal(true);

  editModalOpen = signal(false);
  deleteModalOpen = signal(false);
  addMemberOpen = signal(false);
  memberToRemove = signal<StudentGroupMember | null>(null);
  removeModalOpen = signal(false);

  searchControl = new FormControl<string>('');
  searchResults = signal<User[]>([]);
  selectedStudent = signal<User | null>(null);
  selectedBook = signal<any | null>(null);
  studentBooks = signal<any[]>([]);
  availableBooks = signal<any[]>([]);
  searching = signal(false);
  loadingBooks = signal(false);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private groupService = inject(GroupService);
  private toastService = inject(ToastService);
  private userService = inject(UserService);

  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (id) this.load(id);
    });

    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
      )
      .subscribe((query) => {
        if (query && query.trim()) {
          this.performSearch(query);
        } else {
          this.searchResults.set([]);
        }
      });
  }

  load(id: number) {
    this.loading.set(true);
    this.groupService.getById(id).subscribe({
      next: (g) => {
        this.group.set(g);
        this.loading.set(false);
        this.loadMembers(g.id);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/groups']);
      },
    });
  }

  loadMembers(groupId: number) {
    this.groupService.getMembers(groupId).subscribe((m) => this.members.set(m));
  }

  openEdit() { this.editModalOpen.set(true); }
  closeEdit() { this.editModalOpen.set(false); }

  onSaved() {
    this.closeEdit();
    this.load(this.group()!.id);
  }

  openDeleteModal() { this.deleteModalOpen.set(true); }
  closeDeleteModal() { this.deleteModalOpen.set(false); }

  confirmDelete() {
    const id = this.group()?.id;
    if (!id) return;
    this.groupService.delete(id).subscribe(() => {
      this.toastService.success('Групу видалено');
      this.router.navigate(['/groups']);
    });
  }

  performSearch(query: string) {
    this.searching.set(true);
    this.userService.searchStudents(query).subscribe({
      next: (users) => {
        this.searchResults.set(users);
        this.searching.set(false);
      },
      error: () => {
        this.searchResults.set([]);
        this.searching.set(false);
      },
    });
  }

  selectStudent(student: User) {
    this.selectedStudent.set(student);
    this.selectedBook.set(null);
    this.searchControl.reset();
    this.searchResults.set([]);
    this.loadBooks(student.id);
  }

  private loadBooks(studentId: number) {
    this.loadingBooks.set(true);
    this.userService.getProfile(studentId).subscribe({
      next: (profile) => {
        const allBooks = profile.books || [];
        const members = this.members();

        // Check if this student already has a book in this group
        const studentInGroup = members.some(m => m.studentId === studentId);

        // If student already has a book in this group, show a message and disable adding more
        if (studentInGroup) {
          this.studentBooks.set(allBooks);
          this.availableBooks.set([]);
          this.loadingBooks.set(false);
          return;
        }

        // Filter out books that are already in any group
        const allAddedBookIds = new Set(members.map(m => m.bookNumberId));
        const available = allBooks.filter(book => !allAddedBookIds.has(book.bookId));

        this.studentBooks.set(allBooks);
        this.availableBooks.set(available);
        this.loadingBooks.set(false);

        // If only one available book - auto-select it
        if (available.length === 1) {
          this.selectedBook.set(available[0]);
        }
      },
      error: () => {
        this.studentBooks.set([]);
        this.availableBooks.set([]);
        this.loadingBooks.set(false);
      },
    });
  }

  openAddMember() {
    this.searchControl.reset();
    this.selectedStudent.set(null);
    this.searchResults.set([]);
    this.addMemberOpen.set(true);
  }
  closeAddMember() {
    this.addMemberOpen.set(false);
    this.selectedStudent.set(null);
    this.searchResults.set([]);
  }

  submitAddMember() {
    const student = this.selectedStudent();
    if (!student) {
      this.toastService.error('Виберіть студента');
      return;
    }

    // Check if student already has a book in this group
    const studentInGroup = this.members().some(m => m.studentId === student.id);
    if (studentInGroup) {
      this.toastService.error('Цей студент вже додан до групи. Один студент може бути в групі тільки з однією залікою');
      return;
    }

    const availableBooks = this.availableBooks();
    if (availableBooks.length === 0) {
      this.toastService.error('Усі залікові книжки цього студента вже додані до інших груп');
      return;
    }

    if (!this.selectedBook()) {
      this.toastService.error('Виберіть залікову книжку');
      return;
    }

    const groupId = this.group()!.id;
    const book = this.selectedBook()!;
    const bookNumberId = book.bookId;

    this.groupService.addMember(groupId, bookNumberId).subscribe({
      next: () => {
        this.toastService.success('Студента додано до групи');
        this.loadMembers(groupId);
        this.closeAddMember();
      },
    });
  }

  openRemoveModal(member: StudentGroupMember) {
    this.memberToRemove.set(member);
    this.removeModalOpen.set(true);
  }
  closeRemoveModal() {
    this.removeModalOpen.set(false);
    this.memberToRemove.set(null);
  }

  confirmRemoveMember() {
    const member = this.memberToRemove();
    const groupId = this.group()?.id;
    if (!member || !groupId) return;
    this.groupService.removeMember(groupId, member.bookNumberId).subscribe(() => {
      this.toastService.success('Студента видалено з групи');
      this.loadMembers(groupId);
      this.closeRemoveModal();
    });
  }

  goBack() { this.router.navigate(['/groups']); }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  getStudentInitials(u: User): string {
    return (u.lastName?.[0] || '') + (u.firstName?.[0] || '');
  }

  getStudentFullName(u: User): string {
    return `${u.lastName || ''} ${u.firstName || ''}`.trim();
  }

  removeMemberSelection() {
    this.selectedStudent.set(null);
    this.selectedBook.set(null);
    this.studentBooks.set([]);
    this.availableBooks.set([]);
  }

  selectBook(book: any) {
    this.selectedBook.set(book);
  }

  getBookLabel(book: any): string {
    const status = book.bookNumberStatus || 'ACTIVE';
    return `${book.bookNumber} (${status})`;
  }

  isBookAvailable(bookId: number): boolean {
    return this.availableBooks().some(b => b.bookId === bookId);
  }
}
