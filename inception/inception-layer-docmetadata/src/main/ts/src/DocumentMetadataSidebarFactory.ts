/*
 * Licensed to the Technische Universitat Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universitat Darmstadt
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

import {
    AnnotationOutEvent,
    AnnotationOverEvent,
    type Annotation,
    type Comment,
    type VID,
} from '@inception-project/inception-js-api';
import { factory as diamAjaxFactory } from '@inception-project/inception-diam';
import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte';
import { mount, unmount } from 'svelte';

interface Props {
    ajaxEndpointUrl: string;
}

type SidebarKind = 'annotation' | 'recommendation' | 'curation';

const ITEM_SELECTOR = '[data-annotation-sidebar-item="true"]';

export default class DocumentMetadataSidebarFactory {
    private readonly root: HTMLElement;

    private readonly popover: ReturnType<typeof mount>;

    private readonly onMouseOver = (event: MouseEvent) => {
        const item = this.findItem(event.target);
        if (!item) {
            return;
        }

        if (this.findItem(event.relatedTarget) === item) {
            return;
        }

        const annotation = this.toAnnotation(item);
        if (!annotation) {
            return;
        }

        this.root.dispatchEvent(new AnnotationOverEvent(annotation, event));
    };

    private readonly onMouseOut = (event: MouseEvent) => {
        const item = this.findItem(event.target);
        if (!item) {
            return;
        }

        if (this.findItem(event.relatedTarget) === item) {
            return;
        }

        const annotation = this.toAnnotation(item);
        if (!annotation) {
            return;
        }

        this.root.dispatchEvent(new AnnotationOutEvent(annotation, event));
    };

    constructor(args: { target: HTMLElement; props: Props }) {
        this.root = args.target;

        const ajax = diamAjaxFactory().createAjaxClient(args.props.ajaxEndpointUrl);
        this.popover = mount(AnnotationDetailPopOver, {
            target: this.root.ownerDocument.body,
            props: {
                root: this.root,
                ajax,
            },
        });

        this.root.addEventListener('mouseover', this.onMouseOver);
        this.root.addEventListener('mouseout', this.onMouseOut);
    }

    $destroy() {
        this.root.removeEventListener('mouseover', this.onMouseOver);
        this.root.removeEventListener('mouseout', this.onMouseOut);
        unmount(this.popover);
    }

    private findItem(target: EventTarget | null): HTMLElement | null {
        if (!(target instanceof Element)) {
            return null;
        }

        return target.closest(ITEM_SELECTOR) as HTMLElement | null;
    }

    private toAnnotation(item: HTMLElement): Annotation | undefined {
        const vid = item.dataset.annotationSidebarVid as VID | undefined;
        const layerId = Number(item.dataset.annotationSidebarLayerId);
        const layerName = item.dataset.annotationSidebarLayerName;
        const kind = (item.dataset.annotationSidebarKind as SidebarKind | undefined) ?? 'annotation';
        const label = item.dataset.annotationSidebarLabel?.trim() || undefined;
        const score = Number(item.dataset.annotationSidebarScore ?? Number.NaN);

        if (!vid || !Number.isFinite(layerId) || !layerName) {
            return undefined;
        }

        return {
            document: undefined as never,
            layer: {
                id: layerId,
                name: layerName,
                type: kind,
            },
            vid,
            label,
            score: Number.isFinite(score) ? score : undefined,
            hideScore: !Number.isFinite(score) || score === 0,
            comments: this.buildComments(vid, kind),
        };
    }

    private buildComments(vid: VID, kind: SidebarKind): Comment[] | undefined {
        if (kind === 'annotation') {
            return undefined;
        }

        const label = kind === 'recommendation' ? 'Recommendation' : 'Curation item';

        return [
            {
                targetId: vid,
                type: 'info',
                comment: label,
            },
        ];
    }
}