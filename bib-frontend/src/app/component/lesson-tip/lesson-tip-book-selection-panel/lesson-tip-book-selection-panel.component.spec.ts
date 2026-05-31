import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonTipBookSelectionPanelComponent } from './lesson-tip-book-selection-panel.component';

describe('LessonTipBookSelectionPanelComponent', () => {
  let component: LessonTipBookSelectionPanelComponent;
  let fixture: ComponentFixture<LessonTipBookSelectionPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonTipBookSelectionPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonTipBookSelectionPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('searchQuery', 'test');
    fixture.componentRef.setInput('searchIsbn', '');
    fixture.componentRef.setInput('isLoadingBooks', false);
    fixture.componentRef.setInput('hasBookSearchInput', true);
    fixture.componentRef.setInput('filteredBooks', []);
    fixture.componentRef.setInput('selectedBooksCount', 0);
    fixture.componentRef.setInput('selectedBookIds', []);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('Given_SelectedCopies_When_RenderingBook_Then_ShowsRemoveButton', () => {
    // Arrange
    fixture.componentRef.setInput('allowMultipleSelection', true);
    fixture.componentRef.setInput('filteredBooks', [
      {
        id: 1,
        title: 'Test Book',
        author: 'Test Author',
        isbn13: '9781234567890',
        releaseYear: null,
        availableCopies: 2,
        selectedCount: 1,
      },
    ]);

    // Act
    fixture.detectChanges();

    // Assert
    const removeButton = fixture.nativeElement.querySelector(
      '.quantity-button--minus',
    ) as HTMLButtonElement | null;
    expect(removeButton).not.toBeNull();
  });

  it('Given_RemoveClicked_When_SelectedCopiesExist_Then_EmitsRemoveBookRequested', () => {
    // Arrange
    const emitSpy = spyOn(component.removeBookRequested, 'emit');
    const book = {
      id: 1,
      title: 'Test Book',
      author: 'Test Author',
      isbn13: '9781234567890',
      releaseYear: null,
      availableCopies: 2,
      selectedCount: 1,
    };
    fixture.componentRef.setInput('allowMultipleSelection', true);
    fixture.componentRef.setInput('filteredBooks', [book]);
    fixture.detectChanges();

    // Act
    const removeButton = fixture.nativeElement.querySelector(
      '.quantity-button--minus',
    ) as HTMLButtonElement;
    removeButton.click();

    // Assert
    expect(emitSpy).toHaveBeenCalledWith(book);
  });
});
