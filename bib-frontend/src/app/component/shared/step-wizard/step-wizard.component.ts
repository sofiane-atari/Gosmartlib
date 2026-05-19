import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import {
  WizardStepDefinition,
  WizardStepId,
} from '../../../model/shared/step-wizard/step-wizard.odel';

@Component({
  selector: 'app-step-wizard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './step-wizard.component.html',
  styleUrl: './step-wizard.component.css',
})
export class StepWizardComponent {
  public readonly steps = input.required<readonly WizardStepDefinition[]>();
  public readonly currentStep = input.required<WizardStepId>();
  public readonly canNavigateToStep = input<(step: WizardStepId) => boolean>(
    () => true,
  );

  public readonly stepSelected = output<WizardStepId>();

  protected getStepItemClass(step: WizardStepId): string {
    if (step === this.currentStep()) {
      return 'step-item step-item--active';
    }

    if (step < this.currentStep()) {
      return 'step-item step-item--completed';
    }

    return 'step-item step-item--upcoming';
  }

  protected selectStep(step: WizardStepId): void {
    if (!this.canNavigateToStep()(step)) {
      return;
    }

    this.stepSelected.emit(step);
  }
}
