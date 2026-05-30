import { Component, input } from '@angular/core';

@Component({
  selector: 'app-dev-feature-wrapper',
  standalone: true,
  templateUrl: './dev-feature-wrapper.component.html',
  styleUrl: './dev-feature-wrapper.component.css',
})
export class DevFeatureWrapperComponent {
  public readonly compact = input<boolean>(false);
}
