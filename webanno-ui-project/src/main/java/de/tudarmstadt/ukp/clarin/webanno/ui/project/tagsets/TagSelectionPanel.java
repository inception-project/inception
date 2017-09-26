package de.tudarmstadt.ukp.clarin.webanno.ui.project.tagsets;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

public class TagSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private OverviewListChoice<Tag> overviewList;
    private IModel<TagSet> selectedTagSet;

    public TagSelectionPanel(String id, IModel<TagSet> aTagset, IModel<Tag> aTag)
    {
        super(id, aTagset);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        selectedTagSet = aTagset;

        overviewList = new OverviewListChoice<>("tag");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(aTag);
        overviewList.setChoices(LambdaModel.of(this::listTags));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        add(new LambdaAjaxLink("create", this::actionCreate));
    }

    private List<Tag> listTags()
    {
        if (selectedTagSet.getObject() != null) {
            return annotationSchemaService.listTags(selectedTagSet.getObject());
        }
        else {
            return Collections.emptyList();
        }
    }
}
