import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ReadingListDetailComponent } from './reading-list-detail.component';
import { ReadingListService } from '../../services/reading-list.service';
import { BookService } from '../../services/BookService/book.service';
import { ToastService } from '../../services/Toast/toast.service';

describe('ReadingListDetailComponent', () => {
  let component: ReadingListDetailComponent;
  let fixture: ComponentFixture<ReadingListDetailComponent>;
  let readingListServiceSpy: jasmine.SpyObj<ReadingListService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    readingListServiceSpy = jasmine.createSpyObj<ReadingListService>('ReadingListService', ['getListById', 'removeBookFromList']);
    bookServiceSpy = jasmine.createSpyObj<BookService>('BookService', ['getBookById']);
    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['show', 'showConfirm']);

    readingListServiceSpy.getListById.and.returnValue(of({ id: 10, name: 'Test lijst', books: [] }));
    readingListServiceSpy.removeBookFromList.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [ReadingListDetailComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '10' } } } },
        { provide: ReadingListService, useValue: readingListServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReadingListDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should ask confirmation before removing a book', () => {
    component.list = {
      id: 10,
      name: 'Test lijst',
      books: [{ id: 1, bookId: 200 }]
    };

    const event = new MouseEvent('click');
    spyOn(event, 'stopPropagation');

    component.removeBookFromList(200, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(toastServiceSpy.showConfirm).toHaveBeenCalled();
    expect(readingListServiceSpy.removeBookFromList).not.toHaveBeenCalled();
  });

  it('should remove book from list and update local state after confirmation', () => {
    component.list = {
      id: 10,
      name: 'Test lijst',
      books: [{ id: 1, bookId: 200 }]
    };
    component.booksInList = [{ id: 200, title: 'Boek', author: 'Auteur' } as any];

    const event = new MouseEvent('click');
    spyOn(event, 'stopPropagation');

    component.removeBookFromList(200, event);

    const confirmArgs = toastServiceSpy.showConfirm.calls.mostRecent().args;
    const onConfirm = confirmArgs[1] as () => void;
    onConfirm();

    expect(readingListServiceSpy.removeBookFromList).toHaveBeenCalledWith(10, 200);
    expect(component.booksInList.length).toBe(0);
    expect(component.list.books?.length).toBe(0);
    expect(toastServiceSpy.show).toHaveBeenCalledWith('Boek verwijderd uit leeslijst', 'success');
  });
});
