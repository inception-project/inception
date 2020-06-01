package de.tudarmstadt.ukp.inception.externalsearch.solr.traits;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class SolrSearchProviderTraitsEditor
    extends Panel
{


    private static final String MID_FORM = "form";

    private @SpringBean ExternalSearchProviderFactory<SolrSearchProviderTraits>
        externalSearchProviderFactory;
    private final DocumentRepository documentRepository;
    private final SolrSearchProviderTraits properties;

    public SolrSearchProviderTraitsEditor(String aId,
                                             IModel<DocumentRepository> aDocumentRepository)
    {
        super(aId, aDocumentRepository);
        documentRepository = aDocumentRepository.getObject();
        properties = externalSearchProviderFactory.readTraits(documentRepository);

        Form<SolrSearchProviderTraits> form = new Form<SolrSearchProviderTraits>(
            MID_FORM, CompoundPropertyModel.of(Model.of(properties)))
        {
            private static final long serialVersionUID = -3109239608742291123L; // delete ?

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                externalSearchProviderFactory.writeTraits(documentRepository, properties);
            }
        };

        TextField<String> remoteUrl = new TextField<>("remoteUrl");
        remoteUrl.setRequired(true);
        remoteUrl.add(new UrlValidator());
        form.add(remoteUrl);

        TextField<String> indexName = new TextField<>("indexName");
        indexName.setRequired(true);
        form.add(indexName);

        TextField<String> searchPath = new TextField<>("searchPath"); // delete ?
        searchPath.setRequired(true);
        form.add(searchPath);

        TextField<String> defaultField = new TextField<>("defaultField");
        defaultField.setRequired(true);
        form.add(defaultField);

        TextField<String> textField = new TextField<>("textField");
        textField.setRequired(true);
        form.add(textField);

        NumberTextField<Integer> resultSize =
            new NumberTextField<>("resultSize", Integer.class);
        resultSize.setMinimum(1);
        resultSize.setMaximum(10000);
        resultSize.setRequired(true);
        form.add(resultSize);

        NumberTextField<Integer> seed = new NumberTextField<Integer>("seed", Integer.class);
        seed.setMinimum(0);
        seed.setMaximum(Integer.MAX_VALUE);
        seed.add(visibleWhen(() -> properties.isRandomOrder()));
        seed.add(new AttributeModifier("title", new ResourceModel("seedTooltip")));
        seed.setOutputMarkupPlaceholderTag(true);
        seed.setRequired(true);
        form.add(seed);

        CheckBox randomOrder = new CheckBox("randomOrder");
        randomOrder.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t ->
            t.add(seed, randomOrder)));
        form.add(randomOrder);

        add(form);
    }
}
