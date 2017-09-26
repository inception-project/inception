package de.tudarmstadt.ukp.clarin.webanno.ui.project.tagsets;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

public class TagSetSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private OverviewListChoice<TagSet> overviewList;
    
    private IModel<Project> selectedProject;

    public TagSetSelectionPanel(String id, IModel<Project> aProject, IModel<TagSet> aTagset)
    {
        super(id, aProject);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        selectedProject = aProject;
        
        overviewList = new OverviewListChoice<>("tagset");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(aTagset);
        overviewList.setChoices(LambdaModel.of(this::listTagSets));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        add(new LambdaAjaxLink("create", this::actionCreate));
    }

    private List<TagSet> listTagSets()
    {
        if (selectedProject.getObject() != null) {
            return annotationSchemaService.listTagSets(selectedProject.getObject());
        }
        else {
            return Collections.emptyList();
        }
    }
}
