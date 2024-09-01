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
package de.tudarmstadt.ukp.inception.externaleditor.policy;

import static java.util.regex.Pattern.compile;

import java.io.File;
import java.io.IOException;

import de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorProperties;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.AttributeAction;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.ElementAction;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionBuilder;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils;

public class SafetyNetDocumentPolicy
{
    static final String SAFETY_NET_POLICY_OVERRIDE_YAML = "safety-net.yaml";

    private static final String[] JAVASCRIPT_ACTIVE_ATTRIBUTES = { "href", "src", "codebase",
            "cite", "background", "action", "longdesc", "profile", "classid", "data", "usemap",
            "formaction", "icon", "manifest", "poster", "srcset", "archive" };

    private static final String[] JAVASCRIPT_EVENT_ATTRIBUTES = { "onafterprint", "onbeforeprint",
            "onbeforeunload", "onerror", "onhashchange", "onload", "onmessage", "onoffline",
            "ononline", "onpagehide", "onpageshow", "onpopstate", "onresize", "onstorage",
            "onunload", "onblur", "onchange", "oncontextmenu", "onfocus", "oninput", "oninvalid",
            "onreset", "onsearch", "onselect", "onsubmit", "onkeydown", "onkeypress", "onkeyup",
            "onclick", "ondblclick", "onmousedown", "onmousemove", "onmouseout", "onmouseover",
            "onmouseup", "onmousewheel", "onwheel", "ondrag", "ondragend", "ondragenter",
            "ondragleave", "ondragover", "ondragstart", "ondrop", "onscroll", "oncopy", "oncut",
            "onpaste", "onabort", "oncanplay", "oncanplaythrough", "oncuechange",
            "ondurationchange", "onemptied", "onended", "onerror", "onloadeddata",
            "onloadedmetadata", "onloadstart", "onpause", "onplay", "onplaying", "onprogress",
            "onratechange", "onseeked", "onseeking", "onstalled", "onsuspend", "ontimeupdate",
            "onvolumechange", "onwaiting", "ontoggle" };

    private final ExternalEditorProperties properties;

    private final PolicyCollection defaultPolicy;

    private final WatchedResourceFile<PolicyCollection> policyOverrideFile;

    public SafetyNetDocumentPolicy(ExternalEditorProperties aProperties) throws IOException
    {
        properties = aProperties;

        defaultPolicy = makeDefaultPolicy();

        var appHome = SettingsUtil.getApplicationHome();
        policyOverrideFile = new WatchedResourceFile<>(
                new File(appHome, SAFETY_NET_POLICY_OVERRIDE_YAML).toPath(),
                PolicyCollectionIOUtils::loadPolicies);
    }

    private PolicyCollection makeDefaultPolicy()
    {
        var policy = PolicyCollectionBuilder //
                .caseInsensitive() //
                .defaultAttributeAction(AttributeAction.PASS) //
                .defaultElementAction(ElementAction.PASS);

        policy.disallowElements("script", "meta", "applet", "link", "iframe");

        if (properties.isBlockStyle()) {
            policy.disallowElements("style");
        }

        if (properties.isBlockAudio()) {
            policy.disallowElements("audio");
        }
        else {
            switch (properties.getAllowAudioSource()) {
            case NONE:
                policy.disallowAttributes("src").onElements("audio");
                break;
            case LOCAL:
                policy.disallowAttributes("src") //
                        .matchingValue(compile("(?!res[?]resId=).*")) //
                        .onElements("audio");
                break;
            case ANY:
                // No restriction in this case
                break;
            }
        }

        if (properties.isBlockEmbed()) {
            policy.disallowElements("embed");
        }

        if (properties.isBlockImg()) {
            policy.disallowElements("img");
        }
        else {
            switch (properties.getAllowImgSource()) {
            case NONE:
                policy.disallowAttributes("src").onElements("img");
                break;
            case LOCAL:
                policy.disallowAttributes("src") //
                        .matchingValue(compile("(?!res[?]resId=).*")) //
                        .onElements("img");
                break;
            case ANY:
                // No restriction in this case
                break;
            }
        }

        if (properties.isBlockObject()) {
            policy.disallowElements("object");
        }

        if (properties.isBlockVideo()) {
            policy.disallowElements("video");
        }
        else {
            switch (properties.getAllowVideoSource()) {
            case NONE:
                policy.disallowAttributes("src").onElements("source");
                break;
            case LOCAL:
                policy.disallowAttributes("src") //
                        .matchingValue(compile("(?!res[?]resId=).*")) //
                        .onElements("video");
                break;
            case ANY:
                // No restriction in this case
                break;
            }
        }

        policy.disallowAttributes(JAVASCRIPT_ACTIVE_ATTRIBUTES) //
                .matchingValue(compile("\\s*javascript:.*")) //
                .globally();

        policy.disallowAttributes(JAVASCRIPT_EVENT_ATTRIBUTES).globally();
        return policy.build();
    }

    public PolicyCollection getPolicy() throws IOException
    {
        return policyOverrideFile.get().orElse(defaultPolicy);
    }
}
