import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GamificationSummaryComponent } from './gamification-summary.component';

describe('GamificationSummaryComponent', () => {
  let component: GamificationSummaryComponent;
  let fixture: ComponentFixture<GamificationSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GamificationSummaryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GamificationSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
