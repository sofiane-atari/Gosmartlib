import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BadgeCollectionCardComponent } from './badge-collection-card.component';

describe('BadgeCollectionCardComponent', () => {
  let component: BadgeCollectionCardComponent;
  let fixture: ComponentFixture<BadgeCollectionCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BadgeCollectionCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BadgeCollectionCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
