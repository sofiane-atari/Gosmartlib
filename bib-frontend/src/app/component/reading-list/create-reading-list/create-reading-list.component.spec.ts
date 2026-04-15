import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { CreateReadingListComponent } from './create-reading-list.component';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';

describe('CreateReadingListComponent', () => {
  let component: CreateReadingListComponent;
  let fixture: ComponentFixture<CreateReadingListComponent>;

  beforeEach(async () => {
    const readingListServiceSpy = jasmine.createSpyObj<ReadingListService>('ReadingListService', ['createList']);
    readingListServiceSpy.createList.and.returnValue(of({ id: 1, name: 'Testlijst' }));

    const routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [CreateReadingListComponent],
      providers: [
        { provide: ReadingListService, useValue: readingListServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateReadingListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
