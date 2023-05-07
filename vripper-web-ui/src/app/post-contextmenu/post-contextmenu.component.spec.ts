import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PostContextmenuComponent } from './post-contextmenu.component';

describe('PostContextmenuComponent', () => {
  let component: PostContextmenuComponent;
  let fixture: ComponentFixture<PostContextmenuComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PostContextmenuComponent],
    });
    fixture = TestBed.createComponent(PostContextmenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
