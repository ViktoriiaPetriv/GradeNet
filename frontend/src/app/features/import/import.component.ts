import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ImportService } from '../../core/services/import.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { TokenService } from '../../core/services/token.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ImportResult, ParsedReportMeta } from '../../models/import.model';
import { User } from '../../models/user.model';

type Step = 'upload' | 'assign' | 'result';

interface DisciplineRow {
  index: number;
  name: string;
  professorId: number | null;
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
  importing = signal(false);

  meta = signal<ParsedReportMeta | null>(null);
  disciplines = signal<DisciplineRow[]>([]);
  result = signal<ImportResult | null>(null);

  assignAllSearch = signal('');
  disciplineSearches = signal<Record<number, string>>({});
  showAssignAllDropdown = signal(false);
  showDisciplineDropdown = signal<Record<number, boolean>>({});
  dropdownPositions = signal<Record<number, 'above' | 'below'>>({});

  professorName = computed(() => {
    const user = this.tokenService.currentUser();
    if (!user) return '';
    return [user.lastName, user.firstName].filter(Boolean).join(' ');
  });

  filteredAssignAllProfessors = computed(() => {
    const search = this.assignAllSearch().toLowerCase().trim();
    if (!search) return this.professors();
    return this.professors().filter((p) =>
      `${p.lastName} ${p.firstName}`.toLowerCase().includes(search)
    );
  });

  getFilteredDisciplineProfessors = (search: string) => {
    const searchLower = search.toLowerCase().trim();
    if (!searchLower) return this.professors();
    return this.professors().filter((p) =>
      `${p.lastName} ${p.firstName}`.toLowerCase().includes(searchLower)
    );
  };

  canParse = computed(() => this.selectedFile() !== null && !this.parsing());

  canImport = computed(() => {
    if (this.importing()) return false;
    if (this.isProfessor()) return true;
    return this.disciplines().some((d) => d.professorId !== null);
  });

  assignedCount = computed(() => this.disciplines().filter((d) => d.professorId !== null).length);

  ngOnInit() {
    if (!this.isProfessor()) {
      this.userService.findAll().subscribe({
        next: (users) => this.professors.set(users.filter((u) => u.role === 'PROFESSOR')),
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
    this.meta.set(null);
    this.result.set(null);
    this.step.set('upload');
  }

  clearFile() {
    this.selectedFile.set(null);
    this.meta.set(null);
    this.result.set(null);
    this.step.set('upload');
  }

  parse() {
    const file = this.selectedFile();
    if (!file) return;
    this.parsing.set(true);

    this.importService.parse(file).subscribe({
      next: (meta) => {
        this.meta.set(meta);
        const defaultProfId = this.isProfessor() ? (this.currentUserId() ?? null) : null;
        this.disciplines.set(
          meta.disciplineNames.map((name, index) => ({ index, name, professorId: defaultProfId }))
        );
        this.parsing.set(false);
        this.step.set('assign');
      },
      error: () => {
        this.parsing.set(false);
        this.toastService.error('Не вдалося зчитати файл');
      },
    });
  }

  assignAll(professorId: number | null) {
    this.disciplines.update((rows) => rows.map((r) => ({ ...r, professorId })));
    const professorName = professorId ? this.getProfessorNameById(professorId) : '';
    this.assignAllSearch.set(professorName);
    this.showAssignAllDropdown.set(false);
  }

  assignProfessor(disciplineIndex: number, professorId: number | null) {
    this.disciplines.update((rows) =>
      rows.map((r) => (r.index === disciplineIndex ? { ...r, professorId } : r))
    );
    const professorName = professorId ? this.getProfessorNameById(professorId) : '';
    this.disciplineSearches.update((searches) => ({ ...searches, [disciplineIndex]: professorName }));
    this.showDisciplineDropdown.update((shows) => ({ ...shows, [disciplineIndex]: false }));
  }

  onAssignAllSearchChange(search: string) {
    this.assignAllSearch.set(search);
    this.showAssignAllDropdown.set(true);
  }

  onDisciplineSearchChange(disciplineIndex: number, search: string) {
    this.disciplineSearches.update((searches) => ({ ...searches, [disciplineIndex]: search }));
    this.showDisciplineDropdown.update((shows) => ({ ...shows, [disciplineIndex]: true }));
  }

  toggleAssignAllDropdown() {
    this.showAssignAllDropdown.update((val) => !val);
  }

  toggleDisciplineDropdown(disciplineIndex: number) {
    const isOpening = !this.showDisciplineDropdown()[disciplineIndex];
    if (isOpening) {
      setTimeout(() => this.calculateDropdownPosition(disciplineIndex), 0);
    }
    this.showDisciplineDropdown.update((shows) => ({
      ...shows,
      [disciplineIndex]: !shows[disciplineIndex],
    }));
  }

  private calculateDropdownPosition(disciplineIndex: number) {
    const element = document.querySelector(`[data-discipline-index="${disciplineIndex}"]`);
    if (!element) return;

    const rect = element.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    const maxDropdownHeight = 200;

    const position = spaceBelow < maxDropdownHeight + 50 ? 'above' : 'below';
    this.dropdownPositions.update((pos) => ({ ...pos, [disciplineIndex]: position }));
  }

  getProfessorName(professor: User): string {
    return `${professor.lastName} ${professor.firstName}`;
  }

  getProfessorNameById(professorId: number): string {
    const professor = this.professors().find((p) => p.id === professorId);
    return professor ? this.getProfessorName(professor) : '';
  }

  closeAssignAllDropdown() {
    setTimeout(() => this.showAssignAllDropdown.set(false), 150);
  }

  closeDisciplineDropdown(disciplineIndex: number) {
    setTimeout(() => this.showDisciplineDropdown.update((s) => ({ ...s, [disciplineIndex]: false })), 150);
  }

  import() {
    const file = this.selectedFile();
    if (!file) return;

    const professorMap: Record<number, number> = {};
    for (const d of this.disciplines()) {
      const pid = this.isProfessor() ? (this.currentUserId() ?? null) : d.professorId;
      if (pid !== null) professorMap[d.index] = pid;
    }

    if (Object.keys(professorMap).length === 0) {
      this.toastService.error('Призначте викладача хоча б для однієї дисципліни');
      return;
    }

    this.importing.set(true);
    this.result.set(null);

    this.importService.importGradeReport(file, professorMap).subscribe({
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
    this.step.set('upload');
    this.result.set(null);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' Б';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' КБ';
    return (bytes / (1024 * 1024)).toFixed(1) + ' МБ';
  }
}
