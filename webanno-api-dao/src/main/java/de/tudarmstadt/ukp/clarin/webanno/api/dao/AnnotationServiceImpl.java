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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.uima.cas.CAS;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Implementation of methods defined in the {@link AnnotationService} interface
 *
 * @author Seid Muhie Yimam
 *
 */
public class AnnotationServiceImpl
    implements AnnotationService
{

    @PersistenceContext
    private EntityManager entityManager;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    public AnnotationServiceImpl()
    {

    }

    @Override
    @Transactional
    public void createTag(Tag aTag, User aUser)
        throws IOException
    {
        entityManager.persist(aTag);

        RepositoryServiceDbData.createLog(aTag.getTagSet().getProject(), aUser.getUsername()).info(
                " Added tag [" + aTag.getName() + "] with ID [" + aTag.getId() + "] to TagSet ["
                        + aTag.getTagSet().getName() + "]");
        RepositoryServiceDbData.createLog(aTag.getTagSet().getProject(), aUser.getUsername())
                .removeAllAppenders();
    }

    @Override
    @Transactional
    public void createTagSet(TagSet aTagSet, User aUser)
        throws IOException
    {

        if (aTagSet.getId() == 0) {
            entityManager.persist(aTagSet);
        }
        else {
            entityManager.merge(aTagSet);
        }
        RepositoryServiceDbData.createLog(aTagSet.getProject(), aUser.getUsername()).info(
                " Added tagset [" + aTagSet.getName() + "] with ID [" + aTagSet.getId() + "]");
        RepositoryServiceDbData.createLog(aTagSet.getProject(), aUser.getUsername())
                .removeAllAppenders();
    }

    @Override
    @Transactional
    public void createLayer(AnnotationLayer aLayer, User aUser)
        throws IOException
    {
        if (aLayer.getId() == 0) {
            entityManager.persist(aLayer);
        }
        else {
            entityManager.merge(aLayer);
        }
        RepositoryServiceDbData.createLog(aLayer.getProject(), aUser.getUsername()).info(
                " Added layer [" + aLayer.getName() + "] with ID [" + aLayer.getId() + "]");
        RepositoryServiceDbData.createLog(aLayer.getProject(), aUser.getUsername())
                .removeAllAppenders();
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

    private AnnotationFeature createFeature(String aName, String aUiName, String aDescription,
            String aType, String aTagSetName, String aLanguage, String[] aTags,
            String[] aTagDescription, Project aProject, User aUser)
        throws IOException
    {
        AnnotationFeature feature = new AnnotationFeature();
        feature.setDescription(aDescription);
        feature.setName(aName);
        feature.setType(aType);
        feature.setProject(aProject);
        feature.setUiName(aUiName);

        createFeature(feature);

        TagSet tagSet = new TagSet();
        tagSet.setDescription(aDescription);
        tagSet.setLanguage(aLanguage);
        tagSet.setName(aTagSetName);
        tagSet.setProject(aProject);

        createTagSet(tagSet, aUser);
        feature.setTagset(tagSet);

        int i = 0;
        for (String tagName : aTags) {
            Tag tag = new Tag();
            tag.setTagSet(tagSet);
            tag.setDescription(aTagDescription[i]);
            tag.setName(tagName);
            createTag(tag, aUser);
            i++;
        }
        return feature;
    }

    @Override
    @Transactional
    public void initializeTypesForProject(Project aProject, User aUser, String[] aPostags,
            String[] aPosTagDescriptions, String[] aDepTags, String[] aDepTagDescriptions,
            String[] aNeTags, String[] aNeTagDescriptions, String[] aCorefTypeTags,
            String[] aCorefRelTags)
        throws IOException
    {

        createTokenLayer(aProject, aUser);

        createPOSLayer(aProject, aUser, aPostags, aPosTagDescriptions);

        createDepLayer(aProject, aUser, aDepTags, aDepTagDescriptions);

        createNeLayer(aProject, aUser, aNeTags, aNeTagDescriptions);

        createCorefLayer(aProject, aUser, aCorefTypeTags, aCorefRelTags);

        createLemmaLayer(aProject, aUser);
        
        createChunkLayer(aProject, aUser);
    }

    private void createLemmaLayer(Project aProject, User aUser)
        throws IOException
    {
        AnnotationLayer tokenLayer = getLayer(Token.class.getName(), aProject);
        AnnotationFeature tokenLemmaFeature = createFeature("lemma", "lemma", aProject, tokenLayer,
                Lemma.class.getName());
        tokenLemmaFeature.setVisible(true);
        
        AnnotationLayer lemmaLayer = new AnnotationLayer(Lemma.class.getName(), "Lemma", SPAN_TYPE, aProject, true);
        lemmaLayer.setAttachType(tokenLayer);
        lemmaLayer.setAttachFeature(tokenLemmaFeature);

        createLayer(lemmaLayer, aUser);

        AnnotationFeature lemmaFeature = new AnnotationFeature();
        lemmaFeature.setDescription("lemma Annotation");
        lemmaFeature.setName("value");
        lemmaFeature.setType(CAS.TYPE_NAME_STRING);
        lemmaFeature.setProject(aProject);
        lemmaFeature.setUiName("Lemma value");
        lemmaFeature.setLayer(lemmaLayer);
        createFeature(lemmaFeature);
    }

    private void createCorefLayer(Project aProject, User aUser, String[] aCorefTypeTags,
            String[] aCorefRelTags)
        throws IOException
    {
        // Coref Layer
        AnnotationFeature corefTypeFeature = createFeature("referenceType", "referenceType",
                "coreference type annotation",
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", "BART", "de",
                aCorefTypeTags.length > 0 ? aCorefTypeTags : new String[] { "nam" },
                aCorefTypeTags.length > 0 ? aCorefTypeTags : new String[] { "nam" }, aProject,
                aUser);

        AnnotationFeature corefRelFeature = createFeature("referenceRelation",
                "referenceRelation", "coreference relation annotation",
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", "TuebaDZ", "de",
                aCorefRelTags.length > 0 ? aCorefRelTags : new String[] { "anaphoric" },
                aCorefRelTags.length > 0 ? aCorefRelTags : new String[] { "anaphoric" }, aProject,
                aUser);

        AnnotationLayer base = new AnnotationLayer("de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", "Coreference", CHAIN_TYPE, aProject, true);
        base.setCrossSentence(true);
        base.setAllowStacking(true);
        base.setMultipleTokens(true);
        base.setLockToTokenOffset(false);

        createLayer(base, aUser);

        corefTypeFeature.setLayer(base);
        corefTypeFeature.setVisible(true);

        corefRelFeature.setLayer(base);
        corefRelFeature.setVisible(true);
    }

    private void createNeLayer(Project aProject, User aUser, String[] aNeTags,
            String[] aNeTagDescriptions)
        throws IOException
    {
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
        AnnotationFeature neFeature = createFeature("value", "value", "Named Entity annotation",
                CAS.TYPE_NAME_STRING, "NER_WebAnno", "de", neTags, neTagDescriptions, aProject,
                aUser);

        AnnotationLayer neLayer = new AnnotationLayer(NamedEntity.class.getName(), "Named Entity", SPAN_TYPE, aProject, true);
        neLayer.setAllowStacking(true);
        neLayer.setMultipleTokens(true);
        neLayer.setLockToTokenOffset(false);
        createLayer(neLayer, aUser);

        neFeature.setLayer(neLayer);
    }

    private void createChunkLayer(Project aProject, User aUser)
        throws IOException
    {
        AnnotationLayer chunkLayer = new AnnotationLayer(Chunk.class.getName(), "Chunk", SPAN_TYPE,
                aProject, true);
        chunkLayer.setAllowStacking(false);
        chunkLayer.setMultipleTokens(true);
        chunkLayer.setLockToTokenOffset(false);
        createLayer(chunkLayer, aUser);

        AnnotationFeature chunkValueFeature = new AnnotationFeature();
        chunkValueFeature.setDescription("Chunk tag");
        chunkValueFeature.setName("chunkValue");
        chunkValueFeature.setType(CAS.TYPE_NAME_STRING);
        chunkValueFeature.setProject(aProject);
        chunkValueFeature.setUiName("Tag");
        chunkValueFeature.setLayer(chunkLayer);
        createFeature(chunkValueFeature);
    }
    
    private void createDepLayer(Project aProject, User aUser, String[] aDepTags,
            String[] aDepTagDescriptions)
        throws IOException
    {
        // Dependency Layer
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
        AnnotationFeature deFeature = createFeature("DependencyType", "DependencyType",
                "Dependency annotation", CAS.TYPE_NAME_STRING, "Tiger", "de", depTags,
                depTagsDescription, aProject, aUser);

        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency", RELATION_TYPE, aProject, true);
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

        createLayer(depLayer, aUser);

        deFeature.setLayer(depLayer);
    }

    private void createPOSLayer(Project aProject, User aUser, String[] aPostags,
            String[] aPosTagDescriptions)
        throws IOException
    {
        // POS layer
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

        AnnotationFeature posFeature = createFeature(
                "PosValue",
                "PosValue",
                "Stuttgart-Tübingen-Tag-Set \nGerman Part of Speech tagset "
                        + "STTS Tag Table (1995/1999): "
                        + "http://www.ims.uni-stuttgart.de/projekte/corplex/TagSets/stts-table.html",
                CAS.TYPE_NAME_STRING, "STTS", "de", posTags, posTagDescriptions, aProject, aUser);

        AnnotationLayer tokenLayer = getLayer(Token.class.getName(), aProject);
        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE, aProject, true);
        AnnotationFeature tokenPosFeature = createFeature("pos", "pos", aProject, tokenLayer,
                POS.class.getName());
        tokenPosFeature.setVisible(true);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);

        createLayer(posLayer, aUser);

        posFeature.setLayer(posLayer);
    }

    private AnnotationLayer createTokenLayer(Project aProject, User aUser)
        throws IOException
    {
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE, aProject, true);

        createLayer(tokenLayer, aUser);
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
    public List<AnnotationFeature> listAnnotationFeature(AnnotationLayer aLayer)
    {
        if (aLayer.getId() == 0) {
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
}
