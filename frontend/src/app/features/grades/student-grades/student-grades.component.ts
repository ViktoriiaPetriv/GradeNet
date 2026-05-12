import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { BookService } from '../../../core/services/book.service';
import { StudentDisciplineDTO, GradeDTO } from '../../../models/grade.model';
import { BookNumber } from '../../../models/book.model';

@Component({
  selector: 'app-student-grades',
  standalone: true,
  imports: [],
  templateUrl: './student-grades.component.html',
  styleUrl: './student-grades.component.css',
})
export class StudentGradesComponent implements OnInit {
  bookNumberId = signal(0);
  book = signal<BookNumber | null>(null);
  disciplines = signal<StudentDisciplineDTO[]>([]);
  selectedYears = signal<Set<string>>(new Set());

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gradeService = inject(GradeService);
  private bookService = inject(BookService);

  allYears = computed(() => {
    const years = new Set(this.disciplines().map((d) => d.academicYear));
    return [...years].sort().reverse();
  });

  filteredDisciplines = computed(() => {
    const selected = this.selectedYears();
    if (selected.size === 0) return this.disciplines();
    return this.disciplines().filter((d) => selected.has(d.academicYear));
  });

  studentName = computed(() => {
    const b = this.book();
    if (!b) return '';
    return [b.studentLastName, b.studentFirstName].filter(Boolean).join(' ');
  });

  passedCount = computed(() => this.filteredDisciplines().filter((d) => d.result === 'PASSED').length);
  failedCount = computed(() => this.filteredDisciplines().filter((d) => d.result === 'FAILED').length);
  inProgressCount = computed(() => this.filteredDisciplines().filter((d) => d.status === 'IN_PROGRESS').length);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('bookNumberId'));
    this.bookNumberId.set(id);
    this.bookService.findById(id).subscribe((book) => this.book.set(book));
    this.gradeService.getStudentDisciplines(id).subscribe((d) => this.disciplines.set(d));
  }

  toggleYear(year: string) {
    const years = new Set(this.selectedYears());
    if (years.has(year)) {
      years.delete(year);
    } else {
      years.add(year);
    }
    this.selectedYears.set(years);
  }

  clearYears() {
    this.selectedYears.set(new Set());
  }

  isYearSelected(year: string): boolean {
    return this.selectedYears().size === 0 || this.selectedYears().has(year);
  }

  viewEntry(entryId: number, event: Event) {
    event.stopPropagation();
    this.router.navigate(['/grades', entryId]);
  }

  goBack() {
    this.router.navigate(['/books', this.bookNumberId()]);
  }

  openReport() {
    this.router.navigate(['/grades/student', this.bookNumberId(), 'report']);
  }

  statusLabel(s: string): string {
    return s === 'IN_PROGRESS' ? 'Відкрито' : 'Закрито';
  }

  resultLabel(r: string | null): string {
    if (!r) return '—';
    return r === 'PASSED' ? 'Зараховано' : 'Не зараховано';
  }

  latestGrade(d: StudentDisciplineDTO): GradeDTO | null {
    if (!d.grades?.length) return null;
    return d.grades[d.grades.length - 1];
  }

  nationalLabel(grade: string | undefined): string {
    if (!grade) return '—';
    const map: Record<string, string> = {
      FIVE: 'Відмінно',
      FOUR: 'Добре',
      THREE: 'Задовільно',
      TWO: 'Незадовільно',
      PASSED: 'Зараховано',
      NOT_PASSED: 'Не зараховано',
    };
    return map[grade] ?? grade;
  }

  ectsCredits(d: StudentDisciplineDTO): number | null {
    const h = d.hours.find((hr) => hr.academicYear === d.academicYear);
    return h?.ectsCredits ?? null;
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  rowClass(d: StudentDisciplineDTO): string {
    if (d.result === 'PASSED') return 'row-passed';
    if (d.result === 'FAILED') return 'row-failed';
    return '';
  }
}
