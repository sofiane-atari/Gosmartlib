import { Component, input, output, signal } from '@angular/core';

import { BadgeProgressCardComponent } from '../badge-progress-card/badge-progress-card.component';
import {
  GamificationBadgeCategory,
  GamificationBadgeRarity,
  GamificationProgress,
} from '../../../../model/gamification/gamification.model';
import { GamificationBadgeFilters } from '../../../../services/gamification/gamification.service';
import { BadgeCollectionCardComponent } from '../badge-collection-card/badge-collection-card.component';
import { ButtonHoverHelpComponent } from '../../../shared/button-hover-help/button-hover-help.component';

type BadgeStatusFilter = 'ALL' | 'COMPLETED' | 'LOCKED';
type BadgeCategoryFilter = 'ALL' | GamificationBadgeCategory;
type BadgeRarityFilter = 'ALL' | GamificationBadgeRarity;

interface BadgeFilterOption<TValue> {
  readonly label: string;
  readonly value: TValue;
}

@Component({
  selector: 'app-badge-collection',
  standalone: true,
  imports: [
    BadgeProgressCardComponent,
    BadgeCollectionCardComponent,
    ButtonHoverHelpComponent,
  ],
  templateUrl: './badge-collection.component.html',
  styleUrl: './badge-collection.component.css',
})
export class BadgeCollectionComponent {
  public readonly badges = input.required<GamificationProgress[]>();
  public readonly isLoading = input<boolean>(false);
  public readonly errorMessage = input<string | null>(null);

  public readonly filtersChanged = output<GamificationBadgeFilters>();

  protected readonly selectedCategory = signal<BadgeCategoryFilter>('ALL');
  protected readonly selectedRarity = signal<BadgeRarityFilter>('ALL');
  protected readonly selectedStatus = signal<BadgeStatusFilter>('ALL');

  protected readonly categoryOptions: BadgeFilterOption<BadgeCategoryFilter>[] =
    [
      { label: 'Alles', value: 'ALL' },
      { label: 'Lezen', value: 'READING' },
      { label: 'Reviews', value: 'REVIEWING' },
      { label: 'Stiptheid', value: 'CONSISTENCY' },
      { label: 'Ontdekken', value: 'EXPLORATION' },
    ];

  protected readonly rarityOptions: BadgeFilterOption<BadgeRarityFilter>[] = [
    { label: 'Alle niveaus', value: 'ALL' },
    { label: 'Gewoon', value: 'COMMON' },
    { label: 'Zeldzaam', value: 'RARE' },
    { label: 'Episch', value: 'EPIC' },
    { label: 'Legendarisch', value: 'LEGENDARY' },
  ];

  protected readonly statusOptions: BadgeFilterOption<BadgeStatusFilter>[] = [
    { label: 'Alle badges', value: 'ALL' },
    { label: 'Behaald', value: 'COMPLETED' },
    { label: 'Nog te behalen', value: 'LOCKED' },
  ];

  protected selectCategory(category: BadgeCategoryFilter): void {
    this.selectedCategory.set(category);
    this.emitFilters();
  }

  protected selectRarity(rarity: BadgeRarityFilter): void {
    this.selectedRarity.set(rarity);
    this.emitFilters();
  }

  protected selectStatus(status: BadgeStatusFilter): void {
    this.selectedStatus.set(status);
    this.emitFilters();
  }

  private emitFilters(): void {
    const selectedCategory = this.selectedCategory();
    const selectedRarity = this.selectedRarity();

    this.filtersChanged.emit({
      category: selectedCategory === 'ALL' ? undefined : selectedCategory,
      rarity: selectedRarity === 'ALL' ? undefined : selectedRarity,
      completed: this.getCompletedFilter(),
    });
  }

  private getCompletedFilter(): boolean | undefined {
    if (this.selectedStatus() === 'COMPLETED') {
      return true;
    }

    if (this.selectedStatus() === 'LOCKED') {
      return false;
    }

    return undefined;
  }

  protected resetFilters(): void {
    this.selectedCategory.set('ALL');
    this.selectedRarity.set('ALL');
    this.selectedStatus.set('ALL');

    this.emitFilters();
  }
}
