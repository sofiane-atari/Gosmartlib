import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GamificationRewardNotificationComponent } from './gamification-reward-notification.component';

describe('GamificationRewardNotificationComponent', () => {
  let component: GamificationRewardNotificationComponent;
  let fixture: ComponentFixture<GamificationRewardNotificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GamificationRewardNotificationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GamificationRewardNotificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
