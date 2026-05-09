import { Component, Input } from '@angular/core';
import { AvatarComponent } from '../avatar/avatar.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-hero-card',
  standalone: true,
  imports: [AvatarComponent, CommonModule],
  templateUrl: './hero-card.component.html',
  styleUrls: ['./hero-card.component.css']
})
export class HeroCardComponent {
  @Input() avatarLetter: string = '?';
  @Input() avatarColor: string = '#5B6AF0';
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() topLine: string = ''; // для бейджа типу або коду
}
