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
package de.tudarmstadt.ukp.clarin.webanno.diag.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.AllAnnotationsStartAndEndWithCharactersCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.AllAnnotationsStartAndEndWithinSentencesCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.AllFeatureStructuresIndexedCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.CASMetadataTypeIsPresentCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.Check;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.DanglingRelationsCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.DocumentTextStartsWithBomCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.FeatureAttachedSpanAnnotationsTrulyAttachedCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.LinksReachableThroughChainsCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.NegativeSizeAnnotationsCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.NoMultipleIncomingRelationsCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.NoZeroSizeTokensAndSentencesCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.PdfStructurePresentInNonInitialCasCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.RelationOffsetsCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.TokensAndSententencedDoNotOverlapCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.UniqueDocumentAnnotationCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.UnreachableAnnotationsCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.XmlStructurePresentInNonInitialCasCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.CoverAllTextInSentencesRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.ReattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.ReattachFeatureAttachedSpanAnnotationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.ReindexFeatureAttachedSpanAnnotationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RelationOffsetsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveBomRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveDanglingChainLinksRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveDanglingFeatureAttachedSpanAnnotationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveDanglingRelationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemovePdfStructureFromNonInitialCasRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveXmlStructureFromNonInitialCasRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveZeroSizeTokensAndSentencesRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.SwitchBeginAndEndOnNegativeSizedAnnotationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.TrimAnnotationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.UpgradeCasRepair;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Configuration
@EnableConfigurationProperties(CasDoctorPropertiesImpl.class)
public class CasDoctorAutoConfiguration
{
    @Bean
    CasDoctor casDoctor(CasDoctorProperties aProperties, ChecksRegistry aChecksRegistry,
            RepairsRegistry aRepairsRegistry)
    {
        return new CasDoctorImpl(aProperties, aChecksRegistry, aRepairsRegistry);
    }

    @Bean
    public ChecksRegistry checksRegistry(@Lazy @Autowired(required = false) List<Check> aExtensions)
    {
        return new ChecksRegistryImpl(aExtensions);
    }

    @Bean
    public RepairsRegistry repairsRegistry(
            @Lazy @Autowired(required = false) List<Repair> aExtensions)
    {
        return new RepairsRegistryImpl(aExtensions);
    }

    @Bean
    public AllFeatureStructuresIndexedCheck allFeatureStructuresIndexedCheck()
    {
        return new AllFeatureStructuresIndexedCheck();
    }

    @Bean
    public CASMetadataTypeIsPresentCheck casMetadataTypeIsPresentCheck()
    {
        return new CASMetadataTypeIsPresentCheck();
    }

    @Bean
    public DanglingRelationsCheck danglingRelationsCheck(AnnotationSchemaService aAnnotationService)
    {
        return new DanglingRelationsCheck(aAnnotationService);
    }

    @Bean
    public FeatureAttachedSpanAnnotationsTrulyAttachedCheck featureAttachedSpanAnnotationsTrulyAttachedCheck(
            AnnotationSchemaService aAnnotationService)
    {
        return new FeatureAttachedSpanAnnotationsTrulyAttachedCheck(aAnnotationService);
    }

    @Bean
    public LinksReachableThroughChainsCheck linksReachableThroughChainsCheck(
            AnnotationSchemaService aAnnotationService)
    {
        return new LinksReachableThroughChainsCheck(aAnnotationService);
    }

    @Bean
    public NoMultipleIncomingRelationsCheck noMultipleIncomingRelationsCheck(
            AnnotationSchemaService aAnnotationService)
    {
        return new NoMultipleIncomingRelationsCheck(aAnnotationService);
    }

    @Bean
    public NoZeroSizeTokensAndSentencesCheck noZeroSizeTokensAndSentencesCheck()
    {
        return new NoZeroSizeTokensAndSentencesCheck();
    }

    @Bean
    public RelationOffsetsCheck relationOffsetsCheck(AnnotationSchemaService aAnnotationService)
    {
        return new RelationOffsetsCheck(aAnnotationService);
    }

    @Bean
    public UniqueDocumentAnnotationCheck uniqueDocumentAnnotationCheck()
    {
        return new UniqueDocumentAnnotationCheck();
    }

    @Bean
    public ReattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair //
            reattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair(
                    AnnotationSchemaService aAnnotationService)
    {
        return new ReattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair(aAnnotationService);
    }

    @Bean
    public ReattachFeatureAttachedSpanAnnotationsRepair reattachFeatureAttachedSpanAnnotationsRepair(
            AnnotationSchemaService aAnnotationService)
    {
        return new ReattachFeatureAttachedSpanAnnotationsRepair(aAnnotationService);
    }

    @Bean
    public ReindexFeatureAttachedSpanAnnotationsRepair reindexFeatureAttachedSpanAnnotationsRepair(
            AnnotationSchemaService aAnnotationService)
    {
        return new ReindexFeatureAttachedSpanAnnotationsRepair(aAnnotationService);
    }

    @Bean
    public RelationOffsetsRepair relationOffsetsRepair(AnnotationSchemaService aAnnotationService)
    {
        return new RelationOffsetsRepair(aAnnotationService);
    }

    @Bean
    public RemoveDanglingChainLinksRepair removeDanglingChainLinksRepair(
            AnnotationSchemaService aAnnotationService)
    {
        return new RemoveDanglingChainLinksRepair(aAnnotationService);
    }

    @Bean
    public RemoveDanglingFeatureAttachedSpanAnnotationsRepair removeDanglingFeatureAttachedSpanAnnotationsRepair(
            AnnotationSchemaService aAnnotationService)
    {
        return new RemoveDanglingFeatureAttachedSpanAnnotationsRepair(aAnnotationService);
    }

    @Bean
    public RemoveDanglingRelationsRepair removeDanglingRelationsRepair(
            AnnotationSchemaService aAnnotationService)
    {
        return new RemoveDanglingRelationsRepair(aAnnotationService);
    }

    @Bean
    public RemoveZeroSizeTokensAndSentencesRepair removeZeroSizeTokensAndSentencesRepair()
    {
        return new RemoveZeroSizeTokensAndSentencesRepair();
    }

    @Bean
    public UpgradeCasRepair upgradeCasRepair(AnnotationSchemaService aAnnotationService)
    {
        return new UpgradeCasRepair(aAnnotationService);
    }

    @Bean
    public SwitchBeginAndEndOnNegativeSizedAnnotationsRepair switchBeginAndEndOnNegativeSizedAnnotationsRepair()
    {
        return new SwitchBeginAndEndOnNegativeSizedAnnotationsRepair();
    }

    @Bean
    public NegativeSizeAnnotationsCheck noNegativeSizeAnnotationsCheck()
    {
        return new NegativeSizeAnnotationsCheck();
    }

    @Bean
    public AllAnnotationsStartAndEndWithinSentencesCheck allAnnotationsStartAndEndWithinSentencesCheck(
            AnnotationSchemaService aAnnotationService)
    {
        return new AllAnnotationsStartAndEndWithinSentencesCheck(aAnnotationService);
    }

    @Bean
    public CoverAllTextInSentencesRepair coverAllTextInSentencesRepair()
    {
        return new CoverAllTextInSentencesRepair();
    }

    @Bean
    public TokensAndSententencedDoNotOverlapCheck tokensAndSententencedDoNotOverlapCheck()
    {
        return new TokensAndSententencedDoNotOverlapCheck();
    }

    @Bean
    public UnreachableAnnotationsCheck unreachableAnnotationsCheck()
    {
        return new UnreachableAnnotationsCheck();
    }

    @Bean
    public AllAnnotationsStartAndEndWithCharactersCheck allAnnotationsStartAndEndWithCharactersCheck(
            AnnotationSchemaService aAnnotationService)
    {
        return new AllAnnotationsStartAndEndWithCharactersCheck(aAnnotationService);
    }

    @Bean
    public TrimAnnotationsRepair trimAnnotationsRepair(AnnotationSchemaService aAnnotationService)
    {
        return new TrimAnnotationsRepair(aAnnotationService);
    }

    @Bean
    public DocumentTextStartsWithBomCheck documentTextStartsWithBomCheck()
    {
        return new DocumentTextStartsWithBomCheck();
    }

    @Bean
    public RemoveBomRepair removeBomRepair()
    {
        return new RemoveBomRepair();
    }

    @Bean
    public XmlStructurePresentInNonInitialCasCheck xmlStructurePresentInNonInitialCasCheck()
    {
        return new XmlStructurePresentInNonInitialCasCheck();
    }

    @Bean
    public RemoveXmlStructureFromNonInitialCasRepair removeXmlStructureFromNonInitialCasRepair()
    {
        return new RemoveXmlStructureFromNonInitialCasRepair();
    }

    @Bean
    public PdfStructurePresentInNonInitialCasCheck pdfStructurePresentInNonInitialCasCheck()
    {
        return new PdfStructurePresentInNonInitialCasCheck();
    }

    @Bean
    public RemovePdfStructureFromNonInitialCasRepair removePdfStructureFromNonInitialCasRepair()
    {
        return new RemovePdfStructureFromNonInitialCasRepair();
    }
}
