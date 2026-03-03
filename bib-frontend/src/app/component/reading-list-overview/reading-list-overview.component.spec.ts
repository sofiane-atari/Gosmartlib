import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReadingListOverviewComponent } from './reading-list-overview.component';

describe('ReadingListOverviewComponent', () => {
  let component: ReadingListOverviewComponent;
  let fixture: ComponentFixture<ReadingListOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReadingListOverviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReadingListOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
