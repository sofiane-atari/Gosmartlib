import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecentGamificationEventsComponent } from './recent-gamification-events.component';

describe('RecentGamificationEventsComponent', () => {
  let component: RecentGamificationEventsComponent;
  let fixture: ComponentFixture<RecentGamificationEventsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecentGamificationEventsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RecentGamificationEventsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
