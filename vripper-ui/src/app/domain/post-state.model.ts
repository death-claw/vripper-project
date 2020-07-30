export class PostState {
  constructor(
    public postId: string,
    public title: string,
    public progress: number,
    public status: string,
    public removed: boolean,
    public url: string,
    public done: number,
    public total: number,
    public hosts: string[],
    public thanked: boolean,
    public previews: string[],
    public metadata: Metadata
  ) {
  }
}

export interface Metadata {
  resolvedNames: string[];
  postedBy: string;
}
