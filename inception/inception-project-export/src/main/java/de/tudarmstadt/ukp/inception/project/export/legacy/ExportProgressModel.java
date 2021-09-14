package de.tudarmstadt.ukp.inception.project.export.legacy;

import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class ExportProgressModel extends ProgressionModel
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
            return new Progression(m.getProgress(),
                    msg.map(LogMessage::getMessage).orElse(null));
        }
        else {
            return new Progression(0, "Export not started yet...");
        }
    }
}
