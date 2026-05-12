import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { BookService } from '../../../core/services/book.service';
import { StudentDisciplineDTO, GradeDTO } from '../../../models/grade.model';
import { BookNumber } from '../../../models/book.model';
import { DatePipe, NgFor, NgIf } from '@angular/common';

interface SemesterGroup {
  semester: number | null;
  disciplines: StudentDisciplineDTO[];
}

@Component({
  selector: 'app-student-grade-report',
  standalone: true,
  imports: [DatePipe, NgFor, NgIf],
  templateUrl: './student-grade-report.component.html',
  styleUrl: './student-grade-report.component.css',
})
export class StudentGradeReportComponent implements OnInit {
  book = signal<BookNumber | null>(null);
  disciplines = signal<StudentDisciplineDTO[]>([]);

  private route = inject(ActivatedRoute);
  private gradeService = inject(GradeService);
  private bookService = inject(BookService);

  studentName = computed(() => {
    const b = this.book();
    if (!b) return '';
    return [b.studentLastName, b.studentFirstName].filter(Boolean).join(' ');
  });

  semesterGroups = computed<SemesterGroup[]>(() => {
    const map = new Map<number | null, StudentDisciplineDTO[]>();
    for (const d of this.disciplines()) {
      const key = d.semester;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(d);
    }
    return [...map.entries()]
      .sort(([a], [b]) => (a ?? 999) - (b ?? 999))
      .map(([semester, disciplines]) => ({ semester, disciplines }));
  });

  today = new Date();

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('bookNumberId'));
    this.bookService.findById(id).subscribe((book) => this.book.set(book));
    this.gradeService.getStudentDisciplines(id).subscribe((d) =>
      this.disciplines.set([...d].sort((a, b) => (a.semester ?? 999) - (b.semester ?? 999)))
    );
  }

  print() {
    window.print();
  }

  latestGrade(d: StudentDisciplineDTO): GradeDTO | null {
    if (!d.grades?.length) return null;
    return d.grades[d.grades.length - 1];
  }

  ectsCredits(d: StudentDisciplineDTO): number | null {
    const h = d.hours.find((hr) => hr.academicYear === d.academicYear);
    return h?.ectsCredits ?? null;
  }

  totalHours(d: StudentDisciplineDTO): number | null {
    const h = d.hours.find((hr) => hr.academicYear === d.academicYear);
    return h?.totalHours ?? null;
  }

  formatProfessor(name: string | null): string {
    if (!name) return '—';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0];
    const [last, ...rest] = parts;
    const initials = rest.map((p) => p[0].toUpperCase() + '.').join('');
    return `${last} ${initials}`;
  }

  assessmentLabel(type: string | undefined): string {
    if (!type) return '—';
    return type === 'EXAM' ? 'Екзамен' : 'Залік';
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

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  resultLabel(r: string | null): string {
    if (!r) return '—';
    return r === 'PASSED' ? 'Зараховано' : 'Не зараховано';
  }
}
