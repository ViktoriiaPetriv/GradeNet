import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-avatar',
  standalone: true,
  templateUrl: './avatar.component.html',
  styleUrls: ['./avatar.component.css']
})
export class AvatarComponent {
  @Input() letter: string = '?';
  @Input() backgroundColor: string = '#5B6AF0';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
}
