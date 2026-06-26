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
package de.tudarmstadt.ukp.inception.support.kendo;

import static java.util.Arrays.asList;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.KendoDataSource;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;

/**
 * A Kendo {@link MultiSelect} that lets the user enter arbitrary string values in addition to (or
 * instead of) any choices offered by {@link #getChoices}. It enables server-side filtering so the
 * current input is passed to {@code getChoices}, filters out blank entries on submission and works
 * around a Kendo quirk where chip removals are not synced back to the underlying {@code <select>}.
 * Subclasses only need to implement {@link #getChoices}.
 */
public abstract class FreeTextMultiSelect
    extends MultiSelect<String>
{
    private static final long serialVersionUID = -2674575240091821322L;

    public FreeTextMultiSelect(String aId)
    {
        super(aId);
        setOutputMarkupId(true);
    }

    @Override
    protected void onConfigure(KendoDataSource aDataSource)
    {
        // This ensures that we get the user input in getChoices
        aDataSource.set("serverFiltering", true);
    }

    @Override
    public void onConfigure(JQueryBehavior aBehavior)
    {
        super.onConfigure(aBehavior);
        aBehavior.setOption("filter", Options.asString("contains"));
        aBehavior.setOption("autoClose", false);
    }

    @Override
    public void convertInput()
    {
        var input = getInputAsArray();
        var list = new ArrayList<String>();
        if (input != null) {
            list.addAll(asList(input));
            list.removeIf(StringUtils::isBlank);
        }
        setConvertedInput(list);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        // The Kendo MultiSelect with serverFiltering=true does not reliably sync chip removals
        // back to the underlying <select> element, so removed items still get submitted with the
        // form. Rebuild the <select> from the widget value on every change to keep form submission
        // in sync. The bind is guarded by a flag on the widget so that re-running this dom-ready
        // script after an Ajax re-render (which may reuse the same widget instance) does not stack
        // duplicate change handlers. The poll for the widget is capped so that a widget which never
        // initializes (e.g. it was hidden or removed by an Ajax update before the script ran) does
        // not keep rescheduling for the lifetime of the page.
        var script = "(function(){var tries=0;var attach=function(){var ms=$('#" + getMarkupId()
                + "').data('kendoMultiSelect');if(!ms){if(++tries<100){setTimeout(attach,50);}return;}"
                + "if(ms._inceptionFreeTextSync){return;}ms._inceptionFreeTextSync=true;"
                + "ms.bind('change',function(){var v=this.value();var s=$(this.element);"
                + "s.empty();for(var i=0;i<v.length;i++){"
                + "s.append($('<option selected></option>').val(v[i]).text(v[i]));}});};"
                + "attach();})();";
        aResponse.render(OnDomReadyHeaderItem.forScript(script));
    }
}
