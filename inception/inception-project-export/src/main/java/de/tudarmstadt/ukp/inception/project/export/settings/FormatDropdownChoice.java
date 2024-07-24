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
package de.tudarmstadt.ukp.inception.project.export.settings;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;

public class FormatDropdownChoice
    extends DropDownChoice<String>
{
    private static final long serialVersionUID = -2143443446541508033L;

    private @SpringBean DocumentImportExportService importExportService;

    public FormatDropdownChoice(String aId)
    {
        super(aId);
    }

    public FormatDropdownChoice(String aId, IModel<String> aModel)
    {
        super(aId);
        setDefaultModel(aModel);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setChoiceRenderer(new ChoiceRenderer<String>()
        {
            private static final long serialVersionUID = -6139450455463062998L;

            @Override
            public Object getDisplayValue(String aObject)
            {
                if (FullProjectExportRequest.FORMAT_AUTO.equals(aObject)) {
                    return FullProjectExportRequest.FORMAT_AUTO;
                }

                return importExportService.getFormatById(aObject).get().getName();
            }
        });

        setChoices(LoadableDetachableModel.of(() -> {
            var formats = importExportService.getWritableFormats().stream() //
                    .sorted(comparing(FormatSupport::getName, String.CASE_INSENSITIVE_ORDER)) //
                    .map(FormatSupport::getId) //
                    .collect(toCollection(ArrayList::new));
            formats.add(0, FullProjectExportRequest.FORMAT_AUTO);
            return formats;
        }));
    }
}
