export class EventLog {
  constructor(
    public id: number,
    public type: string,
    public status: string,
    public time: string,
    public message: string
  ) {
  }
}
