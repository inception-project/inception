package de.tudarmstadt.ukp.inception.kb.feature;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 *  To create 3 kinds of editors based on the feature's type:
 *      Fact:subject, Fact:predicate, Fact:object
 */
@Component
public class FactFeatureSupport implements FeatureSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());


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
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
                                      IModel<AnnotatorState> aStateModel, IModel<FeatureState> aFeatureStateModel) {

        FeatureState featureState = aFeatureStateModel.getObject();
        final FeatureEditor editor;

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
                throw unsupportedLinkModeException(featureState);
            default:
                throw unsupportedMultiValueModeException(featureState);
        }

        return editor;
    }

}
