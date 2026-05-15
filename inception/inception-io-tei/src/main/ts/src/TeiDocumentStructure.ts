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
import type { DocumentStructureStrategy } from '@inception-project/inception-js-api';

/**
 * Document structure strategy for TEI XML.
 *
 * TEI uses <div> elements as the section container and <head> as their
 * (optional) title. Divs nest naturally to express document hierarchy, so no
 * preprocessing is needed -- the navigator can build the outline directly
 * from DOM containment.
 *
 * Divs without a direct <head> child are skipped by the navigator (since
 * extractTitle returns undefined), which avoids polluting the outline with
 * structural divs that don't carry a meaningful heading.
 */
export class TeiDocumentStructure implements DocumentStructureStrategy {
    readonly sectionSelector = 'div';

    preprocess(): void {
        // TEI already has explicit nested section structure -- no DOM rewrite needed.
    }

    extractTitle(section: Element): string | undefined {
        const head = section.querySelector(':scope > head');
        const text = head?.textContent?.trim();
        return text || undefined;
    }

    scrollTarget(section: Element): Element {
        return section.querySelector(':scope > head') ?? section;
    }
}
