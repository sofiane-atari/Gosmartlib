import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GamificationTabsComponent } from './gamification-tabs.component';

describe('GamificationTabsComponent', () => {
  let component: GamificationTabsComponent;
  let fixture: ComponentFixture<GamificationTabsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GamificationTabsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GamificationTabsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
