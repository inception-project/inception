export class NewDocumentRequest {
    clientName: string;
    userName: string;
    projectId: number;
    documentId: number;
    viewportType: string;
    viewport: number[][];
    recommenderEnabled: boolean;

    constructor(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aViewportType: string, aViewport: number[][], aRecommenderEnabled: boolean) {
        this.clientName = aClientName;
        this.userName = aUserName;
        this.projectId = aProjectId;
        this.documentId = aDocumentId;
        this.viewportType = aViewportType;
        this.viewport = aViewport;
        this.recommenderEnabled = aRecommenderEnabled;
    }
}