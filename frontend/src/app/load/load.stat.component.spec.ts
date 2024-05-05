import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoadStatComponent } from './load.stat.component';

describe('LoadComponent', () => {
  let component: LoadStatComponent;
  let fixture: ComponentFixture<LoadStatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadStatComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LoadStatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
