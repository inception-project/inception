package de.tudarmstadt.ukp.inception.kb.feature;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

/**
 *  To create 3 kinds of editors based on the feature's type:
 *      Fact:subject, Fact:predicate, Fact:object
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class FactFeatureSupport implements FeatureSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    private KnowledgeBaseService kbService;


    private static final String PREFIX = "Fact";
    private static final String SUBJECT = "subject";
    private static final String PREDICATE = "predicate";
    private static final String OBJECT = "object";
    private static final String CONTACT = ":";

    private static final String SUBJECT_KEY = PREFIX + CONTACT + SUBJECT;
    private static final String PREDICATE_KEY = PREFIX + CONTACT + PREDICATE;
    private static final String OBJECT_KEY = PREFIX + CONTACT + OBJECT;


    private final static List<FeatureType> FEATURE_TYPES = Arrays.asList(
        new FeatureType(SUBJECT_KEY, SUBJECT_KEY),
        new FeatureType(PREDICATE_KEY, PREDICATE_KEY),
        new FeatureType(OBJECT_KEY, OBJECT_KEY)
    );

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer) {
        return FEATURE_TYPES;
    }

    @Override
    public boolean accepts(AnnotationFeature annotationFeature) {
        switch (annotationFeature.getMultiValueMode()) {
            case NONE:
                switch (annotationFeature.getType()) {
                    case SUBJECT_KEY:
                    case PREDICATE_KEY:
                    case OBJECT_KEY:
                        return true;
                    default:
                        return false;
                }
            case ARRAY:
            default:
                return false;
        }
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, AnnotationFS aFs,
                                     Feature aLabelFeature)
    {
        try {
            String value = aFs.getFeatureValueAsString(aLabelFeature);
            String renderValue = null;
            if (value != null) {
                // FIXME Since this might be called very often during rendering, it *might* be
                // worth to set up an LRU cache instead of relying on the performance of the
                // underlying KB store.
                renderValue = kbService.getKnowledgeBases(aFeature.getProject())
                    .stream()
                    .map(k -> kbService.readProperty(k, value))
                    .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                    .map(KBProperty::getUiLabel)
                    .findAny()
                    .orElseThrow(NoSuchElementException::new);
            }
            return renderValue;
        }
        catch (Exception e) {
            logger.error("Unable to render feature value", e);
            return "ERROR";
        }
    }

    @Override
    public void setFeatureValue(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        KBHandle kbProperty = (KBHandle) aValue;
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, kbProperty != null ? kbProperty.getIdentifier() : null);
    }

    @Override
    public KBHandle getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        String value = (String) FeatureSupport.super.getFeatureValue(aFeature, aFs);
        if (value != null) {
            KBProperty property = kbService.getKnowledgeBases(aFeature.getProject())
                .stream()
                .map(k -> kbService.readProperty(k, value))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException());
            return new KBHandle(property.getIdentifier(), property.getName());
        }
        else {
            return null;
        }
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
                                      IModel<AnnotatorState> aStateModel, IModel<FeatureState> aFeatureStateModel) {

        FeatureState featureState = aFeatureStateModel.getObject();
        final FeatureEditor editor;

        if (aFeatureStateModel.getObject().value == null) {
            aFeatureStateModel.setObject(new FeatureState(aFeatureStateModel.getObject().feature,
                new ArrayList<>()));
        }

        switch (featureState.feature.getMultiValueMode()) {
            case NONE:
                switch (featureState.feature.getType()) {
                    case SUBJECT_KEY: {
                        editor = new FactFeatureSubjectEditor(aId, aOwner, aHandler, aStateModel, aFeatureStateModel);
                        logger.debug("created fact editor:" + SUBJECT_KEY);
                        break;
                    }
                    case PREDICATE_KEY: {
                        editor = new FaceFeaturePredicateEditor(aId, aOwner, aHandler, aStateModel, aFeatureStateModel);
                        logger.debug("created fact editor:" + PREDICATE_KEY);
                        break;
                    }
                    case OBJECT_KEY: {
                        editor = new FaceFeatureObjectEditor(aId, aOwner, aHandler, aStateModel, aFeatureStateModel);
                        logger.debug("created fact editor:" + OBJECT_KEY);
                        break;
                    }
                    default:
                        throw unsupportedFeatureTypeException(featureState);
                }
                break;
            case ARRAY:
                switch (featureState.feature.getType()) {

                    default:
                        throw unsupportedLinkModeException(featureState);
                }
            default:
                throw unsupportedMultiValueModeException(featureState);
        }

        return editor;
    }

}
