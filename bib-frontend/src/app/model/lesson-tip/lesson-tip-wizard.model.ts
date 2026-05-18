export type LessonTipWizardStep = 1 | 2 | 3 | 4;

export interface LessonTipWizardStepDefinition {
  readonly step: LessonTipWizardStep;
  readonly title: string;
  readonly description: string;
}

export const LESSON_TIP_CREATE_WIZARD_STEPS: readonly LessonTipWizardStepDefinition[] =
  [
    {
      step: 1,
      title: 'Basisgegevens',
      description:
        'Vul titel, categorie, beschrijving, vak, niveau en trefwoorden in.',
    },
    {
      step: 2,
      title: 'Gekoppelde boeken',
      description: 'Zoek boeken op titel, auteur of ISBN en koppel ze optioneel.',
    },
    {
      step: 3,
      title: 'Bijlagen',
      description: 'Voeg optioneel bestanden toe aan de wachtrij.',
    },
    {
      step: 4,
      title: 'Controle en opslaan',
      description: 'Controleer alles en sla de lestip definitief op.',
    },
  ];
