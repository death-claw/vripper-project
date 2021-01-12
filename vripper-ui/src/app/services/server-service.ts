import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ServerService {

  constructor() {
  }

  private _baseUrl: string;

  get baseUrl(): string {
    return this._baseUrl;
  }

  set baseUrl(baseUrl: string) {
    this._baseUrl = baseUrl;
  }

  private _wsBaseUrl: string;

  get wsBaseUrl(): string {
    return this._wsBaseUrl;
  }

  set wsBaseUrl(wsBaseUrl: string) {
    this._wsBaseUrl = wsBaseUrl;
  }
}
