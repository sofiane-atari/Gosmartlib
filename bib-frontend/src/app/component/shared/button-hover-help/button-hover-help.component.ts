import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-button-hover-help',
  standalone: true,
  templateUrl: './button-hover-help.component.html',
  styleUrls: ['./button-hover-help.component.css']
})
export class ButtonHoverHelpComponent {
  @Input() text: string = '';
}