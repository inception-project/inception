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
 *
 */
@Component
public class FactFeatureSupport implements FeatureSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());


    public static final String PREFIX = "Fact";
    public static final String SUBJECT = "Subject";
    public static final String PREDICATE = "Predicate";
    public static final String OBJECT = "Object";

    private final static List<FeatureType> FEATURE_TYPES = Arrays.asList(
        new FeatureType(PREFIX + "." + SUBJECT, PREFIX + ":" + SUBJECT),
        new FeatureType(PREFIX + "." + PREDICATE, PREFIX + ":" + PREDICATE),
        new FeatureType(PREFIX + "." + OBJECT, PREFIX + ":" + OBJECT)
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
                    case PREFIX + "." + SUBJECT:
                    case PREFIX + "." + PREDICATE:
                    case PREFIX + "." + OBJECT:
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
                    case PREFIX + "." + SUBJECT: {
                        editor = new FactFeatureSubjectEditor(aId, aOwner, aFeatureStateModel);
                        logger.debug("created fact editor:" + PREFIX + "." + SUBJECT);
                        break;
                    }
                    case PREFIX + "." + PREDICATE: {
                        editor = new FaceFeaturePredicateEditor(aId, aOwner, aFeatureStateModel);
                        logger.debug("created fact editor:" + PREFIX + "." + PREDICATE);
                        break;
                    }
                    case PREFIX + "." + OBJECT: {
                        editor = new FactFeatureSubjectEditor(aId, aOwner, aFeatureStateModel);
                        logger.debug("created fact editor:" + PREFIX + "." + OBJECT);
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
