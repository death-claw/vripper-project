import { Injectable } from '@angular/core';

@Injectable()
export class ServerService {

  private _baseUrl: string;

  constructor() {}

  set baseUrl(baseUrl: string) {
    this._baseUrl = baseUrl;
  }

  get baseUrl(): string {
    return this._baseUrl;
  }
}
