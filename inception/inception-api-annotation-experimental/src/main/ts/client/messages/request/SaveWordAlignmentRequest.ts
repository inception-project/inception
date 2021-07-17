export class SaveWordAlignmentRequest
{
    clientName: string;
    userName: string;
    projectId: number;
    sentence: number;
    alignments: string;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aSentence: number, aAlignments: string)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.sentence = aSentence;
        this.alignments = aAlignments;
    }
}