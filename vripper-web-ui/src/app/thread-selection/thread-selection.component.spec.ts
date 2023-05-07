import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThreadSelectionComponent } from './thread-selection.component';

describe('ThreadSelectionComponent', () => {
  let component: ThreadSelectionComponent;
  let fixture: ComponentFixture<ThreadSelectionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ThreadSelectionComponent],
    });
    fixture = TestBed.createComponent(ThreadSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
