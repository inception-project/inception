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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementReportExportFormat.CSV;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationItem;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementReportExportFormat;
import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultRefreshingView;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.PopoverBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class PairwiseCodingAgreementTable
    extends Panel
{
    private static final long serialVersionUID = 571396822546125376L;

    private final static Logger LOG = LoggerFactory.getLogger(PairwiseCodingAgreementTable.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private final RefreshingView<String> rows;
    private final AjaxDownloadLink exportAllButton;
    private final DropDownChoice<AgreementReportExportFormat> formatField;

    private final SerializableSupplier<Map<String, List<CAS>>> casMapSupplier;

    public PairwiseCodingAgreementTable(String aId,
            IModel<PairwiseAnnotationResult<CodingAgreementResult>> aModel,
            SerializableSupplier<Map<String, List<CAS>>> aCasMapSupplier)
    {
        super(aId, aModel);

        casMapSupplier = aCasMapSupplier;

        setOutputMarkupId(true);

        PopoverConfig config = new PopoverConfig().withPlacement(Placement.left).withHtml(true);
        WebMarkupContainer legend = new WebMarkupContainer("legend");
        legend.add(new PopoverBehavior(new ResourceModel("legend"),
                new StringResourceModel("legend.content", legend), config));
        add(legend);

        // This model makes sure we add a "null" dummy rater which accounts for the header columns
        // of the table.
        final IModel<List<String>> ratersAdapter = LoadableDetachableModel.of(() -> {
            List<String> raters = new ArrayList<>();
            if (getModelObject() != null) {
                raters.add(null);
                raters.addAll(getModelObject().getRaters());
            }
            return raters;
        });

        add(formatField = new DropDownChoice<AgreementReportExportFormat>("exportFormat",
                Model.of(CSV), asList(AgreementReportExportFormat.values()),
                new EnumChoiceRenderer<>(this)));
        formatField.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));

        exportAllButton = new AjaxDownloadLink("exportAll",
                () -> "agreement" + formatField.getModelObject().getExtension(),
                this::exportAllAgreements);
        exportAllButton.add(enabledWhen(() -> formatField.getModelObject() != null));
        add(exportAllButton);

        rows = new DefaultRefreshingView<String>("rows", ratersAdapter)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<String> aRowItem)
            {
                // Render regular row
                aRowItem.add(new DefaultRefreshingView<String>("cells", ratersAdapter)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item<String> aCellItem)
                    {
                        aCellItem.setRenderBodyOnly(true);

                        Fragment cell;
                        // Header cell
                        if (aRowItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-centered",
                                    PairwiseCodingAgreementTable.this);
                        }
                        else if (aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-right",
                                    PairwiseCodingAgreementTable.this);
                        }
                        // Content cell
                        else {
                            cell = new Fragment("cell", "td", PairwiseCodingAgreementTable.this);
                        }

                        // Top-left cell
                        if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label", Model.of("")));
                        }
                        // Raters header horizontally
                        else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                            cell.add(new Label("label",
                                    userRepository.get(aCellItem.getModelObject()).getUiName()));
                        }
                        // Raters header vertically
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label",
                                    userRepository.get(aRowItem.getModelObject()).getUiName()));
                        }
                        // Upper diagonal
                        else if (aCellItem.getIndex() > aRowItem.getIndex()) {
                            cell.add(makeUpperDiagonalCellLabel(aRowItem.getModelObject(),
                                    aCellItem.getModelObject()));
                        }
                        // Lower diagonal
                        else if (aCellItem.getIndex() < aRowItem.getIndex()) {
                            cell.add(makeLowerDiagonalCellLabel(aRowItem.getModelObject(),
                                    aCellItem.getModelObject()));
                        }
                        // Rest
                        else {
                            cell.add(new Label("label", Model.of("-")));
                        }

                        aCellItem.add(cell);
                    }
                });
                // Odd/even coloring is reversed here to account for the header row at index 0
                aRowItem.add(new AttributeAppender("class",
                        (aRowItem.getIndex() % 2 == 0) ? "odd" : "even"));
            }
        };
        this.add(visibleWhen(
                () -> (getModelObject() != null && !getModelObject().getRaters().isEmpty())));
        add(rows);
    }

    private Label makeLowerDiagonalCellLabel(String aRater1, String aRater2)
    {
        CodingAgreementResult result = getModelObject().getStudy(aRater1, aRater2);

        String label = String.format("%d/%d", result.getCompleteSetCount(),
                result.getRelevantSetCount());

        String tooltipTitle = "Details about annotations excluded from agreement calculation";

        StringBuilder tooltipContent = new StringBuilder();
        if (result.isExcludeIncomplete()) {
            tooltipContent.append(String.format("- Incomplete (missing): %d%n",
                    result.getIncompleteSetsByPosition().size()));
            tooltipContent.append(String.format("- Incomplete (not labeled): %d%n",
                    result.getIncompleteSetsByLabel().size()));
        }
        tooltipContent.append(String.format("- Plurality: %d", result.getPluralitySets().size()));

        Label l = new Label("label", Model.of(label));
        DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(tooltipTitle,
                tooltipContent.toString());
        tooltip.setOption("position", (Object) null);
        l.add(tooltip);
        l.add(new AttributeAppender("style", "cursor: help", ";"));
        return l;
    }

    private Label makeUpperDiagonalCellLabel(String aRater1, String aRater2)
    {
        CodingAgreementResult result = getModelObject().getStudy(aRater1, aRater2);

        boolean noDataRater0 = isAllNull(result, result.getCasGroupIds().get(0));
        boolean noDataRater1 = isAllNull(result, result.getCasGroupIds().get(1));
        int incPos = result.getIncompleteSetsByPosition().size();
        int incLabel = result.getIncompleteSetsByLabel().size();

        String label;
        if (result.getStudy().getItemCount() == 0) {
            label = "no positions";
        }
        else if (noDataRater0 && noDataRater1) {
            label = "no labels";
        }
        else if (noDataRater0) {
            label = "no labels from " + result.getCasGroupIds().get(0);
        }
        else if (noDataRater1) {
            label = "no labels from " + result.getCasGroupIds().get(1);
        }
        else if (incPos == result.getRelevantSetCount()) {
            label = "positions disjunct";
        }
        else if (incLabel == result.getRelevantSetCount()) {
            label = "labels disjunct";
        }
        else if ((incLabel + incPos) == result.getRelevantSetCount()) {
            label = "labels/positions disjunct";
        }
        else {
            label = String.format("%.2f", result.getAgreement());
        }

        String tooltipTitle = result.getCasGroupIds().get(0) + '/' + result.getCasGroupIds().get(1);

        String tooltipContent = "Positions annotated:\n"
                + String.format("- %s: %d/%d%n", result.getCasGroupIds().get(0),
                        getNonNullCount(result, result.getCasGroupIds().get(0)),
                        result.getStudy().getItemCount())
                + String.format("- %s: %d/%d%n", result.getCasGroupIds().get(1),
                        getNonNullCount(result, result.getCasGroupIds().get(1)),
                        result.getStudy().getItemCount())
                + String.format("Distinct labels: %d%n", result.getStudy().getCategoryCount());

        Label l = new Label("label", Model.of(label));
        l.add(makeDownloadBehavior(aRater1, aRater2));
        DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(tooltipTitle,
                tooltipContent);
        tooltip.setOption("position", (Object) null);
        l.add(tooltip);
        l.add(new AttributeAppender("style", "cursor: pointer", ";"));

        return l;
    }

    public boolean isAllNull(AgreementResult<ICodingAnnotationStudy> aResult, String aCasGroupId)
    {
        for (ICodingAnnotationItem item : aResult.getStudy().getItems()) {
            if (item.getUnit(aResult.getCasGroupIds().indexOf(aCasGroupId)).getCategory() != null) {
                return false;
            }
        }
        return true;
    }

    public int getNonNullCount(AgreementResult<ICodingAnnotationStudy> aResult, String aCasGroupId)
    {
        int i = 0;
        for (ICodingAnnotationItem item : aResult.getStudy().getItems()) {
            if (item.getUnit(aResult.getCasGroupIds().indexOf(aCasGroupId)).getCategory() != null) {
                i++;
            }
        }
        return i;
    }

    private Behavior makeDownloadBehavior(final String aKey1, final String aKey2)
    {
        return new AjaxEventBehavior("click")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                AjaxDownloadBehavior download = new AjaxDownloadBehavior(
                        LoadableDetachableModel.of(PairwiseCodingAgreementTable.this::getFilename),
                        LoadableDetachableModel.of(() -> getAgreementTableData(aKey1, aKey2)));
                getComponent().add(download);
                download.initiate(aTarget);
            }
        };
    }

    private AbstractResourceStream getAgreementTableData(final String aKey1, final String aKey2)
    {
        return new AbstractResourceStream()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException
            {
                try {
                    CodingAgreementResult result = PairwiseCodingAgreementTable.this
                            .getModelObject().getStudy(aKey1, aKey2);

                    switch (formatField.getModelObject()) {
                    case CSV:
                        return AgreementUtils.generateCsvReport(result);
                    case DEBUG:
                        return generateDebugReport(result);
                    default:
                        throw new IllegalStateException(
                                "Unknown export format [" + formatField.getModelObject() + "]");
                    }
                }
                catch (Exception e) {
                    // FIXME Is there some better error handling here?
                    LOG.error("Unable to generate agreement report", e);
                    throw new ResourceStreamNotFoundException(e);
                }
            }

            @Override
            public void close() throws IOException
            {
                // Nothing to do
            }
        };
    }

    private String getFilename()
    {
        return "agreement" + formatField.getModelObject().getExtension();
    }

    private InputStream generateDebugReport(CodingAgreementResult aResult)
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        AgreementUtils.dumpAgreementStudy(new PrintStream(buf), aResult);
        return new ByteArrayInputStream(buf.toByteArray());
    }

    @SuppressWarnings("unchecked")
    public PairwiseAnnotationResult<CodingAgreementResult> getModelObject()
    {
        return (PairwiseAnnotationResult<CodingAgreementResult>) getDefaultModelObject();
    }

    public void setModelObject(PairwiseAnnotationResult<ICodingAnnotationStudy> aAgreements2)
    {
        setDefaultModelObject(aAgreements2);
    }

    private IResourceStream exportAllAgreements()
    {
        return new AbstractResourceStream()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException
            {
                AnnotationFeature feature = getModelObject().getFeature();
                DefaultAgreementTraits traits = getModelObject().getTraits();

                Map<String, List<CAS>> casMap = casMapSupplier.get();

                List<DiffAdapter> adapters = CasDiff.getDiffAdapters(annotationService,
                        asList(feature.getLayer()));

                CasDiff diff = doDiff(adapters, traits.getLinkCompareBehavior(), casMap);

                Set<String> tagset = annotationService.listTags(feature.getTagset()).stream()
                        .map(Tag::getName).collect(toCollection(LinkedHashSet::new));

                // AgreementResult agreementResult = AgreementUtils.makeStudy(diff,
                // feature.getLayer().getName(), feature.getName(),
                // pref.excludeIncomplete, casMap);
                // TODO: for the moment, we always include incomplete annotations during this
                // export.
                CodingAgreementResult agreementResult = makeCodingStudy(diff,
                        feature.getLayer().getName(), feature.getName(), tagset, false, casMap);

                try {
                    return AgreementUtils.generateCsvReport(agreementResult);
                }
                catch (Exception e) {
                    // FIXME Is there some better error handling here?
                    LOG.error("Unable to generate report", e);
                    throw new ResourceStreamNotFoundException(e);
                }
            }

            @Override
            public void close() throws IOException
            {
                // Nothing to do
            }
        };
    }
}
