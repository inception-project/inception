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
import type { DiamAjax, VID } from '@inception-project/inception-js-api'
import { getInlineLabelClientRect, highlights, isPointInRect, NO_LABEL } from './ApacheAnnotatorVisualizer.svelte'
import { createPopper, type Instance } from '@popperjs/core'

export class ApacheAnnotatorSelector {
  private ajax: DiamAjax
  private root: Element

  private popup: Instance | undefined
  private popupContent: HTMLElement | undefined
  private popupAnchor: HTMLElement | undefined

  private clickCount = 0
  private clickTimer: number
  private CLICK_DELAY = 300

  public constructor (element: Element, ajax: DiamAjax) {
    this.ajax = ajax
    this.root = element

    this.root.addEventListener('mousedown', e => this.onMouseDown(e))
  }

  private onMouseDown (event: Event): void {
    // Destroy popup if clicked outside of popup
    if (!this.isVisible()) {
      return
    }

    if (event.target instanceof Node && this.popupContent?.contains(event.target as Node)) {
      return
    }

    this.destroyPopup()
  }

  private destroyPopup () {
    if (!this.popup) return

    this.popup.destroy()
    this.popupContent?.remove()
    this.popupAnchor?.remove()
    this.popup = undefined
    this.popupContent = undefined
    this.popupAnchor = undefined
  }

  public isVisible (): boolean {
    return this.popup !== undefined
  }

  public showSelector (event: Event): void {
    console.warn("showSelector")

    const mouseEvent = event as MouseEvent

    this.destroyPopup()

    const hls = event.target instanceof Node ? highlights(event.target) : []
    // No need to show selector if there is no annotation
    if (hls.length === 0) return

    if (hls.length === 1 || isPointInRect({ x: mouseEvent.clientX, y: mouseEvent.clientY }, getInlineLabelClientRect(hls[0]))) {
      // No need to show selector if there is only a single annotation or if the user clicked on the
      // inline label
      const vid = hls[0].getAttribute('data-iaa-id')
      if (!vid) return
      this.onClick(mouseEvent, vid)
      return
    }

    this.createPopup(mouseEvent, hls)
  }

  private createPopup (mouseEvent: MouseEvent, hls: HTMLElement[]) {
    this.popupAnchor = document.createElement('div')
    this.popupAnchor.style.position = 'absolute'
    this.popupAnchor.style.top = `${mouseEvent.clientY + window.scrollY}px`
    this.popupAnchor.style.left = `${mouseEvent.clientX}px`
    this.popupAnchor.style.pointerEvents = 'none'
    this.popupAnchor.style.visibility = 'hidden'
    this.root.ownerDocument.body.appendChild(this.popupAnchor)

    this.popupContent = document.createElement('div')
    this.popupContent.classList.add('iaa-menu')
    for (const hl of hls) {
      const vid = hl.getAttribute('data-iaa-id')
      if (!vid) continue

      const menuItem = document.createElement('div')
      menuItem.classList.add('iaa-menu-item')

      const label = hl.getAttribute('data-iaa-label') || NO_LABEL
      const labelArea = document.createElement('div')
      labelArea.classList.add('iaa-label')
      labelArea.textContent = label !== NO_LABEL ? label : 'no label'
      labelArea.style.cursor = 'pointer'
      labelArea.addEventListener('mouseup', e => this.onClick(e, vid))
      menuItem.appendChild(labelArea)

      const deleteButton = document.createElement('a')
      deleteButton.classList.add('iaa-btn')
      deleteButton.textContent = '❌'
      deleteButton.addEventListener('click', e => this.onDeleteAnnotation(e, vid))
      menuItem.appendChild(deleteButton)

      this.popupContent.appendChild(menuItem)
    }
    this.root.ownerDocument.body.appendChild(this.popupContent)

    this.popup = createPopper(this.popupAnchor, this.popupContent, { placement: 'top' })
  }

  /**
   * Distinguish between double clicks and single clicks . This is relevant when clicking on
   * annotations. For clicking on text nodes, this is not really relevant.
   */
  private onClick (event: MouseEvent, id: VID) {
    if (event.button != 0) return

    const singleClickAction = this.onSelectAnnotation.bind(this)
    const doubleClickAction = this.onExtensionAction.bind(this)

    event.stopPropagation()
    event.preventDefault()

    this.clickCount++
    console.warn(this.clickCount, id)
    if (this.clickCount === 1) {
      this.clickTimer = window.setTimeout(() => {
        try {
          singleClickAction(id) // perform single-click action
        } finally {
          this.clickCount = 0 // after action performed, reset counter
        }
      }, this.CLICK_DELAY)
    } else {
      if (this.clickTimer !== null) {
        clearTimeout(this.clickTimer) // prevent single-click action
      }
      try {
        doubleClickAction(id) // perform double-click action
      } finally {
        this.clickCount = 0 // after action performed, reset counter
      }
    }
  }

  private onSelectAnnotation (id: VID) {
    console.warn(`Selecting annotation ${id}`)
    this.destroyPopup()
    this.ajax.selectAnnotation(id)
  }

  private onExtensionAction (id: VID) {
    console.warn(`Trigger extension action on annotation ${id}`)
    this.destroyPopup()
    this.ajax.triggerExtensionAction(id)
  }

  private onDeleteAnnotation (event: Event, id: VID) {
    console.log(`Deleting annotation ${id}`)
    event.stopPropagation()
    this.destroyPopup()
    this.ajax.deleteAnnotation(id)
  }

  destroy (): void {
    this.destroyPopup()
  }
}
