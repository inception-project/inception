export class UpdateRelationRequest
{
    clientName : string;
    userName : string;
    projectId : number;
    documentId : number;
    relationAddress : number;
    newDependencyType : string;
    newFlavor : string;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aRelationAddress: number, aNewDependencyType: string, aNewFlavor: string)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.relationAddress = aRelationAddress;
        this.newDependencyType = aNewDependencyType;
        this.newFlavor = aNewFlavor;
    }
}