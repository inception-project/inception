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
package de.tudarmstadt.ukp.inception.project.export.legacy;

import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * @deprecated Old export page code - to be removed in a future release.
 */
@Deprecated
public class ExportProgressModel
    extends ProgressionModel
{
    private static final long serialVersionUID = 1971929040248482474L;

    private final IModel<ProjectExportTaskMonitor> monitor;

    public ExportProgressModel(IModel<ProjectExportTaskMonitor> aMonitor)
    {
        monitor = aMonitor;
    }

    @Override
    protected Progression getProgression()
    {
        ProjectExportTaskMonitor m = monitor.getObject();
        if (m != null) {
            Optional<LogMessage> msg = Optional.ofNullable(m.getMessages().peek());
            return new Progression(m.getProgress(), msg.map(LogMessage::getMessage).orElse(null));
        }
        else {
            return new Progression(0, "Export not started yet...");
        }
    }
}
