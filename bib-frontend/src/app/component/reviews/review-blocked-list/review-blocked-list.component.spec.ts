import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewBlockedListComponent } from './review-blocked-list.component';

describe('ReviewBlockedListComponent', () => {
  let component: ReviewBlockedListComponent;
  let fixture: ComponentFixture<ReviewBlockedListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewBlockedListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewBlockedListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
