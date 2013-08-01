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
package de.tudarmstadt.ukp.clarin.webanno.webapp.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

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

        entityManager.persist(aTagSet);

        RepositoryServiceDbData.createLog(aTagSet.getProject(), aUser.getUsername()).info(
                " Added tagset  [" + aTagSet.getName() + "] with ID [" + aTagSet.getId() + "]");
        RepositoryServiceDbData.createLog(aTagSet.getProject(), aUser.getUsername())
                .removeAllAppenders();
    }

    @Override
    @Transactional
    public void createType(AnnotationType aType)
    {
        if (aType.getId() != 0) {
            throw new IllegalArgumentException("Type already exists");
        }

        entityManager.persist(aType);
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
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existTagSet(AnnotationType aType, Project aProject)
    {
        try {
            entityManager
                    .createQuery("FROM TagSet WHERE type = :type AND project = :project",
                            TagSet.class).setParameter("type", aType)
                    .setParameter("project", aProject).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional
    public TagSet getTagSet(AnnotationType aType, Project aProject)
    {
        return entityManager
                .createQuery("FROM TagSet WHERE type = :type AND project =:project", TagSet.class)
                .setParameter("type", aType).setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public TagSet getTagSet(long aId)
    {
        return entityManager.createQuery("FROM TagSet WHERE id = :id", TagSet.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationType getType(String aName, String aType)
    {
        return entityManager
                .createQuery("From AnnotationType where name = :name AND type = :type",
                        AnnotationType.class).setParameter("name", aName)
                .setParameter("type", aType).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsType(String aName, String aType)
    {
        try {
            entityManager
                    .createQuery("From AnnotationType where name = :name AND type = :type",
                            AnnotationType.class).setParameter("name", aName)
                    .setParameter("type", aType).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    private void initializeType(String aName, String aDescription, String aType,
            String aTagSetName, String aLanguage, String[] aTags, String[] aTagDescription,
            Project aProject, User aUser)
        throws IOException
    {
        AnnotationType type = null;

        if (!existsType(aName, aType)) {
            type = new AnnotationType();
            type.setDescription(aDescription);
            type.setName(aName);
            type.setType(aType);
            createType(type);
        }
        else {
            type = getType(aName, aType);
        }
        TagSet tagSet = new TagSet();
        tagSet.setDescription(aDescription);
        tagSet.setLanguage(aLanguage);
        tagSet.setName(aTagSetName);
        tagSet.setType(type);
        tagSet.setProject(aProject);
        createTagSet(tagSet, aUser);

        int i = 0;
        for (String tagName : aTags) {
            Tag tag = new Tag();
            tag.setTagSet(tagSet);
            tag.setDescription(aTagDescription[i]);
            tag.setName(tagName);
            createTag(tag, aUser);
            i++;
        }
    }

    @Override
    @Transactional
    public void initializeTypesForProject(Project aProject, User aUser)
        throws IOException
    {

        String[] posTags = new String[] { "$(", "$,", "$.", "ADJA", "ADJD", "ADV", "APPO", "APPR",
                "APPRART", "APZR", "ART", "CARD", "FM", "ITJ", "KOKOM", "KON", "KOUI", "KOUS",
                "NE", "NN", "PAV", "PDAT", "PDS", "PIAT", "PIDAT", "PIS", "PPER", "PPOSAT",
                "PPOSS", "PRELAT", "PRELS", "PRF", "PROAV", "PTKA", "PTKANT", "PTKNEG", "PTKVZ",
                "PTKZU", "PWAT", "PWAV", "PWS", "TRUNC", "VAFIN", "VAIMP", "VAINF", "VAPP",
                "VMFIN", "VMINF", "VMPP", "VVFIN", "VVIMP", "VVINF", "VVIZU", "VVPP", "XY", "--" };
        String[] posTagDescriptions = new String[] {
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
                "Imperativ, aux \nBsp: sei [ruhig !]  ", "Infinitiv, aux \nBsp:werden, sein  ",
                "Partizip Perfekt, aux \nBsp: gewesen ", "finites Verb, modal \nBsp: dürfen  ",
                "Infinitiv, modal \nBsp: wollen ",
                "Partizip Perfekt, modal \nBsp: gekonnt, [er hat gehen] können ",
                "finites Verb, voll \nBsp: [du] gehst, [wir] kommen [an]   ",
                "Imperativ, voll \nBsp: komm [!] ", "Infinitiv, voll \nBsp: gehen, ankommen",
                "Infinitiv mit ``zu'', voll \nBsp: anzukommen, loszulassen ",
                "Partizip Perfekt, voll \nBsp:gegangen, angekommen ",
                "Nichtwort, Sonderzeichen enthaltend \nBsp:3:7, H2O, D2XW3", "--" };

        initializeType(
                de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.POS,
                "Stuttgart-Tübingen-Tag-Set \nGerman Part of Speech tagset "
                        + "STTS Tag Table (1995/1999): "
                        + "http://www.ims.uni-stuttgart.de/projekte/corplex/TagSets/stts-table.html",
                "span", "STTS", "de", posTags, posTagDescriptions, aProject, aUser);

        String[] depTags = new String[] { "ADV", "APP", "ATTR", "AUX", "AVZ", "CJ", "DET", "ETH",
                "EXPL", "GMOD", "GRAD", "KOM", "KON", "KONJ", "NEB", "OBJA", "OBJA2", "OBJA3",
                "OBJC", "OBJC2", "OBJC3", "OBJD", "OBJD2", "OBJD3", "OBJG", "OBJG2", "OBJG3",
                "OBJI", "OBJI2", "OBJI3", "OBJP", "OBJP2", "OBJP3", "PAR", "PART", "PN", "PP",
                "PRED", "-PUNCT-", "REL", "ROOT", "S", "SUBJ", "SUBJ2", "SUBJ3", "SUBJC", "SUBJC2",
                "SUBJC3", "SUBJI", "SUBJI2", "CP", "PD", "RE", "CD", "DA", "SVP", "OP", "MO", "JU",
                "CVC", "NG", "SB", "SBP", "AG", "PM", "OCRC", "OG", "SUBJI3", "VOK", "ZEIT", "$",
                "--", "OC", "OA", "MNR", "NK", "RC", "EP", "CC", "CM", "UC", "AC", "PNC" };
        initializeType(
                de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.DEPENDENCY,
                "Dependency annotation", "relation", "Tiger", "de", depTags, depTags, aProject,
                aUser);

        initializeType(
                de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.NAMEDENTITY,
                "Named Entity annotation", "span", "NER_WebAnno", "de", new String[] { "PER",
                        "PERderiv", "PERpart", "LOC", "LOCderiv", "LOCpart", "ORG", "ORGderiv",
                        "ORGpart", "OTH", "OTHderiv", "OTHpart" }, new String[] { "Person",
                        "Person derivative", "Hyphenated part  is person", "Location derivatives",
                        "Location derivative", "Hyphenated part  is location", "Organization",
                        "Organization derivative", "Hyphenated part  is organization",
                        "Other: Every name that is not a location, person or organisation",
                        "Other derivative", "Hyphenated part  is Other" }, aProject, aUser);

        initializeType(
                de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.COREFRELTYPE,
                "coreference type annotation", "span", "BART", "de", new String[] { "nam" },
                new String[] { "nam" }, aProject, aUser);

        initializeType(
                de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.COREFERENCE,
                "coreference annotation", "relation", "TuebaDZ", "de",
                new String[] { "anaphoric" }, new String[] { "anaphoric" }, aProject, aUser);
    }

    @Override
    @Transactional
    public List<AnnotationType> listAnnotationType()
    {
        return entityManager.createQuery("FROM AnnotationType ORDER BY name", AnnotationType.class)
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationType> listAnnotationType(Project aProject)
    {
        List<TagSet> tagSets = listTagSets(aProject);
        List<AnnotationType> annotationTypes = new ArrayList<AnnotationType>();
        for (TagSet tagSet : tagSets) {
            annotationTypes.add(tagSet.getType());
        }
        return annotationTypes;
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
        for(int i=0;i<tags.size();i++){
            tags.get(i).setName(tags.get(i).getName().toUpperCase());
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

}
