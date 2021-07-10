export class Span
{
    id: number;
    coveredText: string;
    color: string;
    begin: number;
    end: number;
    type: string;
    feature: string;
    color: string;

    constructor(aId: number, aCoveredText: string, aBegin: number, aEnd: number, aType: string, aFeature: string, aColor: string)
    {
        this.id = aId;
        this.coveredText = aCoveredText;
        this.begin = aBegin;
        this.end = aEnd;
        this.type = aType;
        this.feature = aFeature;
        this.color = aColor;
    }

}