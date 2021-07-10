export class UpdateAnnotationRequest
{
    clientName : string;
    userName : string;
    projectId : number;
    documentId : number;
    annotationAddress : number;
    newType : string;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aAnnotationAddress: number, aNewType: string)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.annotationAddress = aAnnotationAddress;
        this.newType = aNewType;
    }
}