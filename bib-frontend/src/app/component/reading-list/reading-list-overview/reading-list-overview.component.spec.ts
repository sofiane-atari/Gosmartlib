import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ReadingListOverviewComponent } from './reading-list-overview.component';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';
import { ToastService } from '../../../services/toast/toast.service';

describe('ReadingListOverviewComponent', () => {
  let component: ReadingListOverviewComponent;
  let fixture: ComponentFixture<ReadingListOverviewComponent>;
  let readingListServiceSpy: jasmine.SpyObj<ReadingListService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    readingListServiceSpy = jasmine.createSpyObj<ReadingListService>('ReadingListService', ['getLists', 'deleteList']);
    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['show', 'showConfirm']);

    readingListServiceSpy.getLists.and.returnValue(of([]));
    readingListServiceSpy.deleteList.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [ReadingListOverviewComponent],
      providers: [
        { provide: ReadingListService, useValue: readingListServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ReadingListOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should hide delete button for Favorieten list', () => {
    component.readingLists = [
      { id: 1, name: 'Favorieten' },
      { id: 2, name: 'Zomerboeken' }
    ];

    fixture.detectChanges();

    const deleteButtons = fixture.nativeElement.querySelectorAll('.btn-delete');
    expect(deleteButtons.length).toBe(1);
  });

  it('should not call delete API when trying to delete Favorieten', () => {
    const favoritesList = { id: 1, name: 'Favorieten' };

    component.deleteList(favoritesList);

    expect(readingListServiceSpy.deleteList).not.toHaveBeenCalled();
    expect(toastServiceSpy.show).toHaveBeenCalledWith("De leeslijst 'Favorieten' kan niet verwijderd worden", 'error');
  });

  it('should ask confirmation before deleting a normal list', () => {
    const normalList = { id: 2, name: 'Zomerboeken' };

    component.deleteList(normalList);

    expect(toastServiceSpy.showConfirm).toHaveBeenCalled();
    expect(readingListServiceSpy.deleteList).not.toHaveBeenCalled();
  });

  it('should delete list after confirmation callback', () => {
    const normalList = { id: 2, name: 'Zomerboeken' };
    component.readingLists = [normalList, { id: 3, name: 'Andere lijst' }];

    component.deleteList(normalList);

    const confirmArgs = toastServiceSpy.showConfirm.calls.mostRecent().args;
    const onConfirm = confirmArgs[1] as () => void;
    onConfirm();

    expect(readingListServiceSpy.deleteList).toHaveBeenCalledWith(2);
    expect(component.readingLists.some(l => l.id === 2)).toBeFalse();
    expect(toastServiceSpy.show).toHaveBeenCalledWith('Leeslijst verwijderd', 'success');
  });
});
