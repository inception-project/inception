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
package de.tudarmstadt.ukp.inception.annotation.feature.hyperlink;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

/**
 * Feature support for hyperlink/URL features.
 */
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class HyperlinkFeatureSupport
        extends UimaPrimitiveFeatureSupport_ImplBase<HyperlinkFeatureTraits> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // URL validation pattern
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?" + // Protocol (optional based on traits)
                    "([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}" + // Domain
                    "(:[0-9]+)?" + // Port (optional)
                    "(/.*)?$" // Path (optional)
    );

    private List<FeatureType> primitiveTypes;

    public HyperlinkFeatureSupport() {
        // Constructor for testing
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Hyperlink is trait-based, not a separate feature type
        // Users create regular string features and enable hyperlink mode via traits
        primitiveTypes = asList();
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer) {
        // Return empty - hyperlink is not a separate type, it's trait-based
        // Users create string features and optionally enable hyperlink mode
        return unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature) {
        // Accept string features that have hyperlinkEnabled=true in their traits
        if (!CAS.TYPE_NAME_STRING.equals(aFeature.getType())) {
            return false;
        }

        if (!MultiValueMode.NONE.equals(aFeature.getMultiValueMode())) {
            return false;
        }

        // Check if hyperlink mode is enabled in StringFeatureTraits
        try {
            var json = aFeature.getTraits();
            if (json == null || json.isBlank()) {
                return false;
            }
            var node = MAPPER.readTree(json);
            return node.has("hyperlinkEnabled")
                    && node.get("hyperlinkEnabled").asBoolean(false);
        } catch (Exception e) {
            LOG.debug("Error checking traits for feature [{}]", aFeature.getName(), e);
            return false;
        }
    }

    @Override
    public boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS) {
        var value = FSUtil.getFeature(aFS, aFeature.getName(), String.class);

        if (aFeature.isRequired()) {
            if (!isNotBlank(value)) {
                return false;
            }
        }

        // If value is empty and not required, it's valid
        if (!isNotBlank(value)) {
            return true;
        }

        // Validate URL format
        var traits = readTraits(aFeature);
        return isValid(value, traits);
    }

    private boolean isValid(String aValue, HyperlinkFeatureTraits traits) {
        if (aValue == null || aValue.trim().isEmpty()) {
            return false;
        }

        // Try pipe-delimited format first (new format)
        if (!aValue.startsWith("[") && !aValue.startsWith("{")) {
            // Parse pipe-delimited format: text|url;text2|url2
            var entries = aValue.split(";");
            for (var entry : entries) {
                var parts = entry.split("\\|", 2);
                String url = parts.length == 2 ? parts[1] : parts[0];
                // Unescape
                url = url.replace("\\|", "|").replace("\\;", ";");
                if (!isValidUrl(url, traits)) {
                    return false;
                }
            }
            return true;
        }

        // Fallback: try legacy JSON format
        try {
            var node = MAPPER.readTree(aValue);
            if (node.isArray()) {
                for (var item : node) {
                    if (!isValidItem(item, traits)) {
                        return false;
                    }
                }
                return true;
            }

            return isValidItem(node, traits);
        } catch (Exception e) {
            // Not JSON, treat as raw URL
            return isValidUrl(aValue, traits);
        }
    }

    private boolean isValidItem(com.fasterxml.jackson.databind.JsonNode aNode, HyperlinkFeatureTraits traits) {
        String url = "";
        if (aNode.has("url")) {
            url = aNode.get("url").asText();
        } else if (aNode.isValueNode()) {
            url = aNode.asText();
        }

        return isValidUrl(url, traits);
    }

    private boolean isValidUrl(String aUrl, HyperlinkFeatureTraits traits) {
        if (aUrl == null || aUrl.trim().isEmpty()) {
            return false;
        }

        // Check if it matches the URL pattern
        if (!URL_PATTERN.matcher(aUrl).matches()) {
            return false;
        }

        // Check protocol requirements
        if (traits.isRequireProtocol() && !aUrl.startsWith("http://") && !aUrl.startsWith("https://")) {
            return false;
        }

        // Check allowed protocols
        if (aUrl.contains("://")) {
            var protocol = aUrl.substring(0, aUrl.indexOf("://"));
            if (!traits.getAllowedProtocols().contains(protocol)) {
                return false;
            }
        }

        return true;
    }

    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel) {
        var feature = aFeatureModel.getObject();

        // Provide traits editor for ALL string features, not just ones with hyperlink
        // enabled
        // This allows users to enable hyperlink mode via the checkbox
        if (!CAS.TYPE_NAME_STRING.equals(feature.getType())
                || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
            throw unsupportedFeatureTypeException(feature);
        }

        return new HyperlinkFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel) {
        var feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        return new HyperlinkFeatureEditor(aId, aOwner, aFeatureStateModel);
    }

    @Override
    public HyperlinkFeatureTraits createDefaultTraits() {
        return new HyperlinkFeatureTraits();
    }

    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS) {
        return null;
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
            throws de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException {
        if (aValue instanceof String value) {
            var fs = de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr(aCas, aAddress);
            FSUtil.setFeature(fs, aFeature.getName(), value);
        } else {
            super.setFeatureValue(aCas, aFeature, aAddress, aValue);
        }
    }

    @Override
    public Object getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS) {
        return FSUtil.getFeature(aFS, aFeature.getName(), String.class);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public String getId() {
        return "hyperlink";
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aLabel) {
        if (aLabel == null || aLabel.isBlank()) {
            return null;
        }

        LOG.debug("Rendering hyperlink value for feature {}: {}", aFeature.getName(), aLabel);

        // Try pipe-delimited format first (new format)
        if (!aLabel.startsWith("[") && !aLabel.startsWith("{")) {
            var sb = new StringBuilder();
            var entries = aLabel.split(";");
            for (var entry : entries) {
                var parts = entry.split("\\|", 2);
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                if (parts.length == 2) {
                    // Unescape and prefer text over url
                    String text = parts[0].replace("\\|", "|").replace("\\;", ";");
                    String url = parts[1].replace("\\|", "|").replace("\\;", ";");
                    sb.append(!text.isBlank() ? text : url);
                } else if (parts.length == 1) {
                    sb.append(parts[0].replace("\\|", "|").replace("\\;", ";"));
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }

        // Fallback: try legacy JSON format
        try {
            var data = MAPPER.readTree(aLabel);

            if (data.isArray()) {
                var sb = new StringBuilder();
                for (var item : data) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(renderSingleItem(item));
                }
                return sb.toString();
            }

            return renderSingleItem(data);
        } catch (Exception e) {
            LOG.debug("Error parsing hyperlink value: {}", aLabel, e);
            // If it's not JSON, just return the value as-is (backward compatibility)
            return aLabel;
        }
    }

    private String renderSingleItem(com.fasterxml.jackson.databind.JsonNode data) {
        // Prefer displaying the text if it's set
        if (data.has("text") && !data.get("text").asText().isBlank()) {
            String text = data.get("text").asText();
            LOG.debug("Rendering hyperlink text: {}", text);
            return text;
        }

        // Fall back to showing the URL
        if (data.has("url") && !data.get("url").asText().isBlank()) {
            String url = data.get("url").asText();
            LOG.debug("Rendering hyperlink url: {}", url);
            return url;
        }

        return null;
    }
}
