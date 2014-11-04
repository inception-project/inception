/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;

/**
 * Response for the {@code getDocument} command.
 */
public class GetDocumentResponse
    extends AjaxResponse
{
    public static final String COMMAND = "getDocument";

    private List<String> modifications = new ArrayList<String>();

    @JsonProperty("sentence_number_offset")
    int sentenceNumberOffset;

    private String text;

    @JsonProperty("source_files")
    private List<String> sourceFiles = new ArrayList<String>();

    private long ctime;
    private long mtime;

    // This seems to be no longer used in brat
    // https://github.com/nlplab/brat/blob/master/server/src/document.py#L794
    // private int offset;

    private GetCollectionInformationResponse info;

    public GetCollectionInformationResponse getInfo()
    {
        return info;
    }

    public void setInfo(GetCollectionInformationResponse aInfo)
    {
        info = aInfo;
    }

    /**
     * [ 0, 3 ]
     */
    @JsonProperty("token_offsets")
    private List<Offsets> tokenOffsets = new ArrayList<Offsets>();

    /**
     * [ 0, 3 ]
     */
    @JsonProperty("sentence_offsets")
    private List<Offsets> sentenceOffsets = new ArrayList<Offsets>();

    private List<Relation> relations = new ArrayList<Relation>();

    /**
     * ["T1", "Protein", 8, 11]
     *
     * Guess: ID (maybe token ID?), Type, begin offset, end offset
     */
    private List<Entity> entities = new ArrayList<Entity>();
    private List<String> attributes = new ArrayList<String>();
    private List<String> equivs = new ArrayList<String>();
    private List<String[]> comments = new ArrayList<String[]>();

    public GetDocumentResponse()
    {
        super(COMMAND);
    }

    public void addToken(int aBegin, int aEnd)
    {
        tokenOffsets.add(new Offsets(aBegin, aEnd));
    }

    public void addSentence(int aBegin, int aEnd)
    {
        sentenceOffsets.add(new Offsets(aBegin, aEnd));
    }

    public List<String> getModifications()
    {
        return modifications;
    }

    public void setModifications(List<String> aModifications)
    {
        modifications = aModifications;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }

    public List<String[]> getComments()
    {
        return comments;
    }

    public void setComments(List<String[]> aComments)
    {
        comments = aComments;
    }

    /**
     * Get source files for the annotations.
     * @return the source files.
     */
    public List<String> getSourceFiles()
    {
        return sourceFiles;
    }

    /**
     * Set source files for the annotations.
     * @param aSourceFiles the source files.
     */
    public void setSourceFiles(List<String> aSourceFiles)
    {
        sourceFiles = aSourceFiles;
    }

    /**
     * Get creation time.
     * @return the timestamp.
     */
    public long getCtime()
    {
        return ctime;
    }

    /**
     * Set creation time.
     *
     * @param aCtime
     *            creation time.
     */
    public void setCtime(long aCtime)
    {
        ctime = aCtime;
    }

    /**
     * Get last modification time.
     *
     * @return last modification time.
     */
    public long getMtime()
    {
        return mtime;
    }

    /**
     * Set last modification time.
     *
     * @param aMtime
     *            last modfication time.
     */
    public void setMtime(long aMtime)
    {
        mtime = aMtime;
    }

    // public int getOffset()
    // {
    // return offset;
    // }
    //
    // public void setOffset(int aOffset)
    // {
    // offset = aOffset;
    // }

    public List<Offsets> getTokenOffsets()
    {
        return tokenOffsets;
    }

    public void setTokenOffsets(List<Offsets> aTokenOffsets)
    {
        tokenOffsets = aTokenOffsets;
    }

    public List<Offsets> getSentenceOffsets()
    {
        return sentenceOffsets;
    }

    public void setSentenceOffsets(List<Offsets> aSentenceOffsets)
    {
        sentenceOffsets = aSentenceOffsets;
    }

    public List<Relation> getRelations()
    {
        return relations;
    }

    public void setRelations(List<Relation> aRelations)
    {
        relations = aRelations;
    }

    public void addRelation(Relation aRelation)
    {
        relations.add(aRelation);
    }

    public List<Entity> getEntities()
    {
        return entities;
    }

    public void setEntities(List<Entity> aEntities)
    {
        entities = aEntities;
    }

    public void addEntity(Entity aEntity)
    {
        entities.add(aEntity);
    }

    public List<String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(List<String> aAttributes)
    {
        attributes = aAttributes;
    }

    public List<String> getEquivs()
    {
        return equivs;
    }

    public void setEquivs(List<String> aEquivs)
    {
        equivs = aEquivs;
    }

    public int getSentenceNumberOffset()
    {
        return sentenceNumberOffset;
    }

    public void setSentenceNumberOffset(int aSentenceNumberOffset)
    {
        sentenceNumberOffset = aSentenceNumberOffset;
    }



}
