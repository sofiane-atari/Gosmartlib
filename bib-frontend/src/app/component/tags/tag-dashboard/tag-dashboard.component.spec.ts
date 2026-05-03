import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TagDashboardComponent } from './tag-dashboard.component';

describe('TagDashboardComponent', () => {
  let component: TagDashboardComponent;
  let fixture: ComponentFixture<TagDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TagDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TagDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
