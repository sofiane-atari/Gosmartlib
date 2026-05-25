import { WizardStepDefinition } from '../shared/step-wizard/step-wizard.model';

export type CreateSchoolWizardStep = 1 | 2 | 3 | 4;

export type CreateSchoolWizardStepDefinition = WizardStepDefinition & {
  readonly step: CreateSchoolWizardStep;
};

export const CREATE_SCHOOL_WIZARD_STEPS: readonly CreateSchoolWizardStepDefinition[] =
  [
    {
      step: 1,
      title: 'Schoolgegevens',
      description: 'Vul de naam en het domein van de school in.',
    },
    {
      step: 2,
      title: 'OneRoster-integratie',
      description:
        'Voer de OneRoster-credentials in voor synchronisatie van leerlingen en leerkrachten.',
    },
    {
      step: 3,
      title: 'Campussen',
      description:
        'Configureer de campussen van deze school.',
    },
    {
      step: 4,
      title: 'Controleren en aanmaken',
      description:
        'Controleer alle gegevens en maak de school definitief aan.',
    },
  ];
