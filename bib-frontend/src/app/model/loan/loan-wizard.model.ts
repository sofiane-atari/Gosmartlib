import { WizardStepDefinition } from '../shared/step-wizard/step-wizard.model';

export type CreateLoanWizardStep = 1 | 2 | 3;

export type CreateLoanWizardStepDefinition = WizardStepDefinition & {
  readonly step: CreateLoanWizardStep;
};

export const CREATE_LOAN_WIZARD_STEPS: readonly CreateLoanWizardStepDefinition[] =
  [
    {
      step: 1,
      title: 'Gebruiker kiezen',
      description: 'Zoek en selecteer de gebruiker voor deze lening.',
    },
    {
      step: 2,
      title: 'Boeken kiezen',
      description: 'Zoek of scan boeken en voeg ze toe aan de lening.',
    },
    {
      step: 3,
      title: 'Controleren en aanmaken',
      description: 'Controleer de selectie, exemplaren en terugbrengdatum.',
    },
  ];
