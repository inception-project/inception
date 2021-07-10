export class SelectAnnotationRequest
{
    clientName : string;
    userName : string;
    projectId : number;
    documentId : number;
    annotationAddress : number;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.annotationAddress = aAnnotationAddress;
    }
}