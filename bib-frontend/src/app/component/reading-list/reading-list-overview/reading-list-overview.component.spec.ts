import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ReadingListOverviewComponent } from './reading-list-overview.component';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';
import { ToastService } from '../../../services/toast/toast.service';
import { BookService } from '../../../services/bookService/book.service';
import { RoleService } from '../../../services/role-service/role.service';

describe('ReadingListOverviewComponent', () => {
  let component: ReadingListOverviewComponent;
  let fixture: ComponentFixture<ReadingListOverviewComponent>;
  let readingListServiceSpy: jasmine.SpyObj<ReadingListService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let roleServiceSpy: jasmine.SpyObj<RoleService>;

  beforeEach(async () => {
    readingListServiceSpy = jasmine.createSpyObj<ReadingListService>('ReadingListService', ['getLists', 'deleteList']);
    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['show', 'showConfirm']);
    const bookServiceSpy = jasmine.createSpyObj<BookService>('BookService', ['getBookById']);
    roleServiceSpy = jasmine.createSpyObj<RoleService>('RoleService', ['isTeacher']);

    readingListServiceSpy.getLists.and.returnValue(of([]));
    readingListServiceSpy.deleteList.and.returnValue(of(void 0));
    roleServiceSpy.isTeacher.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [ReadingListOverviewComponent],
      providers: [
        { provide: ReadingListService, useValue: readingListServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: RoleService, useValue: roleServiceSpy }
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

  it('should route students to standard create page', () => {
    roleServiceSpy.isTeacher.and.returnValue(false);
    expect(component.createRoute).toBe('/reading-list/create');
  });

  it('should route teachers to teacher create page', () => {
    roleServiceSpy.isTeacher.and.returnValue(true);
    expect(component.createRoute).toBe('/reading-list/create-teacher');
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
    expect(toastServiceSpy.show).toHaveBeenCalledWith('Deze leeslijst kan niet verwijderd worden', 'error');
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

  it('should allow teachers to delete shared lists', () => {
    roleServiceSpy.isTeacher.and.returnValue(true);
    component.readingLists = [
      { id: 1, name: 'Klaslijst', targetType: 'CLASS' },
      { id: 2, name: 'Favorieten', targetType: 'PERSONAL' }
    ];

    fixture.detectChanges();

    const deleteButtons = fixture.nativeElement.querySelectorAll('.btn-delete');
    expect(deleteButtons.length).toBe(1);
  });
});
