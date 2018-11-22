package de.tudarmstadt.ukp.clarin.ui.core.documentlist;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;

import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * This is used for a Websocket Test
 */
//@MenuItem(icon = "images/information.png", label = "Table View Annotation")
@MountPath("/DocumentListPage.html")
public class DocumentListPage
    extends ApplicationPageBase
{
    /**
     * 
     */
    private static final long serialVersionUID = -5972434095216188594L;

    private class RequestForm extends Form<String>
    {
        private static final long serialVersionUID = -1L;
        Label output = new Label("output", "OutputText");
        public RequestForm(String id, IModel<String> aModel)
        {
            super(id, new CompoundPropertyModel<>(aModel));
            
            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);
            
            output.setVisible(true);
            add(output);
        }
        
        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            
            setVisible(getModelObject() != null);
        }
    }

    private RequestForm requestForm;
    
    private IModel<String> selectedMsg;
    
    public DocumentListPage()
    {        
        requestForm = new RequestForm("detailForm", selectedMsg);

        add(requestForm);
        requestForm.setVisible(true);
    }
}
