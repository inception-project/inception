/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.kb.KnowledgeBasePageMenuItem;
import de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptFeatureSupport;
import de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptLabelCache;
import de.tudarmstadt.ukp.inception.ui.kb.feature.MultiValueConceptFeatureSupport;
import de.tudarmstadt.ukp.inception.ui.kb.initializers.NamedEntityIdentifierFeatureInitializer;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.ui.kb.project.ProjectKnowledgeBaseMenuItem;
import de.tudarmstadt.ukp.inception.ui.kb.search.ConceptFeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.DefaultColoringStrategyImpl;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.DescriptionColoringStrategyImpl;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.LabelColoringStrategyImpl;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.StatementColoringRegistry;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.StatementColoringRegistryImpl;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.StatementColoringStrategy;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.SubclassOfColoringStrategyImpl;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.TypeColoringStrategyImpl;
import de.tudarmstadt.ukp.inception.ui.kb.value.BooleanLiteralValueSupport;
import de.tudarmstadt.ukp.inception.ui.kb.value.IriValueSupport;
import de.tudarmstadt.ukp.inception.ui.kb.value.NumericLiteralValueSupport;
import de.tudarmstadt.ukp.inception.ui.kb.value.StringLiteralValueSupport;
import de.tudarmstadt.ukp.inception.ui.kb.value.ValueTypeSupport;
import de.tudarmstadt.ukp.inception.ui.kb.value.ValueTypeSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.kb.value.ValueTypeSupportRegistryImpl;

@ConditionalOnWebApplication
@Configuration
@AutoConfigureAfter(KnowledgeBaseServiceAutoConfiguration.class)
@ConditionalOnBean(KnowledgeBaseService.class)
public class KnowledgeBaseServiceUIAutoConfiguration
{
    @Bean
    public NamedEntityIdentifierFeatureInitializer namedEntityIdentifierFeatureInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new NamedEntityIdentifierFeatureInitializer(aAnnotationSchemaService);
    }

    @Bean
    public KnowledgeBaseProjectSettingsPanelFactory knowledgeBaseProjectSettingsPanelFactory()
    {
        return new KnowledgeBaseProjectSettingsPanelFactory();
    }

    @Bean
    public ConceptFeatureSupport conceptFeatureSupport(ConceptLabelCache aConceptLabelCache)
    {
        return new ConceptFeatureSupport(aConceptLabelCache);
    }

    @Bean
    public MultiValueConceptFeatureSupport multiValueConceptFeatureSupport(
            ConceptLabelCache aConceptLabelCache)
    {
        return new MultiValueConceptFeatureSupport(aConceptLabelCache);
    }

    @Bean
    public ConceptLabelCache conceptLabelCache(KnowledgeBaseService aKbService,
            KnowledgeBaseProperties aKBProperties)
    {
        return new ConceptLabelCache(aKbService, aKBProperties);
    }

    @Bean
    public ConceptFeatureIndexingSupport conceptFeatureIndexingSupport(
            FeatureSupportRegistry aFeatureSupportRegistry, KnowledgeBaseService aKbService)
    {
        return new ConceptFeatureIndexingSupport(aFeatureSupportRegistry, aKbService);
    }

    @Bean
    public DefaultColoringStrategyImpl defaultColoringStrategy()
    {
        return new DefaultColoringStrategyImpl();
    }

    @Bean
    public DescriptionColoringStrategyImpl descriptionColoringStrategy()
    {
        return new DescriptionColoringStrategyImpl();
    }

    @Bean
    public LabelColoringStrategyImpl labelColoringStrategy()
    {
        return new LabelColoringStrategyImpl();
    }

    @Bean
    public SubclassOfColoringStrategyImpl subclassOfColoringStrategy()
    {
        return new SubclassOfColoringStrategyImpl();
    }

    @Bean
    public TypeColoringStrategyImpl typeColoringStrategy()
    {
        return new TypeColoringStrategyImpl();
    }

    @Bean
    public StatementColoringRegistry statementColoringRegistry(
            @Lazy @Autowired(required = false) List<StatementColoringStrategy> aStatementColoringStrategies)
    {
        return new StatementColoringRegistryImpl(aStatementColoringStrategies);
    }

    @Bean
    public BooleanLiteralValueSupport booleanLiteralValueSupport()
    {
        return new BooleanLiteralValueSupport();
    }

    @Bean
    public NumericLiteralValueSupport numericLiteralValueSupport()
    {
        return new NumericLiteralValueSupport();
    }

    @Bean
    public StringLiteralValueSupport stringLiteralValueSupport()
    {
        return new StringLiteralValueSupport();
    }

    @Bean
    public IriValueSupport iriValueSupport()
    {
        return new IriValueSupport();
    }

    @Bean
    public ValueTypeSupportRegistry valueTypeSupportRegistry(
            @Lazy @Autowired(required = false) List<ValueTypeSupport> aValueTypeSupports)
    {
        return new ValueTypeSupportRegistryImpl(aValueTypeSupports);
    }

    @Bean
    public KnowledgeBasePageMenuItem knowledgeBasePageMenuItem(UserDao aUserRepo,
            ProjectService aProjectService, KnowledgeBaseService aKbService)
    {
        return new KnowledgeBasePageMenuItem(aUserRepo, aProjectService, aKbService);
    }

    @Bean
    public ProjectKnowledgeBaseMenuItem projectKnowledgeBaseMenuItem()
    {
        return new ProjectKnowledgeBaseMenuItem();
    }
}
