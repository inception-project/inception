package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;

public class DocumentMetadataAnnotationSelectionPanel extends Panel
{
    public DocumentMetadataAnnotationSelectionPanel(String aId, IModel<?> aModel)
    {
        super(aId, aModel);
        
        add(new DropDownChoice<>("layer"));
    }
    
    private ListView<Project> createProjectList()
    {
        return new ListView<Project>("annotations")
        {
            @Override
            protected void populateItem(ListItem<Project> aItem)
            {
                LambdaStatelessLink projectLink = new LambdaStatelessLink("annotationLink", () -> {
                });
                projectLink.add(new Label("label", Model.of("DUMMY LABEL")));
                aItem.add(projectLink);
            }
        };
    }}
