import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { CreateTeacherReadingListComponent } from './create-teacher-reading-list.component';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';

describe('CreateTeacherReadingListComponent', () => {
  let component: CreateTeacherReadingListComponent;
  let fixture: ComponentFixture<CreateTeacherReadingListComponent>;
  let readingListServiceSpy: jasmine.SpyObj<ReadingListService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    readingListServiceSpy = jasmine.createSpyObj<ReadingListService>('ReadingListService', ['createList']);
    readingListServiceSpy.createList.and.returnValue(of({ id: 1, name: 'Leerkracht lijst', targetType: 'PERSONAL' }));

    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
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

  it('should show target value field for class/year/school scopes', () => {
    component.selectedScope = 'CLASS';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#targetValue')).toBeTruthy();
    expect(component.requiresTargetValue).toBeTrue();
    expect(component.targetValueLabel).toBe('Klas');
  });

  it('should hide target value field for personal scope', () => {
    component.selectedScope = 'PERSONAL';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#targetValue')).toBeFalsy();
    expect(component.requiresTargetValue).toBeFalse();
  });

  it('should submit target type and target value for teacher scopes', () => {
    component.listName = 'Verplichte lectuur';
    component.selectedScope = 'YEAR';
    component.targetValue = '2';

    component.submit();

    expect(readingListServiceSpy.createList).toHaveBeenCalledWith({
      name: 'Verplichte lectuur',
      targetType: 'YEAR',
      targetValue: '2'
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/reading-list']);
  });
});
