import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CatSummaryComponent } from './cat.summary.component';

describe('CatSummaryComponent', () => {
  let component: CatSummaryComponent;
  let fixture: ComponentFixture<CatSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatSummaryComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CatSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
