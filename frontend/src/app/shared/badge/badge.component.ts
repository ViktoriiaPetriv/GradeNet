import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';

@Component({
  selector: 'app-badge',
  standalone: true,
  templateUrl: './badge.component.html',
  imports: [
    NgClass
  ],
  styleUrls: ['./badge.component.css']
})
export class BadgeComponent {
  @Input() type: string = ''; // e.g., 'role-ADMIN', 'degree-BACHELOR', 'type-FACULTY', 'status-REGISTERED'
  @Input() variant: 'badge' | 'status' | 'degree' | 'type' = 'badge';
}
