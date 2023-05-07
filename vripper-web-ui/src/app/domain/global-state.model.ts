export class GlobalState {
  constructor(
    public running: number,
    public remaining: number,
    public error: number,
    public loggedUser: string,
    public downloadSpeed: string
  ) {}
}
