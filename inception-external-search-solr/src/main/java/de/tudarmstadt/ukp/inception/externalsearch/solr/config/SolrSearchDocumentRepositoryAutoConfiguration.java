package de.tudarmstadt.ukp.inception.externalsearch.solr.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.solr.SolrSearchProviderFactory;

/**
 * Provides support for SolrSearch-based document repositories.
 */
@Configuration
@AutoConfigureAfter(ExternalSearchAutoConfiguration.class)
@ConditionalOnProperty(prefix = "external-search.solr", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ExternalSearchService.class)
public class SolrSearchDocumentRepositoryAutoConfiguration {

    @Bean
    public SolrSearchProviderFactory solrSearchProviderFactory()
    {
        return new SolrSearchProviderFactory();
    }
}
