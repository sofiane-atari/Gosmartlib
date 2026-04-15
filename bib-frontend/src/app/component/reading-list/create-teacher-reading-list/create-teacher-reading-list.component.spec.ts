import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { CreateTeacherReadingListComponent } from './create-teacher-reading-list.component';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';

describe('CreateTeacherReadingListComponent', () => {
  let component: CreateTeacherReadingListComponent;
  let fixture: ComponentFixture<CreateTeacherReadingListComponent>;

  beforeEach(async () => {
    const readingListServiceSpy = jasmine.createSpyObj<ReadingListService>('ReadingListService', ['createList']);
    readingListServiceSpy.createList.and.returnValue(of({ id: 1, name: 'Leerkracht lijst', targetType: 'PERSONAL' }));

    const routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [CreateTeacherReadingListComponent],
      providers: [
        { provide: ReadingListService, useValue: readingListServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTeacherReadingListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
