export class Annotation
{
    id: string;
    word: string;
    begin: number;
    end: number;
    type: string;

    constructor()
    {
    }

    constructor(aId: string, aWord: string, aBegin: number, aEnd: number, aType: string)
    {
        this.id = aId;
        this.word = aWord;
        this.begin = aBegin;
        this.end = aEnd;
        this.type = aType;
    }
}