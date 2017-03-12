/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.uima.cas.CAS;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.Logging;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;

/**
 * Implementation of methods defined in the {@link AnnotationService} interface
 */
public class AnnotationServiceImpl
    implements AnnotationService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${webanno.repository}")
    private File dir;

    @PersistenceContext
    private EntityManager entityManager;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    public AnnotationServiceImpl()
    {

    }

    @Override
    @Transactional
    public void createTag(Tag aTag)
        throws IOException
    {
        entityManager.persist(aTag);

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aTag.getTagSet().getProject().getId()))) {
            TagSet tagset = aTag.getTagSet();
            Project project = tagset.getProject();
            log.info("Created tag [{}]({}) in tagset [{}]({}) in project [{}]({})", aTag.getName(),
                    aTag.getId(), tagset.getName(), tagset.getId(), project.getName(),
                    project.getId());
        }
    }

    @Override
    @Transactional
    public void createTagSet(TagSet aTagSet)
        throws IOException
    {

        if (aTagSet.getId() == 0) {
            entityManager.persist(aTagSet);
        }
        else {
            entityManager.merge(aTagSet);
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aTagSet.getProject().getId()))) {
            Project project = aTagSet.getProject();
            log.info("Created tagset [{}]({}) in project [{}]({})", aTagSet.getName(),
                    aTagSet.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void createLayer(AnnotationLayer aLayer)
        throws IOException
    {
        if (aLayer.getId() == 0) {
            entityManager.persist(aLayer);
        }
        else {
            entityManager.merge(aLayer);
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aLayer.getProject().getId()))) {
            Project project = aLayer.getProject();
            log.info("Created layer [{}]({}) in project [{}]({})", aLayer.getName(),
                    aLayer.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void createFeature(AnnotationFeature aFeature)
    {
        if (aFeature.getId() == 0) {
            entityManager.persist(aFeature);
        }
        else {
            entityManager.merge(aFeature);
        }
    }

    @Override
    @Transactional
    public Tag getTag(String aTagName, TagSet aTagSet)
    {
        return entityManager
                .createQuery("FROM Tag WHERE name = :name AND" + " tagSet =:tagSet", Tag.class)
                .setParameter("name", aTagName).setParameter("tagSet", aTagSet).getSingleResult();
    }

    @Override
    public boolean existsTag(String aTagName, TagSet aTagSet)
    {

        try {
            getTag(aTagName, aTagSet);
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsTagSet(String aName, Project aProject)
    {
        try {
            entityManager
                    .createQuery("FROM TagSet WHERE name = :name AND project = :project",
                            TagSet.class).setParameter("name", aName)
                    .setParameter("project", aProject).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsTagSet(Project aProject)
    {
        try {
            entityManager.createQuery("FROM TagSet WHERE  project = :project", TagSet.class)
                    .setParameter("project", aProject).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsLayer(String aName, String aType, Project aProject)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM AnnotationLayer WHERE name = :name AND type = :type AND project = :project",
                            AnnotationLayer.class).setParameter("name", aName)
                    .setParameter("type", aType).setParameter("project", aProject)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    public boolean existsFeature(String aName, AnnotationLayer aLayer)
    {

        try {
            entityManager
                    .createQuery("FROM AnnotationFeature WHERE name = :name AND layer = :layer",
                            AnnotationFeature.class).setParameter("name", aName)
                    .setParameter("layer", aLayer).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional
    public TagSet getTagSet(String aName, Project aProject)
    {
        return entityManager
                .createQuery("FROM TagSet WHERE name = :name AND project =:project", TagSet.class)
                .setParameter("name", aName).setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public TagSet getTagSet(long aId)
    {
        return entityManager.createQuery("FROM TagSet WHERE id = :id", TagSet.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional
    public AnnotationLayer getLayer(long aId)
    {
        return entityManager
                .createQuery("FROM AnnotationLayer WHERE id = :id", AnnotationLayer.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationLayer getLayer(String aName, Project aProject)
    {
        return entityManager
                .createQuery("From AnnotationLayer where name = :name AND project =:project",
                        AnnotationLayer.class).setParameter("name", aName)
                .setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationFeature getFeature(long aId)
    {
        return entityManager
                .createQuery("From AnnotationFeature where id = :id", AnnotationFeature.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationFeature getFeature(String aName, AnnotationLayer aLayer)
    {
        return entityManager
                .createQuery("From AnnotationFeature where name = :name AND layer = :layer",
                        AnnotationFeature.class).setParameter("name", aName)
                .setParameter("layer", aLayer).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsType(String aName, String aType)
    {
        try {
            entityManager
                    .createQuery("From AnnotationLayer where name = :name AND type = :type",
                            AnnotationLayer.class).setParameter("name", aName)
                    .setParameter("type", aType).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public TagSet createTagSet(String aDescription, String aTagSetName, String aLanguage,
            String[] aTags, String[] aTagDescription, Project aProject)
                throws IOException
    {
        TagSet tagSet = new TagSet();
        tagSet.setDescription(aDescription);
        tagSet.setLanguage(aLanguage);
        tagSet.setName(aTagSetName);
        tagSet.setProject(aProject);

        createTagSet(tagSet);

        int i = 0;
        for (String tagName : aTags) {
            Tag tag = new Tag();
            tag.setTagSet(tagSet);
            tag.setDescription(aTagDescription[i]);
            tag.setName(tagName);
            createTag(tag);
            i++;
        }
        
        return tagSet;
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
        createFeature(feature);
        
        return feature;
    }

    @Override
    @Transactional
    public void initializeTypesForProject(Project aProject, String[] aPostags, String[] aPosTagDescriptions,
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
        String[] posTagDescriptions = aPosTagDescriptions.length == posTags.length ? aPosTagDescriptions
                : new String[] {
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

        TagSet posFeatureTagset = createTagSet(
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
        String[] depTagsDescription = aDepTagDescriptions.length == depTags.length ? aDepTagDescriptions
                : depTags;
        TagSet deFeatureTagset = createTagSet("Dependency annotation", "Tiger", "de", depTags,
                depTagsDescription, aProject);
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
        TagSet neFeatureTagset = createTagSet("Named Entity annotation", "NER_WebAnno", "de",
                neTags, neTagDescriptions, aProject);
        createNeLayer(aProject, neFeatureTagset);

        // Coref Layer
        TagSet corefTypeFeatureTagset = createTagSet("coreference type annotation", "BART", "de",
                aCorefTypeTags.length > 0 ? aCorefTypeTags : new String[] { "nam" },
                aCorefTypeTags.length > 0 ? aCorefTypeTags : new String[] { "nam" }, aProject);
        TagSet corefRelFeatureTagset = createTagSet("coreference relation annotation", "TuebaDZ",
                "de", aCorefRelTags.length > 0 ? aCorefRelTags : new String[] { "anaphoric" },
                aCorefRelTags.length > 0 ? aCorefRelTags : new String[] { "anaphoric" }, aProject);
        createCorefLayer(aProject, corefTypeFeatureTagset, corefRelFeatureTagset);

        createLemmaLayer(aProject);

        createChunkLayer(aProject);
    }

    @Override
    @Transactional
    public void initializeTypesForProject(Project aProject)
        throws IOException
    {
        // Default layers with default tagsets
        createTokenLayer(aProject);

        TagSet posTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-pos-ud.json").getInputStream(), this);
        createPOSLayer(aProject, posTagSet);

        TagSet depTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-dep-ud.json").getInputStream(), this);
        createDepLayer(aProject, depTagSet);

        TagSet nerTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-ne-webanno.json").getInputStream(), this);
        createNeLayer(aProject, nerTagSet);

        TagSet corefTypeTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-coref-type-bart.json").getInputStream(), this);
        TagSet corefRelTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-coref-rel-tuebadz.json").getInputStream(), this);
        createCorefLayer(aProject, corefTypeTagSet, corefRelTagSet);

        createLemmaLayer(aProject);

        createChunkLayer(aProject);

        createSurfaceFormLayer(aProject);

        // Extra tagsets
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-pos-stts.json").getInputStream(), this);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-dep-tiger.json").getInputStream(), this);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/en-dep-sd.json").getInputStream(), this);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/en-pos-ptb-tt.json").getInputStream(), this);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-pos-upos.json").getInputStream(), this);
    }
    
    private void createLemmaLayer(Project aProject)
        throws IOException
    {
        AnnotationLayer tokenLayer = getLayer(Token.class.getName(), aProject);
        AnnotationFeature tokenLemmaFeature = createFeature("lemma", "lemma", aProject, tokenLayer,
                Lemma.class.getName());
        tokenLemmaFeature.setVisible(true);

        AnnotationLayer lemmaLayer = new AnnotationLayer(Lemma.class.getName(), "Lemma", SPAN_TYPE,
                aProject, true);
        lemmaLayer.setAttachType(tokenLayer);
        lemmaLayer.setAttachFeature(tokenLemmaFeature);
        createLayer(lemmaLayer);

        AnnotationFeature lemmaFeature = new AnnotationFeature();
        lemmaFeature.setDescription("lemma Annotation");
        lemmaFeature.setName("value");
        lemmaFeature.setType(CAS.TYPE_NAME_STRING);
        lemmaFeature.setProject(aProject);
        lemmaFeature.setUiName("Lemma value");
        lemmaFeature.setLayer(lemmaLayer);
        createFeature(lemmaFeature);
    }

    private AnnotationLayer createCorefLayer(Project aProject, TagSet aCorefTypeTags, TagSet aCorefRelTags)
                throws IOException
    {
        AnnotationLayer base = new AnnotationLayer(
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", "Coreference",
                CHAIN_TYPE, aProject, true);
        base.setCrossSentence(true);
        base.setAllowStacking(true);
        base.setMultipleTokens(true);
        base.setLockToTokenOffset(false);
        createLayer(base);

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
        createLayer(neLayer);

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
        createLayer(chunkLayer);

        AnnotationFeature chunkValueFeature = new AnnotationFeature();
        chunkValueFeature.setDescription("Chunk tag");
        chunkValueFeature.setName("chunkValue");
        chunkValueFeature.setType(CAS.TYPE_NAME_STRING);
        chunkValueFeature.setProject(aProject);
        chunkValueFeature.setUiName("Tag");
        chunkValueFeature.setLayer(chunkLayer);
        createFeature(chunkValueFeature);
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
        createLayer(surfaceFormLayer);

        AnnotationFeature surfaceFormValueFeature = new AnnotationFeature();
        surfaceFormValueFeature.setDescription("Original surface text");
        surfaceFormValueFeature.setName("value");
        surfaceFormValueFeature.setType(CAS.TYPE_NAME_STRING);
        surfaceFormValueFeature.setProject(aProject);
        surfaceFormValueFeature.setUiName("Form");
        surfaceFormValueFeature.setLayer(surfaceFormLayer);
        createFeature(surfaceFormValueFeature);
    }

    private void createDepLayer(Project aProject, TagSet aTagset)
        throws IOException
    {
        // Dependency Layer
        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RELATION_TYPE, aProject, true);
        AnnotationLayer tokenLayer = getLayer(Token.class.getName(), aProject);
        List<AnnotationFeature> tokenFeatures = listAnnotationFeature(tokenLayer);
        AnnotationFeature tokenPosFeature = null;
        for (AnnotationFeature feature : tokenFeatures) {
            if (feature.getName().equals("pos")) {
                tokenPosFeature = feature;
                break;
            }
        }
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenPosFeature);

        createLayer(depLayer);

        AnnotationFeature featRel = createFeature("DependencyType", "Relation",
                "Dependency relation", CAS.TYPE_NAME_STRING, aTagset, aProject);
        featRel.setLayer(depLayer);
        
        String[] flavors = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        String[] flavorDesc = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        TagSet flavorsTagset = createTagSet("Dependency flavors", "Dependency flavors", "mul",
                flavors, flavorDesc, aProject);
        AnnotationFeature featFlavor = createFeature("flavor", "Flavor",
                "Dependency relation", CAS.TYPE_NAME_STRING, flavorsTagset, aProject);
        featFlavor.setLayer(depLayer);
    }

    private void createPOSLayer(Project aProject, TagSet aPosTagset)
        throws IOException
    {
        AnnotationLayer tokenLayer = getLayer(Token.class.getName(), aProject);
        
        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE,
                aProject, true);
        AnnotationFeature tokenPosFeature = createFeature("pos", "pos", aProject, tokenLayer,
                POS.class.getName());
        tokenPosFeature.setVisible(true);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        createLayer(posLayer);

        AnnotationFeature posFeature = createFeature("PosValue", "PosValue", "Part-of-speech tag",
                CAS.TYPE_NAME_STRING, aPosTagset, aProject);
        posFeature.setLayer(posLayer);
    }

    private AnnotationLayer createTokenLayer(Project aProject)
        throws IOException
    {
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE,
                aProject, true);

        createLayer(tokenLayer);
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

        createFeature(feature);
        return feature;
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAnnotationType()
    {
        return entityManager.createQuery("FROM AnnotationLayer ORDER BY name",
                AnnotationLayer.class).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAnnotationLayer(Project aProject)
    {
        return entityManager
                .createQuery("FROM AnnotationLayer WHERE project =:project ORDER BY uiName",
                        AnnotationLayer.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAttachedRelationLayers(AnnotationLayer aLayer)
    {
        return entityManager
                .createQuery(
                        "SELECT l FROM AnnotationLayer l LEFT JOIN l.attachFeature f "
                        + "WHERE l.type = :type AND "
                        + "(l.attachType = :attachType OR f.type = :attachTypeName) "
                        + "ORDER BY l.uiName",
                        AnnotationLayer.class).setParameter("type", RELATION_TYPE)
                .setParameter("attachType", aLayer)
                .setParameter("attachTypeName", aLayer.getName()).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAttachedLinkFeatures(AnnotationLayer aLayer)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationFeature WHERE linkMode in (:modes) AND project = :project AND "
                                + "type in (:attachType) ORDER BY uiName", AnnotationFeature.class)
                .setParameter("modes", asList(LinkMode.SIMPLE, LinkMode.WITH_ROLE))
                .setParameter("attachType", asList(aLayer.getName(), CAS.TYPE_NAME_ANNOTATION))
                // Checking for project is necessary because type match is string-based
                .setParameter("project", aLayer.getProject()).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAnnotationFeature(AnnotationLayer aLayer)
    {
        if (aLayer == null || aLayer.getId() == 0) {
            return new ArrayList<AnnotationFeature>();
        }

        return entityManager
                .createQuery("FROM AnnotationFeature  WHERE layer =:layer ORDER BY uiName",
                        AnnotationFeature.class).setParameter("layer", aLayer).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAnnotationFeature(Project aProject)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationFeature f WHERE project =:project ORDER BY f.layer.uiName, f.uiName",
                        AnnotationFeature.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public List<Tag> listTags()
    {
        return entityManager.createQuery("From Tag ORDER BY name", Tag.class).getResultList();
    }

    @Override
    @Transactional
    public List<Tag> listTags(TagSet aTagSet)
    {
        List<Tag> tags = entityManager
                .createQuery("FROM Tag WHERE tagSet = :tagSet ORDER BY name ASC", Tag.class)
                .setParameter("tagSet", aTagSet).getResultList();
        // FIXME ?!? This loop appears to make absolutely not sense!
        for (int i = 0; i < tags.size(); i++) {
            tags.get(i).setName(tags.get(i).getName());
        }
        return tags;
    }

    @Override
    @Transactional
    public List<TagSet> listTagSets()
    {
        return entityManager.createQuery("FROM TagSet ORDER BY name ASC", TagSet.class)
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<TagSet> listTagSets(Project aProject)
    {
        return entityManager
                .createQuery("FROM TagSet where project = :project ORDER BY name ASC", TagSet.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public void removeTag(Tag aTag)
    {
        entityManager.remove(aTag);
    }

    @Override
    @Transactional
    public void removeTagSet(TagSet aTagSet)
    {
        for (Tag tag : listTags(aTagSet)) {
            entityManager.remove(tag);
        }
        entityManager.remove(aTagSet);
    }

    @Override
    @Transactional
    public void removeAnnotationFeature(AnnotationFeature aFeature)
    {
        entityManager.remove(aFeature);
    }

    @Override
    @Transactional
    public void removeAnnotationLayer(AnnotationLayer aLayer)
    {
        entityManager.remove(aLayer);
    }

	@Override
	@Transactional
	public void removeAllTags(TagSet aTagSet) {
		for (Tag tag : listTags(aTagSet)) {
			entityManager.remove(tag);
		}
	}
}
