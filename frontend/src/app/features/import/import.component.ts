import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ImportService } from '../../core/services/import.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { TokenService } from '../../core/services/token.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import {
  ImportResult,
  DisciplineCheckResult,
  DisciplineCheckItem,
  StudentCheckResult,
  StudentCheckItem,
} from '../../models/import.model';
import { User } from '../../models/user.model';

type Step = 'upload' | 'disciplines' | 'students' | 'preview' | 'result';

interface DisciplineSelection {
  index: number;
  selected: boolean;
  createIfNew: boolean;
  professorId: number | null;
  name: string;
  existsInSystem: boolean;
  totalHours: number;
  ectsCredits: number;
  semester: number | null;
}

interface PreviewRow {
  fullName: string;
  bookNumberId: number;
  grades: (number | null)[];
}

@Component({
  selector: 'app-import',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './import.component.html',
  styleUrl: './import.component.css',
})
export class ImportComponent implements OnInit {
  private importService = inject(ImportService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private authState = inject(AuthStateService);
  private tokenService = inject(TokenService);

  isProfessor = this.authState.isProfessor;
  currentUserId = this.authState.currentUserId;

  step = signal<Step>('upload');
  professors = signal<User[]>([]);
  selectedFile = signal<File | null>(null);
  isDragging = signal(false);
  parsing = signal(false);
  checkingDisciplines = signal(false);
  creatingDisciplines = signal(false);
  checkingStudents = signal(false);
  importing = signal(false);

  disciplineCheck = signal<DisciplineCheckResult | null>(null);
  disciplineSelections = signal<DisciplineSelection[]>([]);
  studentCheck = signal<StudentCheckResult | null>(null);
  selectedBookNumberIds = signal<Set<number>>(new Set());
  result = signal<ImportResult | null>(null);

  disciplineSearches = signal<Record<number, string>>({});
  showDisciplineDropdown = signal<Record<number, boolean>>({});
  dropdownPositions = signal<Record<number, 'above' | 'below'>>({});

  professorName = computed(() => {
    const user = this.tokenService.currentUser();
    if (!user) return '';
    return [user.lastName, user.firstName].filter(Boolean).join(' ');
  });

  canProceedToUpload = computed(() => !this.parsing());

  canProceedToDisciplines = computed(
    () => this.selectedFile() !== null && !this.parsing()
  );

  selectedDisciplineCount = computed(() =>
    this.disciplineSelections()
      .filter((d) => {
        if (d.existsInSystem) return d.selected;
        return d.createIfNew && d.selected;
      })
      .length
  );

  disciplinesWithMissingProfessor = computed(() =>
    this.disciplineSelections().filter((d) => {
      if (d.existsInSystem) {
        return d.selected && d.professorId === null;
      }
      return d.createIfNew && d.selected && d.professorId === null;
    })
  );

  canProceedToStudents = computed(
    () =>
      this.selectedDisciplineCount() > 0 &&
      this.disciplinesWithMissingProfessor().length === 0
  );

  selectedStudentCount = computed(() => this.selectedBookNumberIds().size);

  canProceedToPreview = computed(() => this.selectedStudentCount() > 0);

  existingDisciplines = computed(() =>
    this.disciplineSelections().filter((d) => d.existsInSystem)
  );

  newDisciplines = computed(() =>
    this.disciplineSelections().filter((d) => !d.existsInSystem)
  );

  selectedNewDisciplines = computed(() =>
    this.disciplineSelections().filter(
      (d) => !d.existsInSystem && d.createIfNew && d.selected
    )
  );

  newDisciplinesWithCreateFlag = computed(() =>
    this.disciplineSelections().filter(
      (d) => !d.existsInSystem && d.createIfNew
    )
  );

  newDisciplinesReadyToCreate = computed(() =>
    this.selectedNewDisciplines().length > 0 &&
    this.selectedNewDisciplines().every((d) => d.professorId !== null)
  );


  existingStudents = computed(() =>
    this.studentCheck()?.students.filter((s) => s.existsInSystem) ?? []
  );

  notFoundStudents = computed(() =>
    this.studentCheck()?.students.filter((s) => !s.existsInSystem) ?? []
  );

  previewTable = computed(() => {
    const studentCheck = this.studentCheck();
    const selections = this.disciplineSelections();
    const selectedBooks = this.selectedBookNumberIds();

    if (!studentCheck) return { columns: [], rows: [], professors: [] };

    const selectedDisciplineIndices = selections
      .filter((d) => d.selected)
      .map((d) => d.index);

    const columns = selections
      .filter((d) => d.selected)
      .map((d) => ({ index: d.index, name: d.name }));

    const professors = selections
      .filter((d) => d.selected)
      .map((d) => this.getProfessorNameById(d.professorId ?? 0));

    const rows: PreviewRow[] = studentCheck.students
      .filter((s) => s.existsInSystem && selectedBooks.has(s.bookNumberId!))
      .map((s) => {
        const grades = selectedDisciplineIndices.map((disciplineIdx) => {
          const gradeData = s.grades.find((g) => g.disciplineIndex === disciplineIdx);
          return gradeData?.universityGrade ?? null;
        });
        return {
          fullName: s.fullName,
          bookNumberId: s.bookNumberId!,
          grades,
        };
      });

    return { columns, rows, professors };
  });

  ngOnInit() {
    if (!this.isProfessor()) {
      this.userService.findAll().subscribe({
        next: (users) =>
          this.professors.set(users.filter((u) => u.role === 'PROFESSOR')),
        error: () => this.toastService.error('Помилка завантаження викладачів'),
      });
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.setFile(input.files[0]);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave() {
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(false);
    const file = event.dataTransfer?.files[0];
    if (file) this.setFile(file);
  }

  private setFile(file: File) {
    if (!file.name.endsWith('.xlsx')) {
      this.toastService.error('Підтримується лише формат .xlsx');
      return;
    }
    this.selectedFile.set(file);
    this.disciplineCheck.set(null);
    this.studentCheck.set(null);
    this.result.set(null);
    this.step.set('upload');
  }

  clearFile() {
    this.selectedFile.set(null);
    this.disciplineCheck.set(null);
    this.disciplineSelections.set([]);
    this.studentCheck.set(null);
    this.selectedBookNumberIds.set(new Set());
    this.result.set(null);
    this.step.set('upload');
  }

  checkDisciplines() {
    const file = this.selectedFile();
    if (!file) return;
    this.checkingDisciplines.set(true);

    this.importService.checkDisciplines(file).subscribe({
      next: (check) => {
        this.disciplineCheck.set(check);
        const defaultProfId = this.isProfessor() ? (this.currentUserId() ?? null) : null;
        this.disciplineSelections.set(
          check.disciplines.map((d) => ({
            index: d.index,
            name: d.name,
            existsInSystem: d.existsInSystem,
            selected: true,
            createIfNew: d.existsInSystem ? false : true,
            professorId: defaultProfId,
            totalHours: d.totalHours,
            ectsCredits: d.ectsCredits,
            semester: d.semester,
          }))
        );
        this.checkingDisciplines.set(false);
        this.step.set('disciplines');
      },
      error: () => {
        this.checkingDisciplines.set(false);
        this.toastService.error('Помилка при перевірці дисциплін');
      },
    });
  }

  proceedToStudents() {
    const file = this.selectedFile();
    const check = this.disciplineCheck();
    if (!file || !check) return;

    const selectionsToCreate = this.disciplineSelections()
      .filter((d) => d.createIfNew && !d.existsInSystem)
      .map((d) => d.index);

    if (selectionsToCreate.length > 0) {
      this.creatingDisciplines.set(true);
      this.importService
        .createDisciplines(
          file,
          selectionsToCreate,
          check.specialtyId!,
          check.academicYear
        )
        .subscribe({
          next: () => {
            this.creatingDisciplines.set(false);
            this.checkStudents();
          },
          error: (err) => {
            this.creatingDisciplines.set(false);
            console.error('Failed to create disciplines:', err);
            this.toastService.error('Помилка при створенні дисциплін');
          },
        });
    } else {
      this.checkStudents();
    }
  }

  createDisciplinesOnly() {
    const file = this.selectedFile();
    const check = this.disciplineCheck();
    if (!file || !check) return;

    const selectionsToCreate = this.newDisciplinesWithCreateFlag().map((d) => d.index);

    if (selectionsToCreate.length === 0) {
      this.toastService.error('Немає дисциплін для створення');
      return;
    }

    this.creatingDisciplines.set(true);
    this.importService
      .createDisciplines(
        file,
        selectionsToCreate,
        check.specialtyId!,
        check.academicYear
      )
      .subscribe({
        next: () => {
          this.creatingDisciplines.set(false);
          this.toastService.success(
            `Успішно створено ${selectionsToCreate.length} дисциплін`
          );
          this.clearFile();
        },
        error: (err) => {
          this.creatingDisciplines.set(false);
          console.error('Failed to create disciplines:', err);
          this.toastService.error('Помилка при створенні дисциплін');
        },
      });
  }

  private checkStudents() {
    const file = this.selectedFile();
    if (!file) return;
    this.checkingStudents.set(true);

    this.importService.checkStudents(file).subscribe({
      next: (check) => {
        this.studentCheck.set(check);
        this.selectedBookNumberIds.set(
          new Set(
            check.students
              .filter((s) => s.existsInSystem)
              .map((s) => s.bookNumberId!)
          )
        );
        this.checkingStudents.set(false);
        this.step.set('students');
      },
      error: () => {
        this.checkingStudents.set(false);
        this.toastService.error('Помилка при перевірці студентів');
      },
    });
  }

  toggleStudentSelection(bookNumberId: number) {
    this.selectedBookNumberIds.update((selected) => {
      const newSet = new Set(selected);
      if (newSet.has(bookNumberId)) {
        newSet.delete(bookNumberId);
      } else {
        newSet.add(bookNumberId);
      }
      return newSet;
    });
  }

  selectAllStudents() {
    const check = this.studentCheck();
    if (!check) return;
    const bookNumbers = check.students
      .filter((s) => s.existsInSystem)
      .map((s) => s.bookNumberId!);
    this.selectedBookNumberIds.set(new Set(bookNumbers));
  }

  deselectAllStudents() {
    this.selectedBookNumberIds.set(new Set());
  }

  proceedToPreview() {
    this.step.set('preview');
  }

  assignDisciplineProfessor(index: number, professorId: number | null) {
    this.disciplineSelections.update((selections) =>
      selections.map((s) => (s.index === index ? { ...s, professorId } : s))
    );
    const professorName = professorId
      ? this.getProfessorNameById(professorId)
      : '';
    this.disciplineSearches.update((searches) => ({
      ...searches,
      [index]: professorName,
    }));
    this.showDisciplineDropdown.update((shows) => ({ ...shows, [index]: false }));
  }

  onDisciplineSearchChange(index: number, search: string) {
    this.disciplineSearches.update((searches) => ({
      ...searches,
      [index]: search,
    }));
    this.showDisciplineDropdown.update((shows) => ({ ...shows, [index]: true }));
  }

  toggleDisciplineDropdown(index: number) {
    const isOpening = !this.showDisciplineDropdown()[index];
    if (isOpening) {
      setTimeout(() => this.calculateDropdownPosition(index), 0);
    }
    this.showDisciplineDropdown.update((shows) => ({
      ...shows,
      [index]: !shows[index],
    }));
  }

  private calculateDropdownPosition(index: number) {
    const element = document.querySelector(`[data-discipline-index="${index}"]`);
    if (!element) return;

    const rect = element.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    const maxDropdownHeight = 200;

    const position = spaceBelow < maxDropdownHeight + 50 ? 'above' : 'below';
    this.dropdownPositions.update((pos) => ({ ...pos, [index]: position }));
  }

  getProfessorName(professor: User): string {
    return `${professor.lastName} ${professor.firstName}`;
  }

  getProfessorNameById(professorId: number): string {
    const professor = this.professors().find((p) => p.id === professorId);
    return professor ? this.getProfessorName(professor) : '';
  }

  getFilteredDisciplineProfessors(search: string) {
    const searchLower = search.toLowerCase().trim();
    if (!searchLower) return this.professors();
    return this.professors().filter((p) =>
      `${p.lastName} ${p.firstName}`.toLowerCase().includes(searchLower)
    );
  }

  closeDisciplineDropdown(index: number) {
    setTimeout(
      () =>
        this.showDisciplineDropdown.update((s) => ({
          ...s,
          [index]: false,
        })),
      150
    );
  }

  import() {
    const file = this.selectedFile();
    if (!file) return;

    const professorMap: Record<number, number> = {};
    for (const d of this.disciplineSelections()) {
      const shouldInclude = d.existsInSystem ? d.selected : d.createIfNew && d.selected;
      if (!shouldInclude) continue;
      const pid = this.isProfessor()
        ? (this.currentUserId() ?? null)
        : d.professorId;
      if (pid !== null) professorMap[d.index] = pid;
    }

    if (Object.keys(professorMap).length === 0) {
      this.toastService.error('Призначте викладача хоча б для однієї дисципліни');
      return;
    }

    const selectedStudentIds = Array.from(this.selectedBookNumberIds());

    this.importing.set(true);
    this.result.set(null);

    this.importService
      .importGradeReport(file, professorMap, selectedStudentIds)
      .subscribe({
        next: (res) => {
          this.result.set(res);
          this.importing.set(false);
          this.step.set('result');
          if (res.errors.length === 0 && res.studentsUnmatched === 0) {
            this.toastService.success(`Імпортовано ${res.gradesCreated} оцінок`);
          } else {
            this.toastService.warning('Імпорт завершено з попередженнями');
          }
        },
        error: () => {
          this.importing.set(false);
          this.toastService.error('Помилка під час імпорту');
        },
      });
  }

  goBack() {
    const currentStep = this.step();
    if (currentStep === 'preview') {
      this.step.set('students');
    } else if (currentStep === 'students') {
      this.step.set('disciplines');
    } else if (currentStep === 'disciplines') {
      this.clearFile();
    } else if (currentStep === 'result') {
      this.clearFile();
    }
  }

  toggleDisciplineSelected(index: number) {
    this.disciplineSelections.update((selections) =>
      selections.map((s) =>
        s.index === index ? { ...s, selected: !s.selected } : s
      )
    );
  }

  toggleDisciplineCreateIfNew(index: number) {
    this.disciplineSelections.update((selections) =>
      selections.map((s) => {
        if (s.index === index && !s.existsInSystem) {
          const newCreateIfNew = !s.createIfNew;
          return {
            ...s,
            createIfNew: newCreateIfNew,
            selected: newCreateIfNew ? s.selected : false,
            professorId: newCreateIfNew ? s.professorId : null,
          };
        }
        return s;
      })
    );
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' Б';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' КБ';
    return (bytes / (1024 * 1024)).toFixed(1) + ' МБ';
  }

  selectAllDisciplines() {
    this.disciplineSelections.update((selections) =>
      selections.map((s) =>
        s.existsInSystem
          ? { ...s, selected: true }
          : s.createIfNew
          ? { ...s, selected: true }
          : s
      )
    );
  }

  deselectAllDisciplines() {
    this.disciplineSelections.update((selections) =>
      selections.map((s) => ({ ...s, selected: false }))
    );
  }

  createAllNewDisciplines() {
    this.disciplineSelections.update((selections) =>
      selections.map((s) =>
        !s.existsInSystem ? { ...s, createIfNew: true } : s
      )
    );
  }

  clearAllNewDisciplines() {
    this.disciplineSelections.update((selections) =>
      selections.map((s) =>
        !s.existsInSystem
          ? { ...s, createIfNew: false, selected: false }
          : s
      )
    );
  }
}
