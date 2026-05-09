import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-info-card',
  standalone: true,
  templateUrl: './info-card.component.html',
  styleUrls: ['./info-card.component.css']
})
export class InfoCardComponent {
  @Input() title: string = '';
  @Input() icon: string = ''; // клас іконки, наприклад 'pi pi-info-circle'
  @Input() count: number | null = null; // для бейджа кількості
}
