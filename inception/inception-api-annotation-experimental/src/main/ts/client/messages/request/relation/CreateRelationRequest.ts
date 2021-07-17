export class CreateRelationRequest
{
    clientName : string;
    userName : string;
    projectId : number;
    documentId : number;
    governorId : number;
    dependentId : number;
    dependencyType : string;
    flavor : string;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aGovernorId: number, aDependentId: number, aDependencyType: string, aFlavor: string)
    {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.governorId = aGovernorId;
        this.dependentId = aDependentId;
        this.dependencyType = aDependencyType;
        this.flavor = aFlavor;
    }
}