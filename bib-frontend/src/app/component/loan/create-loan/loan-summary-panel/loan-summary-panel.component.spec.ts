import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';

import { LoanSummaryPanelComponent } from './loan-summary-panel.component';
import { Book, BookCopyLocation } from '../../../../model/book/book.model';

describe('LoanSummaryPanelComponent', () => {
  let component: LoanSummaryPanelComponent;
  let fixture: ComponentFixture<LoanSummaryPanelComponent>;

  const book: Book = {
    id: 1,
    title: 'Test Book',
    author: 'Test Author',
    pages: 100,
    releaseYear: 2026,
    didactic: false,
    copyLocations: [
      {
        campusId: 1,
        campusName: 'Campus 1',
        copies: [
          { id: 10, identifier: '2800000000103', availability: 'AVAILABLE', condition: 'GOOD_CONDITION' },
          { id: 11, identifier: '2800000000110', availability: 'AVAILABLE', condition: 'GOOD_CONDITION' },
        ],
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoanSummaryPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanSummaryPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput(
      'loanForm',
      new FormGroup({ returnDate: new FormControl('2026-06-10') }),
    );
    fixture.componentRef.setInput('minReturnDate', '2026-06-01');
    fixture.componentRef.setInput('selectedBooks', [book, book]);
    fixture.componentRef.setInput('selectedBookCounts', { 1: 2 });
    fixture.componentRef.setInput('selectedCopyIdsByBook', { 1: [10, 11] });
    fixture.componentRef.setInput('hasSelectedUser', true);
    fixture.componentRef.setInput('isSubmitting', false);
    fixture.detectChanges();
  });

  it('Given_CopySelectedInOtherSlot_When_IsCopyDisabled_Then_ReturnsTrue', () => {
    // Arrange
    const copy: BookCopyLocation = {
      id: 10,
      identifier: '2800000000103',
      availability: 'AVAILABLE',
      condition: 'GOOD_CONDITION',
      campusId: 1,
    };

    // Act
    const result = component.isCopyDisabled(book, copy, 1);

    // Assert
    expect(result).toBeTrue();
  });

  it('Given_CopySelectedInCurrentSlot_When_IsCopyDisabled_Then_ReturnsFalse', () => {
    // Arrange
    const copy: BookCopyLocation = {
      id: 10,
      identifier: '2800000000103',
      availability: 'AVAILABLE',
      condition: 'GOOD_CONDITION',
      campusId: 1,
    };

    // Act
    const result = component.isCopyDisabled(book, copy, 0);

    // Assert
    expect(result).toBeFalse();
  });

  it('Given_Copy_When_GetCopyLabel_Then_ReturnsStableCopyIdentifier', () => {
    // Arrange
    const copy: BookCopyLocation = {
      id: 10,
      identifier: '2800000000103',
      availability: 'AVAILABLE',
      condition: 'GOOD_CONDITION',
      campusName: 'Campus 1',
    };

    // Act
    const result = component.getCopyLabel(copy);

    // Assert
    expect(result).toBe('Exemplaar 2800000000103 - Beschikbaar - Goede staat (Campus 1)');
  });

  it('Given_SelectedCopyIdsByBook_When_RenderingCopySelects_Then_EachSlotShowsItsOwnSelectedCopy', () => {
    // Arrange
    fixture.detectChanges();

    // Act
    const selects = fixture.nativeElement.querySelectorAll('select') as NodeListOf<HTMLSelectElement>;

    // Assert
    expect(selects.length).toBe(2);
    expect(selects[0].selectedOptions[0].textContent?.trim()).toBe(
      'Exemplaar 2800000000103 - Beschikbaar - Goede staat (Campus 1)',
    );
    expect(selects[1].selectedOptions[0].textContent?.trim()).toBe(
      'Exemplaar 2800000000110 - Beschikbaar - Goede staat (Campus 1)',
    );
  });
});
