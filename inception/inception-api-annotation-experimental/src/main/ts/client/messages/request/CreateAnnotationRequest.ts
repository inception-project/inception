export class CreateAnnotationRequest
{
    clientName : string;
    userName : string;
    projectId : number;
    documentId : number;
    begin : number;
    end : number;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aBegin: number, aEnd: number)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.begin = aBegin;
        this.end = aEnd;
    }
}