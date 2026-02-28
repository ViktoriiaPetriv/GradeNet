import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.css',
})
export class PaginationComponent {
  currentPage = input.required<number>();
  totalPages = input.required<number>();
  perPage = input<number>(10);
  perPageOptions = input<number[]>([1, 5, 10, 25, 50]);
  info = input<string>('');

  pageChange = output<number>();
  perPageChange = output<number>();

  pages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPage();

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

  setPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.pageChange.emit(p);
  }

  onPerPageChange(e: Event) {
    this.perPageChange.emit(Number((e.target as HTMLSelectElement).value));
  }
}
