/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.tei.dkprobackport;

import javax.xml.namespace.QName;

public final class TeiConstants
{
    /**
     * (character) contains a significant punctuation mark as identified by the CLAWS tagger.
     */
    public static final String TAG_CHARACTER = "c";

    /**
     * (word) represents a grammatical (not necessarily orthographic) word.
     */
    public static final String TAG_WORD = "w";
    public static final String TAG_MULTIWORD = "mw";

    /**
     * (s-unit) contains a sentence-like division of a text.
     */
    public static final String TAG_SUNIT = "s";

    /**
     * (utterance) contains a stretch of speech usually preceded and followed by silence or by a
     * change of speaker.
     */
    public static final String TAG_U = "u";

    /**
     * (paragraph) marks paragraphs in prose.
     */
    public static final String TAG_PARAGRAPH = "p";

    /**
     * (phrase) represents a grammatical phrase.
     */
    public static final String TAG_PHRASE = "phr";

    /**
     * (referencing string) contains a general purpose name or referring string.
     */
    public static final String TAG_RS = "rs";

    /**
     * contains a single text of any kind, whether unitary or composite, for example a poem or
     * drama, a collection of essays, a novel, a dictionary, or a corpus sample.
     */
    public static final String TAG_TEXT = "text";

    /**
     * contains the full title of a work of any kind.
     */
    public static final String TAG_TITLE = "title";

    /**
     * (TEI document) contains a single TEI-conformant document, comprising a TEI header and a text,
     * either in isolation or as part of a teiCorpus element.
     */
    public static final String TAG_TEI_DOC = "TEI";

    public static final String ATTR_TYPE = "type";
    public static final String ATTR_POS = "pos";
    public static final String ATTR_FUNCTION = "function";
    public static final String ATTR_LEMMA = "lemma";

    public static final String ATTR_XML_ID = "xml:id";

    public static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
    public static final QName E_TEI_TEI = new QName(TEI_NS, TAG_TEI_DOC);
    public static final QName E_TEI_HEADER = new QName(TEI_NS, "teiHeader");
    public static final QName E_TEI_FILE_DESC = new QName(TEI_NS, "fileDesc");
    public static final QName E_TEI_TITLE_STMT = new QName(TEI_NS, "titleStmt");
    public static final QName E_TEI_TITLE = new QName(TEI_NS, TAG_TITLE);
    public static final QName E_TEI_TEXT = new QName(TEI_NS, TAG_TEXT);
    public static final QName E_TEI_BODY = new QName(TEI_NS, "body");

    private TeiConstants()
    {
        // No instances
    }
}
