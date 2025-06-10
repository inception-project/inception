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
package de.tudarmstadt.ukp.inception.ui.core.log;

import static java.lang.String.format;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.logging.RingBufferAppender;

public class LogPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -3184701055919509115L;

    private @SpringBean UserDao userService;

    private final WebMarkupContainer messagesContainer;
    private final LambdaAjaxLink refreshButton;
    private final CheckBox autoRefresh;

    public LogPage()
    {
        if (!userService.isCurrentUserAdmin()) {
            denyAccess();
        }

        var formatter = FastDateFormat.getInstance();

        autoRefresh = new CheckBox("autoRefresh", Model.of(true));
        autoRefresh.setOutputMarkupId(true);
        queue(autoRefresh);
        refreshButton = new LambdaAjaxLink("refresh", this::actionRefresh);
        refreshButton.setOutputMarkupId(true);
        queue(refreshButton);

        messagesContainer = new WebMarkupContainer("messagesContainer");
        messagesContainer.setOutputMarkupId(true);
        queue(messagesContainer);

        queue(new ListView<>("messages", LoadableDetachableModel.of(this::getMessages))
        {
            private static final long serialVersionUID = -5458639302523937217L;

            @Override
            protected void populateItem(ListItem<LogEvent> aItem)
            {
                var event = aItem.getModelObject();
                aItem.add(new Icon("level", getLevelIcon(event)).add(classModifier(event)));
                aItem.add(new Label("timestamp",
                        formatter.format(event.getInstant().getEpochMillisecond())));
                aItem.add(new Label("message", event.getMessage().getFormattedMessage())
                        .setVisible(event.getThrown() == null));
                aItem.add(new Label("errorMessage", event.getMessage().getFormattedMessage())
                        .setVisible(event.getThrown() != null));
                aItem.add(new Label("stackTrace", stackTrace(aItem.getModelObject()))
                        .setVisible(event.getThrown() != null));
            }

            private ClassAttributeModifier classModifier(LogEvent aEvent)
            {
                return new ClassAttributeModifier()
                {
                    private static final long serialVersionUID = 6931937199394211037L;

                    @Override
                    protected Set<String> update(Set<String> aOldClasses)
                    {
                        switch (aEvent.getLevel().getStandardLevel()) {
                        case ERROR:
                            aOldClasses.add("text-danger");
                            break;
                        case WARN:
                            aOldClasses.add("text-warning");
                            break;
                        default:
                            aOldClasses.add("text-primary");
                            break;
                        }
                        return aOldClasses;
                    }

                };
            }

            private FontAwesome5IconType getLevelIcon(LogEvent aEvent)
            {
                var levelIcon = switch (aEvent.getLevel().getStandardLevel()) {
                case ERROR -> FontAwesome5IconType.exclamation_triangle_s;
                case WARN -> FontAwesome5IconType.exclamation_triangle_s;
                default -> FontAwesome5IconType.info_circle_s;
                };
                return levelIcon;
            }
        });
    }

    private void actionRefresh(AjaxRequestTarget aTarget)
    {
        aTarget.add(messagesContainer);
        aTarget.appendJavaScript(scrollDownScript());
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(OnDomReadyHeaderItem.forScript(scrollDownScript()));
        aResponse.render(OnDomReadyHeaderItem.forScript(getRefreshScript()));
    }

    private String getRefreshScript()
    {
        return "let enableCheckBox = document.getElementById('" + autoRefresh.getMarkupId()
                + "');\n" + //
                "setInterval(function() {\n" + //
                "  if (!enableCheckBox.checked) return;\n" + //
                "  document.getElementById('" + refreshButton.getMarkupId() + "').click();\n" + //
                "}, 5000);\n";
    }

    private String scrollDownScript()
    {
        return """
                var element = document.getElementById('""" + messagesContainer.getMarkupId() + """
                ');\
                if (element) {\
                  element.scrollTop = element.scrollHeight;\
                }""";
    }

    private List<LogEvent> getMessages()
    {
        return new ArrayList<>(RingBufferAppender.events());
    }

    private void denyAccess()
    {
        getSession().error(format("Access to [%s] denied.", getClass().getSimpleName()));
        throw new RestartResponseException(getApplication().getHomePage());
    }

    private String stackTrace(LogEvent aEvent)
    {
        if (aEvent.getThrown() == null) {
            return null;
        }

        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        aEvent.getThrown().printStackTrace(pw);
        return sw.toString();
    }
}
