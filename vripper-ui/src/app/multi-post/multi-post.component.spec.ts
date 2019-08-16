import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiPostComponent } from './multi-post.component';

describe('MultiPostComponent', () => {
  let component: MultiPostComponent;
  let fixture: ComponentFixture<MultiPostComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MultiPostComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MultiPostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
