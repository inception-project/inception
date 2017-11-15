/*
 * Copyright 2017
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.core.io.ClassPathResource;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;

/**
 * This class is meant to be used only by {@link AnnotationSchemaServiceImpl} and within a
 * transaction set up by that class via
 * {@link AnnotationSchemaServiceImpl#initializeTypesForProject(Project)} or
 * {@link AnnotationSchemaServiceImpl#initializeTypesForProject(Project)}.
 */
class ProjectInitializer
{
    private AnnotationSchemaService annotationSchemaService;
    
    public ProjectInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }
    
    public void initialize(Project aProject)
            throws IOException
    {
        // Default layers with default tagsets
        createTokenLayer(aProject);

        TagSet posTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-pos-ud.json").getInputStream(),
                annotationSchemaService);
        createPOSLayer(aProject, posTagSet);

        TagSet depTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-dep-ud.json").getInputStream(),
                annotationSchemaService);
        createDepLayer(aProject, depTagSet);

        TagSet nerTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-ne-webanno.json").getInputStream(),
                annotationSchemaService);
        createNeLayer(aProject, nerTagSet);

        TagSet corefTypeTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-coref-type-bart.json").getInputStream(),
                annotationSchemaService);
        TagSet corefRelTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-coref-rel-tuebadz.json").getInputStream(),
                annotationSchemaService);
        createCorefLayer(aProject, corefTypeTagSet, corefRelTagSet);

        createLemmaLayer(aProject);

        createChunkLayer(aProject);

        createSurfaceFormLayer(aProject);
        
        createSemArgLayer(aProject);
        
        createSemPredLayer(aProject);

        // Extra tagsets
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-pos-stts.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-dep-tiger.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/en-dep-sd.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/en-pos-ptb-tt.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-pos-upos.json").getInputStream(),
                annotationSchemaService);
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
    public void initializeV0(Project aProject, String[] aPostags, String[] aPosTagDescriptions,
            String[] aDepTags, String[] aDepTagDescriptions, String[] aNeTags,
            String[] aNeTagDescriptions, String[] aCorefTypeTags, String[] aCorefRelTags)
        throws IOException
    {
        createTokenLayer(aProject);

        String[] posTags = aPostags.length > 0 ? aPostags : new String[] { "$(", "$,", "$.",
                "ADJA", "ADJD", "ADV", "APPO", "APPR", "APPRART", "APZR", "ART", "CARD", "FM",
                "ITJ", "KOKOM", "KON", "KOUI", "KOUS", "NE", "NN", "PAV", "PDAT", "PDS", "PIAT",
                "PIDAT", "PIS", "PPER", "PPOSAT", "PPOSS", "PRELAT", "PRELS", "PRF", "PROAV",
                "PTKA", "PTKANT", "PTKNEG", "PTKVZ", "PTKZU", "PWAT", "PWAV", "PWS", "TRUNC",
                "VAFIN", "VAIMP", "VAINF", "VAPP", "VMFIN", "VMINF", "VMPP", "VVFIN", "VVIMP",
                "VVINF", "VVIZU", "VVPP", "XY", "--" };
        String[] posTagDescriptions = aPosTagDescriptions.length == posTags.length
                ? aPosTagDescriptions : new String[] {
                        "sonstige Satzzeichen; satzintern \nBsp: - [,]()",
                        "Komma \nBsp: ,",
                        "Satzbeendende Interpunktion \nBsp: . ? ! ; :   ",
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
                        "attribuierendes Possessivpronome \nBsp: mein [Buch], deine [Mutter] ",
                        "substituierendes Possessivpronome \nBsp: meins, deiner",
                        "attribuierendes Relativpronomen \nBsp: [der Mann ,] dessen [Hund]   ",
                        "substituierendes Relativpronomen \nBsp: [der Hund ,] der  ",
                        "reflexives Personalpronomen \nBsp: sich, einander, dich, mir",
                        "PROAV",
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

        String[] depTags = aDepTags.length > 0 ? aDepTags : new String[] { "ADV", "APP", "ATTR",
                "AUX", "AVZ", "CJ", "DET", "ETH", "EXPL", "GMOD", "GRAD", "KOM", "KON", "KONJ",
                "NEB", "OBJA", "OBJA2", "OBJA3", "OBJC", "OBJC2", "OBJC3", "OBJD", "OBJD2",
                "OBJD3", "OBJG", "OBJG2", "OBJG3", "OBJI", "OBJI2", "OBJI3", "OBJP", "OBJP2",
                "OBJP3", "PAR", "PART", "PN", "PP", "PRED", "-PUNCT-", "REL", "ROOT", "S", "SUBJ",
                "SUBJ2", "SUBJ3", "SUBJC", "SUBJC2", "SUBJC3", "SUBJI", "SUBJI2", "CP", "PD", "RE",
                "CD", "DA", "SVP", "OP", "MO", "JU", "CVC", "NG", "SB", "SBP", "AG", "PM", "OCRC",
                "OG", "SUBJI3", "VOK", "ZEIT", "$", "--", "OC", "OA", "MNR", "NK", "RC", "EP",
                "CC", "CM", "UC", "AC", "PNC" };
        String[] depTagsDescription = aDepTagDescriptions.length == depTags.length
                ? aDepTagDescriptions : depTags;
        TagSet deFeatureTagset = annotationSchemaService.createTagSet("Dependency annotation",
                "Tiger", "de", depTags, depTagsDescription, aProject);
        createDepLayer(aProject, deFeatureTagset);

        String[] neTags = aNeTags.length > 0 ? aNeTags : new String[] { "PER", "PERderiv",
                "PERpart", "LOC", "LOCderiv", "LOCpart", "ORG", "ORGderiv", "ORGpart", "OTH",
                "OTHderiv", "OTHpart" };
        String[] neTagDescriptions = aNeTagDescriptions.length == neTags.length ? aNeTagDescriptions
                : new String[] { "Person", "Person derivative", "Hyphenated part  is person",
                        "Location derivatives", "Location derivative",
                        "Hyphenated part  is location", "Organization", "Organization derivative",
                        "Hyphenated part  is organization",
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
    
    private void createLemmaLayer(Project aProject)
            throws IOException
    {
        AnnotationLayer tokenLayer = annotationSchemaService.getLayer(Token.class.getName(),
                aProject);
        AnnotationFeature tokenLemmaFeature = createFeature("lemma", "lemma", aProject, tokenLayer,
                Lemma.class.getName());
        tokenLemmaFeature.setVisible(true);

        AnnotationLayer lemmaLayer = new AnnotationLayer(Lemma.class.getName(), "Lemma", SPAN_TYPE,
                aProject, true);
        lemmaLayer.setAttachType(tokenLayer);
        lemmaLayer.setAttachFeature(tokenLemmaFeature);
        annotationSchemaService.createLayer(lemmaLayer);

        AnnotationFeature lemmaFeature = new AnnotationFeature();
        lemmaFeature.setDescription("lemma Annotation");
        lemmaFeature.setName("value");
        lemmaFeature.setType(CAS.TYPE_NAME_STRING);
        lemmaFeature.setProject(aProject);
        lemmaFeature.setUiName("Lemma value");
        lemmaFeature.setLayer(lemmaLayer);
        annotationSchemaService.createFeature(lemmaFeature);
    }

    private AnnotationLayer createCorefLayer(Project aProject, TagSet aCorefTypeTags,
            TagSet aCorefRelTags)
        throws IOException
    {
        AnnotationLayer base = new AnnotationLayer(
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", "Coreference",
                CHAIN_TYPE, aProject, true);
        base.setCrossSentence(true);
        base.setAllowStacking(true);
        base.setMultipleTokens(true);
        base.setLockToTokenOffset(false);
        annotationSchemaService.createLayer(base);

        AnnotationFeature corefTypeFeature = createFeature("referenceType", "referenceType",
                "Coreference type", CAS.TYPE_NAME_STRING, aCorefTypeTags, aProject);
        corefTypeFeature.setLayer(base);
        corefTypeFeature.setVisible(true);

        AnnotationFeature corefRelFeature = createFeature("referenceRelation", "referenceRelation",
                "Coreference relation", CAS.TYPE_NAME_STRING, aCorefRelTags, aProject);
        corefRelFeature.setLayer(base);
        corefRelFeature.setVisible(true);

        return base;
    }

    private void createNeLayer(Project aProject, TagSet aTagset)
        throws IOException
    {
        AnnotationFeature neFeature = createFeature("value", "value", "Named entity type",
                CAS.TYPE_NAME_STRING, aTagset, aProject);

        AnnotationLayer neLayer = new AnnotationLayer(NamedEntity.class.getName(), "Named Entity",
                SPAN_TYPE, aProject, true);
        neLayer.setAllowStacking(true);
        neLayer.setMultipleTokens(true);
        neLayer.setLockToTokenOffset(false);
        annotationSchemaService.createLayer(neLayer);

        neFeature.setLayer(neLayer);
    }

    private void createChunkLayer(Project aProject)
        throws IOException
    {
        AnnotationLayer chunkLayer = new AnnotationLayer(Chunk.class.getName(), "Chunk", SPAN_TYPE,
                aProject, true);
        chunkLayer.setAllowStacking(false);
        chunkLayer.setMultipleTokens(true);
        chunkLayer.setLockToTokenOffset(false);
        annotationSchemaService.createLayer(chunkLayer);

        AnnotationFeature chunkValueFeature = new AnnotationFeature();
        chunkValueFeature.setDescription("Chunk tag");
        chunkValueFeature.setName("chunkValue");
        chunkValueFeature.setType(CAS.TYPE_NAME_STRING);
        chunkValueFeature.setProject(aProject);
        chunkValueFeature.setUiName("Tag");
        chunkValueFeature.setLayer(chunkLayer);
        annotationSchemaService.createFeature(chunkValueFeature);
    }

    private void createSurfaceFormLayer(Project aProject)
        throws IOException
    {
        AnnotationLayer surfaceFormLayer = new AnnotationLayer(SurfaceForm.class.getName(),
                "Surface form", SPAN_TYPE, aProject, true);
        surfaceFormLayer.setAllowStacking(false);
        // The surface form must be locked to tokens for CoNLL-U writer to work properly
        surfaceFormLayer.setLockToTokenOffset(false);
        surfaceFormLayer.setMultipleTokens(true);
        annotationSchemaService.createLayer(surfaceFormLayer);

        AnnotationFeature surfaceFormValueFeature = new AnnotationFeature();
        surfaceFormValueFeature.setDescription("Original surface text");
        surfaceFormValueFeature.setName("value");
        surfaceFormValueFeature.setType(CAS.TYPE_NAME_STRING);
        surfaceFormValueFeature.setProject(aProject);
        surfaceFormValueFeature.setUiName("Form");
        surfaceFormValueFeature.setLayer(surfaceFormLayer);
        annotationSchemaService.createFeature(surfaceFormValueFeature);
    }

    private void createDepLayer(Project aProject, TagSet aTagset)
        throws IOException
    {
        // Dependency Layer
        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RELATION_TYPE, aProject, true);
        AnnotationLayer tokenLayer = annotationSchemaService.getLayer(Token.class.getName(),
                aProject);
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

        annotationSchemaService.createLayer(depLayer);

        AnnotationFeature featRel = createFeature("DependencyType", "Relation",
                "Dependency relation", CAS.TYPE_NAME_STRING, aTagset, aProject);
        featRel.setLayer(depLayer);
        
        String[] flavors = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        String[] flavorDesc = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        TagSet flavorsTagset = annotationSchemaService.createTagSet("Dependency flavors",
                "Dependency flavors", "mul", flavors, flavorDesc, aProject);
        AnnotationFeature featFlavor = createFeature("flavor", "Flavor",
                "Dependency relation", CAS.TYPE_NAME_STRING, flavorsTagset, aProject);
        featFlavor.setLayer(depLayer);
    }

    private void createPOSLayer(Project aProject, TagSet aPosTagset)
        throws IOException
    {
        AnnotationLayer tokenLayer = annotationSchemaService.getLayer(Token.class.getName(),
                aProject);
        
        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE,
                aProject, true);
        AnnotationFeature tokenPosFeature = createFeature("pos", "pos", aProject, tokenLayer,
                POS.class.getName());
        tokenPosFeature.setVisible(true);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        annotationSchemaService.createLayer(posLayer);

        AnnotationFeature posFeature = createFeature("PosValue", "PosValue", "Part-of-speech tag",
                CAS.TYPE_NAME_STRING, aPosTagset, aProject);
        posFeature.setLayer(posLayer);
    }
    
    private AnnotationLayer createSemPredLayer(Project aProject)
            throws IOException
    {
        AnnotationLayer semPredLayer = new AnnotationLayer(SemPred.class.getName(), "SemPred",
                SPAN_TYPE, aProject, true);
        semPredLayer.setAllowStacking(true);
        semPredLayer.setCrossSentence(false);
        semPredLayer.setLockToTokenOffset(false);
        semPredLayer.setMultipleTokens(true);
        
        AnnotationFeature semPredCategoryFeature = createFeature("category", "category",
                "Category of the semantic predicate, e.g. the frame identifier.",
                CAS.TYPE_NAME_STRING, null, aProject);
        semPredCategoryFeature.setLayer(semPredLayer);
        
        AnnotationFeature semPredArgumentsFeature = new AnnotationFeature();
        semPredArgumentsFeature.setName("arguments");
        semPredArgumentsFeature.setUiName("arguments");
        semPredArgumentsFeature.setDescription("Arguments of the semantic predicate");
        semPredArgumentsFeature.setType(SemArg.class.getName());
        semPredArgumentsFeature.setProject(aProject);
        semPredArgumentsFeature.setTagset(null);
        semPredArgumentsFeature.setMode(MultiValueMode.ARRAY);
        semPredArgumentsFeature.setLinkMode(LinkMode.WITH_ROLE);
        semPredArgumentsFeature.setLinkTypeName(SemArgLink.class.getName());
        semPredArgumentsFeature.setLinkTypeRoleFeatureName("role");
        semPredArgumentsFeature.setLinkTypeTargetFeatureName("target");
        semPredArgumentsFeature.setLayer(semPredLayer);
        annotationSchemaService.createFeature(semPredArgumentsFeature);
        
        annotationSchemaService.createLayer(semPredLayer);
        return semPredLayer;
    }

    private AnnotationLayer createSemArgLayer(Project aProject)
            throws IOException
    {
        AnnotationLayer semArgLayer = new AnnotationLayer(SemArg.class.getName(), "SemArg",
                SPAN_TYPE, aProject, true);
        semArgLayer.setAllowStacking(true);
        semArgLayer.setCrossSentence(false);
        semArgLayer.setLockToTokenOffset(false);
        semArgLayer.setMultipleTokens(true);
        
        annotationSchemaService.createLayer(semArgLayer);
        return semArgLayer;
    }

    private AnnotationLayer createTokenLayer(Project aProject)
        throws IOException
    {
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE,
                aProject, true);

        annotationSchemaService.createLayer(tokenLayer);
        return tokenLayer;
    }
    
    private AnnotationFeature createFeature(String aName, String aUiname, Project aProject,
            AnnotationLayer aLayer, String aType)
    {
        AnnotationFeature feature = new AnnotationFeature();
        feature.setName(aName);
        feature.setEnabled(true);
        feature.setType(aType);
        feature.setUiName(aUiname);
        feature.setLayer(aLayer);
        feature.setProject(aProject);

        annotationSchemaService.createFeature(feature);
        return feature;
    }
    
    private AnnotationFeature createFeature(String aName, String aUiName, String aDescription,
            String aType, TagSet aTagSet, Project aProject)
        throws IOException
    {
        AnnotationFeature feature = new AnnotationFeature();
        feature.setDescription(aDescription);
        feature.setName(aName);
        feature.setType(aType);
        feature.setProject(aProject);
        feature.setUiName(aUiName);
        feature.setTagset(aTagSet);        
        annotationSchemaService.createFeature(feature);
        
        return feature;
    }
}
