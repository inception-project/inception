package de.tudarmstadt.ukp.inception.ui.kb.stmt.editor;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.UrlValidator;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

/**
 * {@link FallbackEditorPresenter} doubles as an editor and presenter.<br>
 * Presents values of whichever type as a {@code String}. When functioning as an editor, provides a
 * basic {@code String} input field.
 */
public class FallbackEditorPresenter extends ValueEditor<Value> {

    private static final long serialVersionUID = 5698178279574974977L;
    
    private IModel<String> stringValueModel;
    private IModel<String> datatypeModel;

    private Component focusComponent;

    public FallbackEditorPresenter(String id, IModel<Value> valueModel, boolean edit) {
        super(id, valueModel);
        
        stringValueModel = Model.of();
        datatypeModel = Model.of();
        
        Object object = valueModel.getObject();
        if (object != null) {
            stringValueModel.setObject(object.toString());
            IRI datatypeIri = DefaultDatatypeMapper.getDatatypeURI(object.getClass());
            // null values for datatype IRIs happen frequently if the value is an IRI itself
            datatypeModel.setObject(datatypeIri == null ? null : datatypeIri.stringValue());
        }        
        
        WebMarkupContainer editorWrapper = new WebMarkupContainer("editorWrapper");
        editorWrapper.setVisible(edit);
        editorWrapper.setOutputMarkupId(true);
        
        TextArea<String> valueTextArea = new TextArea<>("value", stringValueModel);
        valueTextArea.setOutputMarkupId(true);
        valueTextArea.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", t -> t.add(getParent())));
        editorWrapper.add(valueTextArea);
        focusComponent = valueTextArea;
        
        // TODO sloppy validation, schema not set, existence not checked
        TextField<String> datatypeField =  new TextField<>("datatype", datatypeModel);
        datatypeField.add(new UrlValidator());
        editorWrapper.add(datatypeField);
        
        add(editorWrapper);        
        
        Label label = new Label("label", stringValueModel);
        label.setVisibilityAllowed(!edit);
        add(label);
    }
    
    @Override
    public void convertInput() {       
        // set KBStatement value to a String literal regardless of the KBProperty's range
        ValueFactory vf = SimpleValueFactory.getInstance();
        Value val;
        if (datatypeModel.getObject() == null) {
            val = vf.createIRI(stringValueModel.getObject());
        } else {
            val = vf.createLiteral(stringValueModel.getObject(), datatypeModel.getObject());
        }
        setConvertedInput(val);
        valueModel.setObject(val);
    }

    @Override
    public Component getFocusComponent() {
        return focusComponent;
    }
}
