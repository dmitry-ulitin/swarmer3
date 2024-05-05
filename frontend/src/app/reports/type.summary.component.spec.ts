import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TypeSummaryComponent } from './type.summary.component';

describe('TypeSummaryComponent', () => {
  let component: TypeSummaryComponent;
  let fixture: ComponentFixture<TypeSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TypeSummaryComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TypeSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
