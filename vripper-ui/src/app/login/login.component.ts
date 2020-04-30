import {Component, HostBinding, OnInit} from '@angular/core';
import {DomSanitizer, SafeStyle} from '@angular/platform-browser';
import {Credentials} from '../domain/credential.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})

export class LoginComponent implements OnInit {

  constructor(
    private sanitizer: DomSanitizer
  ) { }

  @HostBinding('style')
  get myStyle(): SafeStyle {
    return this.sanitizer.bypassSecurityTrustStyle('height: 100%;');
  }

  credentials: Credentials = {
    username: '',
    password: ''
  };

  login(event) {
  }

  ngOnInit() {
  }

}
