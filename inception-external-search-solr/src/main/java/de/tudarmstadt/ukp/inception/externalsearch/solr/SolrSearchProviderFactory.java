package de.tudarmstadt.ukp.inception.externalsearch.solr;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraitsEditor;




@Order(101)
public class SolrSearchProviderFactory
    implements BeanNameAware, ExternalSearchProviderFactory<SolrSearchProviderTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String beanName;

    @Override
    public void setBeanName(String aName)
    {
        beanName = aName;
    }

    @Override
    public String getBeanName()
    {
        return beanName;
    }

    @Override
    public String getDisplayName()
    {
        return "Solr Search";
    }

    @Override
    public ExternalSearchProvider getNewExternalSearchProvider()
    {
        return new SolrSearchProvider();
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<DocumentRepository> aDocumentRepository)
    {
        return new SolrSearchProviderTraitsEditor(aId, aDocumentRepository);
    }

    @Override
    public SolrSearchProviderTraits readTraits(DocumentRepository aDocumentRepository)
    {
        SolrSearchProviderTraits traits = null;
        try {
            traits = fromJsonString(SolrSearchProviderTraits.class,
                aDocumentRepository.getProperties());
        }
        catch (IOException e) {
            log.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = new SolrSearchProviderTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(DocumentRepository aDocumentRepository,
                            SolrSearchProviderTraits aProperties)
    {
        try {
            String json = toJsonString(aProperties);
            aDocumentRepository.setProperties(json);
        }
        catch (IOException e) {
            log.error("Error while writing traits", e);
        }
    }
}
