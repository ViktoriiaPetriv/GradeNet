import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { BookService } from '../../../core/services/book.service';
import { AdditionalWorkService } from '../../../core/services/additional-work.service';
import { CommissionService } from '../../../core/services/commission.service';
import { UserService } from '../../../core/services/user.service';
import { StudentDisciplineDTO, GradeDTO } from '../../../models/grade.model';
import { BookNumber } from '../../../models/book.model';
import { AdditionalWork, WorkType } from '../../../models/additional-work.model';
import { Commission } from '../../../models/commission.model';
import { User } from '../../../models/user.model';
import { nationalGradeLabel, workNationalGradeLabel } from '../../../shared/grade-labels';
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
  additionalWorks = signal<AdditionalWork[]>([]);
  commissions = signal<Commission[]>([]);
  professors = signal<User[]>([]);

  private route = inject(ActivatedRoute);
  private gradeService = inject(GradeService);
  private bookService = inject(BookService);
  private additionalWorkService = inject(AdditionalWorkService);
  private commissionService = inject(CommissionService);
  private userService = inject(UserService);

  studentName = computed(() => {
    const b = this.book();
    if (!b) return '';
    return [b.studentLastName, b.studentFirstName].filter(Boolean).join(' ');
  });

  semesterGroups = computed<SemesterGroup[]>(() => {
    const map = new Map<number | null, StudentDisciplineDTO[]>();
    for (const d of this.disciplines()) {
      if (!map.has(d.semester)) map.set(d.semester, []);
      map.get(d.semester)!.push(d);
    }
    return [...map.entries()]
      .sort(([a], [b]) => (a ?? 999) - (b ?? 999))
      .map(([semester, disciplines]) => ({ semester, disciplines }));
  });

  courseWorks    = computed(() => this.additionalWorks().filter(w => w.type === 'COURSE_WORK'));
  practices      = computed(() => this.additionalWorks().filter(w => w.type === 'EDUCATIONAL_PRACTICE' || w.type === 'PRODUCTION_PRACTICE'));
  qualifications     = computed(() => this.additionalWorks().filter(w => w.type === 'QUALIFICATION'));
  comprehensiveExams = computed(() => this.additionalWorks().filter(w => w.type === 'COMPREHENSIVE_EXAM'));

  today = new Date();

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('bookNumberId'));
    this.bookService.findById(id).subscribe((book) => this.book.set(book));
    this.gradeService.getStudentDisciplines(id).subscribe((d) =>
      this.disciplines.set([...d].sort((a, b) => (a.semester ?? 999) - (b.semester ?? 999)))
    );
    this.additionalWorkService.getByBookNumberId(id).subscribe((w) => this.additionalWorks.set(w));
    this.commissionService.getAll().subscribe((c) => this.commissions.set(c));
    this.userService.getProfessors().subscribe((p) => this.professors.set(p));
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

  nationalLabel = nationalGradeLabel;

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }

  resultLabel(r: string | null): string {
    if (!r) return '—';
    return r === 'PASSED' ? 'Зараховано' : 'Не зараховано';
  }

  workTypeLabel(type: WorkType): string {
    const map: Record<WorkType, string> = { COURSE_WORK: 'Курсова робота', EDUCATIONAL_PRACTICE: 'Навчальна практика', PRODUCTION_PRACTICE: 'Виробнича практика', QUALIFICATION: 'Кваліфікаційна робота', COMPREHENSIVE_EXAM: 'Комплексний екзамен' };
    return map[type] ?? type;
  }

  workAssessmentLabel(type: WorkType): string {
    const map: Record<WorkType, string> = { COURSE_WORK: 'Курсова', EDUCATIONAL_PRACTICE: 'Практика', PRODUCTION_PRACTICE: 'Практика', QUALIFICATION: 'Випускна кваліфікаційна робота (проєкт)', COMPREHENSIVE_EXAM: 'Комплексний екзамен' };
    return map[type] ?? type;
  }

  workNationalLabel = workNationalGradeLabel;

  workTotalHours(w: AdditionalWork): number | string {
    return w.courseWorkDetails?.totalHours ?? w.practiceDetails?.totalHours ?? '—';
  }

  commissionMembersText(w: AdditionalWork): string {
    const commission = this.commissions().find(c => c.id === w.commissionId);
    if (!commission?.members?.length) return '—';
    return commission.members
      .map(m => {
        const p = this.professors().find(p => p.id === m.professorId);
        if (!p) return `ID:${m.professorId}`;
        return this.formatProfessor(`${p.lastName} ${p.firstName}${p.patronymic ? ' ' + p.patronymic : ''}`);
      })
      .join(', ');
  }

  workRowClass(w: AdditionalWork): string {
    const state = w.courseWorkDetails?.state ?? w.qualificationDetails?.state;
    if (state === 'COMPLETED') return 'row-passed';
    if (state === 'FAILED') return 'row-failed';
    return '';
  }
}
