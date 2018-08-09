package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class InstancePanel
    extends Panel
{
    private static final long serialVersionUID = -1413622323011843523L;
    private static final Logger LOG = LoggerFactory.getLogger(ConceptInstancePanel.class);

    private static final String INSTANCE_INFO_MENTIONS_MARKUP_ID = "annotatedResultGroups";

    private @SpringBean KnowledgeBaseService kbService;

    public InstancePanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> selectedConceptHandle, IModel<KBHandle> selectedInstanceHandle,
            IModel<KBInstance> selectedInstanceModel)
    {
        super(aId, selectedInstanceModel);
        setOutputMarkupId(true);
        addOrReplace(new InstanceInfoPanel("instanceinfo", aKbModel, selectedInstanceHandle,
                selectedInstanceModel));

        Component annotatedSearchPanel;
        if (selectedInstanceHandle.getObject() != null) {
            annotatedSearchPanel = new AnnotatedListIdentifiers(INSTANCE_INFO_MENTIONS_MARKUP_ID,
                    aKbModel, selectedConceptHandle, selectedInstanceHandle, true);
            add(annotatedSearchPanel);
        }
        else {
            annotatedSearchPanel = new EmptyPanel(INSTANCE_INFO_MENTIONS_MARKUP_ID)
                    .setVisibilityAllowed(false);
            add(annotatedSearchPanel);
        }
    }

}
