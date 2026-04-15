import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  CreateReadingListDTO,
  ReadingListService,
  ReadingListTargetType
} from '../../../services/reading-list/reading-list.service';

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
  ]
})
export class CreateTeacherReadingListComponent {
  listName = '';
  selectedScope: ReadingListTargetType = 'PERSONAL';
  targetValue = '';

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
    {
      value: 'YEAR',
      label: 'Jaarleeslijst',
      description: 'Geldt voor het schooljaar van jouw groep.'
    },
    {
      value: 'SCHOOL',
      label: 'Schoolleeslijst',
      description: 'Zichtbaar voor de volledige school.'
    }
  ];

  constructor(
    private readonly readingListService: ReadingListService,
    private readonly router: Router
  ) {}

  get selectedScopeDescription(): string {
    return (
      this.scopeOptions.find(option => option.value === this.selectedScope)
        ?.description ?? ''
    );
  }

  get requiresTargetValue(): boolean {
    return ['CLASS', 'YEAR', 'SCHOOL'].includes(this.selectedScope);
  }

  get targetValueLabel(): string {
    return {
      CLASS: 'Klas',
      YEAR: 'Jaar',
      SCHOOL: 'School'
    }[this.selectedScope as 'CLASS' | 'YEAR' | 'SCHOOL'] ?? 'Doelwaarde';
  }

  get targetValuePlaceholder(): string {
    return {
      CLASS: 'Bijv. 3A',
      YEAR: 'Bijv. 2',
      SCHOOL: 'Bijv. De Stadsschool'
    }[this.selectedScope as 'CLASS' | 'YEAR' | 'SCHOOL'] ?? '';
  }

  submit(): void {
    const trimmedName = this.listName.trim();
    if (!trimmedName) {
      return;
    }

    const dto: CreateReadingListDTO = {
      name: trimmedName,
      targetType: this.selectedScope,
      targetValue: this.requiresTargetValue ? this.targetValue.trim() : null
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
}
