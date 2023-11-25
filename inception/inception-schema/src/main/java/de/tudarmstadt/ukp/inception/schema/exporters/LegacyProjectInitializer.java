/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.schema.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class LegacyProjectInitializer
{
    private AnnotationSchemaService annotationSchemaService;

    public LegacyProjectInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    /**
     * This method only exists to support importing projects previous to WebAnno version 2.0.
     * Initialize the project with default {@link AnnotationLayer}, {@link TagSet}s, and {@link Tag}
     * s. This is done per Project. For older projects, this method is used to import old tagsets
     * and convert to the new scheme.
     * 
     * @param aProject
     *            the project.
     * @param aPostags
     *            the pos tags.
     * @param aPosTagDescriptions
     *            the pos-tag descriptions.
     * @param aDepTags
     *            the dep tags.
     * @param aDepTagDescriptions
     *            the dep-tag descriptions.
     * @param aNeTags
     *            the ne tags.
     * @param aNeTagDescriptions
     *            the ne-tag descriptions.
     * @param aCorefTypeTags
     *            the coref tags.
     * @param aCorefRelTags
     *            the relation tags.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void initialize(Project aProject, String[] aPostags, String[] aPosTagDescriptions,
            String[] aDepTags, String[] aDepTagDescriptions, String[] aNeTags,
            String[] aNeTagDescriptions, String[] aCorefTypeTags, String[] aCorefRelTags)
        throws IOException
    {
        createTokenLayer(aProject);

        String[] posTags = aPostags.length > 0 ? aPostags
                : new String[] { "$(", "$,", "$.", "ADJA", "ADJD", "ADV", "APPO", "APPR", "APPRART",
                        "APZR", "ART", "CARD", "FM", "ITJ", "KOKOM", "KON", "KOUI", "KOUS", "NE",
                        "NN", "PAV", "PDAT", "PDS", "PIAT", "PIDAT", "PIS", "PPER", "PPOSAT",
                        "PPOSS", "PRELAT", "PRELS", "PRF", "PROAV", "PTKA", "PTKANT", "PTKNEG",
                        "PTKVZ", "PTKZU", "PWAT", "PWAV", "PWS", "TRUNC", "VAFIN", "VAIMP", "VAINF",
                        "VAPP", "VMFIN", "VMINF", "VMPP", "VVFIN", "VVIMP", "VVINF", "VVIZU",
                        "VVPP", "XY", "--" };
        String[] posTagDescriptions = aPosTagDescriptions.length == posTags.length
                ? aPosTagDescriptions
                : new String[] { "sonstige Satzzeichen; satzintern \nBsp: - [,]()",
                        "Komma \nBsp: ,", "Satzbeendende Interpunktion \nBsp: . ? ! ; :   ",
                        "attributives Adjektiv \nBsp: [das] große [Haus]",
                        "adverbiales oder prädikatives Adjektiv \nBsp: [er fährt] schnell, [er ist] schnell",
                        "Adverb \nBsp: schon, bald, doch ",
                        "Postposition \nBsp: [ihm] zufolge, [der Sache] wegen",
                        "Präposition; Zirkumposition links \nBsp: in [der Stadt], ohne [mich]",
                        "Präposition mit Artikel \nBsp: im [Haus], zur [Sache]",
                        "Zirkumposition rechts \nBsp: [von jetzt] an",
                        "bestimmter oder unbestimmter Artikel \nBsp: der, die, das, ein, eine",
                        "Kardinalzahl \nBsp: zwei [Männer], [im Jahre] 1994",
                        "Fremdsprachliches Material \nBsp: [Er hat das mit ``] A big fish ['' übersetzt]",
                        "Interjektion \nBsp: mhm, ach, tja",
                        "Vergleichskonjunktion \nBsp: als, wie",
                        "nebenordnende Konjunktion \nBsp: und, oder, aber",
                        "unterordnende Konjunktion mit ``zu'' und Infinitiv \nBsp: um [zu leben], anstatt [zu fragen]",
                        "unterordnende Konjunktion mit Satz \nBsp: weil, daß, damit, wenn, ob ",
                        "Eigennamen \nBsp: Hans, Hamburg, HSV ",
                        "normales Nomen \nBsp: Tisch, Herr, [das] Reisen",
                        "Pronominaladverb \nBsp: dafür, dabei, deswegen, trotzdem ",
                        "attribuierendes Demonstrativpronomen \nBsp: jener [Mensch]",
                        "substituierendes Demonstrativpronomen \nBsp: dieser, jener",
                        "attribuierendes Indefinitpronomen ohne Determiner \nBsp: kein [Mensch], irgendein [Glas]   ",
                        "attribuierendes Indefinitpronomen mit Determiner \nBsp: [ein] wenig [Wasser], [die] beiden [Brüder] ",
                        "substituierendes Indefinitpronomen \nBsp: keiner, viele, man, niemand ",
                        "irreflexives Personalpronomen \nBsp: ich, er, ihm, mich, dir",
                        "attribuierendes Possessivpronomen \nBsp: mein [Buch], deine [Mutter] ",
                        "substituierendes Possessivpronomen \nBsp: meins, deiner",
                        "attribuierendes Relativpronomen \nBsp: [der Mann ,] dessen [Hund]   ",
                        "substituierendes Relativpronomen \nBsp: [der Hund ,] der  ",
                        "reflexives Personalpronomen \nBsp: sich, einander, dich, mir", "PROAV",
                        "Partikel bei Adjektiv oder Adverb \nBsp: am [schönsten], zu [schnell]",
                        "Antwortpartikel \nBsp: ja, nein, danke, bitte  ",
                        "Negationspartikel \nBsp: nicht",
                        "abgetrennter Verbzusatz \nBsp: [er kommt] an, [er fährt] rad   ",
                        "``zu'' vor Infinitiv \nBsp: zu [gehen]",
                        "attribuierendes Interrogativpronomen \nBsp: welche [Farbe], wessen [Hut]  ",
                        "adverbiales Interrogativ- oder Relativpronomen \nBsp: warum, wo, wann, worüber, wobei",
                        "substituierendes Interrogativpronomen \nBsp: wer, was",
                        "Kompositions-Erstglied \nBsp: An- [und Abreise]",
                        "finites Verb, aux \nBsp: [du] bist, [wir] werden  ",
                        "Imperativ, aux \nBsp: sei [ruhig !]  ",
                        "Infinitiv, aux \nBsp:werden, sein  ",
                        "Partizip Perfekt, aux \nBsp: gewesen ",
                        "finites Verb, modal \nBsp: dürfen  ", "Infinitiv, modal \nBsp: wollen ",
                        "Partizip Perfekt, modal \nBsp: gekonnt, [er hat gehen] können ",
                        "finites Verb, voll \nBsp: [du] gehst, [wir] kommen [an]   ",
                        "Imperativ, voll \nBsp: komm [!] ",
                        "Infinitiv, voll \nBsp: gehen, ankommen",
                        "Infinitiv mit ``zu'', voll \nBsp: anzukommen, loszulassen ",
                        "Partizip Perfekt, voll \nBsp:gegangen, angekommen ",
                        "Nichtwort, Sonderzeichen enthaltend \nBsp:3:7, H2O, D2XW3", "--" };

        TagSet posFeatureTagset = annotationSchemaService.createTagSet(
                "Stuttgart-Tübingen-Tag-Set \nGerman Part of Speech tagset "
                        + "STTS Tag Table (1995/1999): "
                        + "http://www.ims.uni-stuttgart.de/projekte/corplex/TagSets/stts-table.html",
                "STTS", "de", posTags, posTagDescriptions, aProject);

        createPOSLayer(aProject, posFeatureTagset);

        String[] depTags = aDepTags.length > 0 ? aDepTags
                : new String[] { "ADV", "APP", "ATTR", "AUX", "AVZ", "CJ", "DET", "ETH", "EXPL",
                        "GMOD", "GRAD", "KOM", "KON", "KONJ", "NEB", "OBJA", "OBJA2", "OBJA3",
                        "OBJC", "OBJC2", "OBJC3", "OBJD", "OBJD2", "OBJD3", "OBJG", "OBJG2",
                        "OBJG3", "OBJI", "OBJI2", "OBJI3", "OBJP", "OBJP2", "OBJP3", "PAR", "PART",
                        "PN", "PP", "PRED", "-PUNCT-", "REL", "ROOT", "S", "SUBJ", "SUBJ2", "SUBJ3",
                        "SUBJC", "SUBJC2", "SUBJC3", "SUBJI", "SUBJI2", "CP", "PD", "RE", "CD",
                        "DA", "SVP", "OP", "MO", "JU", "CVC", "NG", "SB", "SBP", "AG", "PM", "OCRC",
                        "OG", "SUBJI3", "VOK", "ZEIT", "$", "--", "OC", "OA", "MNR", "NK", "RC",
                        "EP", "CC", "CM", "UC", "AC", "PNC" };
        String[] depTagsDescription = aDepTagDescriptions.length == depTags.length
                ? aDepTagDescriptions
                : depTags;
        TagSet deFeatureTagset = annotationSchemaService.createTagSet("Dependency annotation",
                "Tiger", "de", depTags, depTagsDescription, aProject);
        createDepLayer(aProject, deFeatureTagset);

        String[] neTags = aNeTags.length > 0 ? aNeTags
                : new String[] { "PER", "PERderiv", "PERpart", "LOC", "LOCderiv", "LOCpart", "ORG",
                        "ORGderiv", "ORGpart", "OTH", "OTHderiv", "OTHpart" };
        String[] neTagDescriptions = aNeTagDescriptions.length == neTags.length ? aNeTagDescriptions
                : new String[] { "Person", "Person derivative", "Hyphenated part  is person",
                        "Location", "Location derivative", "Hyphenated part is location",
                        "Organization", "Organization derivative",
                        "Hyphenated part is organization",
                        "Other: Every name that is not a location, person or organisation",
                        "Other derivative", "Hyphenated part  is Other" };
        TagSet neFeatureTagset = annotationSchemaService.createTagSet("Named Entity annotation",
                "NER_WebAnno", "de", neTags, neTagDescriptions, aProject);
        createNeLayer(aProject, neFeatureTagset);

        // Coref Layer
        TagSet corefTypeFeatureTagset = annotationSchemaService.createTagSet(
                "coreference type annotation", "BART", "de",
                aCorefTypeTags.length > 0 ? aCorefTypeTags : new String[] { "nam" },
                aCorefTypeTags.length > 0 ? aCorefTypeTags : new String[] { "nam" }, aProject);
        TagSet corefRelFeatureTagset = annotationSchemaService.createTagSet(
                "coreference relation annotation", "TuebaDZ", "de",
                aCorefRelTags.length > 0 ? aCorefRelTags : new String[] { "anaphoric" },
                aCorefRelTags.length > 0 ? aCorefRelTags : new String[] { "anaphoric" }, aProject);
        createCorefLayer(aProject, corefTypeFeatureTagset, corefRelFeatureTagset);

        createLemmaLayer(aProject);

        createChunkLayer(aProject);
    }

    private void createLemmaLayer(Project aProject) throws IOException
    {
        AnnotationLayer tokenLayer = annotationSchemaService.findLayer(aProject,
                Token.class.getName());

        AnnotationFeature tokenLemmaFeature = new AnnotationFeature(aProject, tokenLayer, "lemma",
                "lemma", Lemma.class.getName());
        annotationSchemaService.createFeature(tokenLemmaFeature);

        AnnotationLayer lemmaLayer = new AnnotationLayer(Lemma.class.getName(), "Lemma", SPAN_TYPE,
                aProject, true, SINGLE_TOKEN, NO_OVERLAP);
        lemmaLayer.setAttachType(tokenLayer);
        lemmaLayer.setAttachFeature(tokenLemmaFeature);
        annotationSchemaService.createOrUpdateLayer(lemmaLayer);

        AnnotationFeature lemmaFeature = new AnnotationFeature();
        lemmaFeature.setDescription("lemma Annotation");
        lemmaFeature.setName("value");
        lemmaFeature.setType(CAS.TYPE_NAME_STRING);
        lemmaFeature.setProject(aProject);
        lemmaFeature.setUiName("Lemma");
        lemmaFeature.setLayer(lemmaLayer);
        annotationSchemaService.createFeature(lemmaFeature);
    }

    private AnnotationLayer createCorefLayer(Project aProject, TagSet aCorefTypeTags,
            TagSet aCorefRelTags)
        throws IOException
    {
        AnnotationLayer base = new AnnotationLayer(
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", "Coreference",
                CHAIN_TYPE, aProject, true, TOKENS, ANY_OVERLAP);
        base.setCrossSentence(true);
        annotationSchemaService.createOrUpdateLayer(base);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, base, "referenceType",
                "referenceType", CAS.TYPE_NAME_STRING, "Coreference type", aCorefTypeTags));
        annotationSchemaService.createFeature(
                new AnnotationFeature(aProject, base, "referenceRelation", "referenceRelation",
                        CAS.TYPE_NAME_STRING, "Coreference relation", aCorefRelTags));

        return base;
    }

    private void createNeLayer(Project aProject, TagSet aTagSet) throws IOException
    {
        AnnotationLayer neLayer = new AnnotationLayer(NamedEntity.class.getName(), "Named entity",
                SPAN_TYPE, aProject, true, TOKENS, ANY_OVERLAP);
        annotationSchemaService.createOrUpdateLayer(neLayer);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, neLayer, "value",
                "value", CAS.TYPE_NAME_STRING, "Named entity type", aTagSet));
    }

    private void createChunkLayer(Project aProject) throws IOException
    {
        AnnotationLayer chunkLayer = new AnnotationLayer(Chunk.class.getName(), "Chunk", SPAN_TYPE,
                aProject, true, TOKENS, NO_OVERLAP);
        annotationSchemaService.createOrUpdateLayer(chunkLayer);

        AnnotationFeature chunkValueFeature = new AnnotationFeature();
        chunkValueFeature.setDescription("Chunk tag");
        chunkValueFeature.setName("chunkValue");
        chunkValueFeature.setType(CAS.TYPE_NAME_STRING);
        chunkValueFeature.setProject(aProject);
        chunkValueFeature.setUiName("Tag");
        chunkValueFeature.setLayer(chunkLayer);
        annotationSchemaService.createFeature(chunkValueFeature);
    }

    private void createDepLayer(Project aProject, TagSet aTagset) throws IOException
    {
        // Dependency Layer
        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RELATION_TYPE, aProject, true, SINGLE_TOKEN, OVERLAP_ONLY);
        AnnotationLayer tokenLayer = annotationSchemaService.findLayer(aProject,
                Token.class.getName());
        List<AnnotationFeature> tokenFeatures = annotationSchemaService
                .listAnnotationFeature(tokenLayer);
        AnnotationFeature tokenPosFeature = null;
        for (AnnotationFeature feature : tokenFeatures) {
            if (feature.getName().equals("pos")) {
                tokenPosFeature = feature;
                break;
            }
        }
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenPosFeature);

        annotationSchemaService.createOrUpdateLayer(depLayer);
        annotationSchemaService
                .createFeature(new AnnotationFeature(aProject, depLayer, "DependencyType",
                        "Relation", CAS.TYPE_NAME_STRING, "Dependency relation", aTagset));

        String[] flavors = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        String[] flavorDesc = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        TagSet flavorsTagset = annotationSchemaService.createTagSet("Dependency flavors",
                "Dependency flavors", "mul", flavors, flavorDesc, aProject);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, depLayer, "flavor",
                "Flavor", CAS.TYPE_NAME_STRING, "Dependency relation", flavorsTagset));
    }

    private void createPOSLayer(Project aProject, TagSet aPosTagset) throws IOException
    {
        AnnotationLayer tokenLayer = annotationSchemaService.findLayer(aProject,
                Token.class.getName());

        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE,
                aProject, true, SINGLE_TOKEN, NO_OVERLAP);

        AnnotationFeature tokenPosFeature = new AnnotationFeature(aProject, tokenLayer, "pos",
                "pos", POS.class.getName());
        annotationSchemaService.createFeature(tokenPosFeature);

        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        annotationSchemaService.createOrUpdateLayer(posLayer);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, posLayer, "PosValue",
                "PosValue", CAS.TYPE_NAME_STRING, "Part-of-speech tag", aPosTagset));
    }

    private AnnotationLayer createTokenLayer(Project aProject) throws IOException
    {
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE,
                aProject, true, SINGLE_TOKEN, NO_OVERLAP);

        annotationSchemaService.createOrUpdateLayer(tokenLayer);
        return tokenLayer;
    }
}
