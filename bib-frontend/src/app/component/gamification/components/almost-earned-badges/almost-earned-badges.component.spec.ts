import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AlmostEarnedBadgesComponent } from './almost-earned-badges.component';

describe('AlmostEarnedBadgesComponent', () => {
  let component: AlmostEarnedBadgesComponent;
  let fixture: ComponentFixture<AlmostEarnedBadgesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlmostEarnedBadgesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AlmostEarnedBadgesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
