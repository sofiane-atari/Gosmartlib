export type WizardStepId = number;

export interface WizardStepDefinition {
  readonly step: WizardStepId;
  readonly title: string;
  readonly description: string;
}
