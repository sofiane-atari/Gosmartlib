import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FeaturedReadingListsSidebarComponent } from './featured-reading-lists-sidebar.component';

describe('FeaturedReadingListsSidebarComponent', () => {
  let component: FeaturedReadingListsSidebarComponent;
  let fixture: ComponentFixture<FeaturedReadingListsSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeaturedReadingListsSidebarComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FeaturedReadingListsSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
