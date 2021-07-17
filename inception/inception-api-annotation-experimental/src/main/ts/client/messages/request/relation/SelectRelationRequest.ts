export class SelectRelationRequest
{
    clientName : string;
    userName : string;
    projectId : number;
    documentId : number;
    relationAddress : number;


    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aRelationAddress: number)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.relationAddress = aRelationAddress;
    }
}