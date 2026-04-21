import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialAttachmentsQueueComponent } from './didactic-material-attachments-queue.component';

describe('DidacticMaterialAttachmentsQueueComponent', () => {
  let component: DidacticMaterialAttachmentsQueueComponent;
  let fixture: ComponentFixture<DidacticMaterialAttachmentsQueueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialAttachmentsQueueComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialAttachmentsQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
