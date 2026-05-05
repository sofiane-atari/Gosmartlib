import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BadgeProgressCardComponent } from './badge-progress-card.component';

describe('BadgeProgressCardComponent', () => {
  let component: BadgeProgressCardComponent;
  let fixture: ComponentFixture<BadgeProgressCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BadgeProgressCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BadgeProgressCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
