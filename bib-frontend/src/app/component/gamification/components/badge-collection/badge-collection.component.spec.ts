import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BadgeCollectionComponent } from './badge-collection.component';

describe('BadgeCollectionComponent', () => {
  let component: BadgeCollectionComponent;
  let fixture: ComponentFixture<BadgeCollectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BadgeCollectionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BadgeCollectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
