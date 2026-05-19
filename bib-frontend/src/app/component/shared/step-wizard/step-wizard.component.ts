import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import {
  WizardStepDefinition,
  WizardStepId,
} from '../../../model/shared/step-wizard/step-wizard.model';

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
  public readonly maxReachableStep = input<WizardStepId>(1);
  public readonly allowStepClick = input<boolean>(true);

  public readonly stepSelected = output<WizardStepId>();

  protected readonly normalizedMaxReachableStep = computed(() => {
    const steps = this.steps();

    if (steps.length === 0) {
      return 1;
    }

    const highestStep = Math.max(...steps.map((step) => step.step));
    return Math.min(this.maxReachableStep(), highestStep);
  });

  protected getStepItemClass(step: WizardStepId): string {
    if (step === this.currentStep()) {
      return 'step-item step-item--active';
    }

    if (step < this.currentStep()) {
      return 'step-item step-item--completed';
    }

    return 'step-item step-item--upcoming';
  }

  protected isStepDisabled(step: WizardStepId): boolean {
    return !this.allowStepClick() || step > this.normalizedMaxReachableStep();
  }

  protected selectStep(step: WizardStepId): void {
    if (this.isStepDisabled(step)) {
      return;
    }

    this.stepSelected.emit(step);
  }
}
