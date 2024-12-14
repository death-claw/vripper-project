export class Log {
  constructor(
    public sequence: number,
    public timestamp: string,
    public threadName: string,
    public loggerName: string,
    public levelString: string,
    public formattedMessage: string,
    public throwable: string,
  ) {}
}
