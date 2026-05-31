import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';
import { CreateReadingListDTO, ReadingListTargetType, TeacherAudienceOptions } from '../../../model/readingList.model';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

interface ScopeOption {
  value: ReadingListTargetType;
  label: string;
  description: string;
}

@Component({
  selector: 'app-create-teacher-reading-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './create-teacher-reading-list.component.html',
  styleUrls: [
    '../create-reading-list/create-reading-list.component.css',
    './create-teacher-reading-list.component.css'
  ],
  animations: [fadeInUpAnimation],
})
export class CreateTeacherReadingListComponent implements OnInit {
  listName = '';
  selectedScope: ReadingListTargetType = 'PERSONAL';
  targetValue = '';

  isLoadingAudienceOptions = false;
  audienceOptionsError = '';

  private audienceOptions: TeacherAudienceOptions = {
    classes: [],
    years: [],
    schools: []
  };

  readonly scopeOptions: ScopeOption[] = [
    {
      value: 'PERSONAL',
      label: 'Persoonlijk',
      description: 'Alleen zichtbaar voor jou.'
    },
    {
      value: 'SYSTEM',
      label: 'Uitgelicht',
      description: 'Zichtbaar voor alle gebruikers als uitgelichte lijst.'
    },
    {
      value: 'CLASS',
      label: 'Klassikale lijst',
      description: 'Geldt voor jouw klasgroep.'
    },
  ];

  constructor(
    private readonly readingListService: ReadingListService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadAudienceOptions();
  }

  get selectedScopeDescription(): string {
    return (
      this.scopeOptions.find(option => option.value === this.selectedScope)
        ?.description ?? ''
    );
  }

  get requiresTargetValue(): boolean {
    return ['CLASS'].includes(this.selectedScope);
  }

  get targetValueLabel(): string {
    return {
      CLASS: 'Klas',
      YEAR: 'Jaar',
      SCHOOL: 'School'
    }[this.selectedScope as 'CLASS'] ?? 'Doelwaarde';
  }

  get targetValuePlaceholder(): string {
    return {
      CLASS: 'Bijv. 3A',
      YEAR: 'Bijv. 2',
      SCHOOL: 'Bijv. De Stadsschool'
    }[this.selectedScope as 'CLASS'] ?? '';
  }

  get currentTargetOptions(): string[] {
    if (this.selectedScope === 'CLASS') {
      return this.audienceOptions.classes;
    }
    return [];
  }

  get hasTargetOptions(): boolean {
    return this.currentTargetOptions.length > 0;
  }

  get usesTargetSelect(): boolean {
    return this.requiresTargetValue && !this.isLoadingAudienceOptions && this.hasTargetOptions;
  }

  onScopeChange(): void {
    this.targetValue = '';

    if (this.usesTargetSelect) {
      this.targetValue = this.currentTargetOptions[0];
    }
  }

  submit(): void {
    const trimmedName = this.listName.trim();
    if (!trimmedName) {
      return;
    }

    const trimmedTargetValue = this.targetValue.trim();
    if (this.requiresTargetValue && !trimmedTargetValue) {
      return;
    }

    const dto: CreateReadingListDTO = {
      name: trimmedName,
      targetType: this.selectedScope,
      targetValue: this.requiresTargetValue ? trimmedTargetValue : null
    };

    this.readingListService.createList(dto).subscribe({
      next: () => {
        this.router.navigate(['/reading-list']);
      },
      error: (err) => {
        console.error('Fout bij maken van leeslijst:', err);
      }
    });
  }

  private loadAudienceOptions(): void {
    this.isLoadingAudienceOptions = true;
    this.audienceOptionsError = '';

    this.readingListService.getTeacherAudienceOptions().subscribe({
      next: (options) => {
        this.audienceOptions = {
          classes: options.classes ?? [],
          years: options.years ?? [],
          schools: options.schools ?? []
        };
        this.isLoadingAudienceOptions = false;
        this.onScopeChange();
      },
      error: () => {
        this.isLoadingAudienceOptions = false;
        this.audienceOptionsError = 'Kon doelgroepkeuzes niet ophalen. Je kan nog manueel invullen.';
      }
    });
  }
}
