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
package de.tudarmstadt.ukp.clarin.webanno.model;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;
import de.tudarmstadt.ukp.inception.support.wicket.HasSymbol;

/**
 * Annotation anchoring mode.
 */
public enum AnchoringMode
    implements PersistentEnum, Serializable, HasSymbol
{
    /**
     * Any number of characters - allows zero-span annotations as well.
     */
    CHARACTERS("characters", true),

    /**
     * Single token - no zero-span annotations.
     */
    SINGLE_TOKEN("singleToken", false),

    /**
     * Any number of tokens - allows zero-span annotations as well.
     */
    TOKENS("tokens", true),

    /**
     * Any number of sentences - allows zero-span annotations as well.
     */
    SENTENCES("sentences", true);

    public static final AnchoringMode DEFAULT_ANCHORING_MODE = AnchoringMode.TOKENS;

    private static final PackageResourceReference SYMBOL_CHARACTERS = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "AnchoringMode_CHARACTERS.svg");

    private static final PackageResourceReference SYMBOL_SINGLE_TOKEN = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "AnchoringMode_SINGLE_TOKEN.svg");

    private static final PackageResourceReference SYMBOL_TOKENS = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "AnchoringMode_TOKENS.svg");

    private static final PackageResourceReference SYMBOL_SENTENCES = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "AnchoringMode_SENTENCES.svg");

    private final String id;
    private final boolean zeroSpanAllowed;

    AnchoringMode(String aId, boolean aZeroSpanAllowed)
    {
        id = aId;
        zeroSpanAllowed = aZeroSpanAllowed;
    }

    @Override
    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return getId();
    }

    @Override
    public String toString()
    {
        return getId();
    }

    public boolean isZeroSpanAllowed()
    {
        return zeroSpanAllowed;
    }

    public boolean allows(AnchoringMode aMode)
    {
        return switch (this) {
        case CHARACTERS -> true;
        case SINGLE_TOKEN -> aMode == SINGLE_TOKEN;
        case TOKENS -> aMode == SINGLE_TOKEN || aMode == TOKENS || aMode == SENTENCES;
        case SENTENCES -> aMode == SENTENCES;
        };
    }

    @Override
    public String symbol()
    {
        var symbolRef = switch (this) {
        case CHARACTERS -> SYMBOL_CHARACTERS;
        case SINGLE_TOKEN -> SYMBOL_SINGLE_TOKEN;
        case TOKENS -> SYMBOL_TOKENS;
        case SENTENCES -> SYMBOL_SENTENCES;
        };

        // return "<img src=\"" + RequestCycle.get().urlFor(symbolRef, null).toString()
        // + "\" style=\"max-height: 1rem; width: auto; object-fit: scale-down;\"/>";

        try (var is = symbolRef.getResource().getResourceStream().getInputStream()) {
            return IOUtils.toString(is, UTF_8);
        }
        catch (IOException | ResourceStreamNotFoundException e) {
            return "";
        }
    }
}
