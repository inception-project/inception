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
class FeedbackPanelExtension {
 
  /**
   * @param {string} feedbackPanelId    id of the feedbackPanel component that
   *                 incoming messages can be added to
   */
  constructor(feedbackPanelId) {
    this.panelId = feedbackPanelId;
  }
  
  /**
   * @param {string} msg   message that will be added as info type to the panel
   */
  addInfoToFeedbackPanel(msg) {
    this.addMsgToFeedbackPanel(msg, 'alert-info');
  }
  
  /**
   * @param {string} msg   message that will be added as error type to the panel
   */
  addWarningToFeedbackPanel(msg) {
    this.addMsgToFeedbackPanel(msg, 'alert-warning');
  }
 
  /**
   * @param {string} msg   message that will be added as error type to the panel
   */
  addErrorToFeedbackPanel(msg) {
    this.addMsgToFeedbackPanel(msg, 'alert-danger');
  }
 
  /**
   * @param {string} msg   message that will be added as info type to the panel
   * @param {string} msgClasse  class of the message item depending on message type (info or error)
   */
  addMsgToFeedbackPanel(msg, msgClass) {
    this.feedbackPanel = document.getElementById(this.panelId);

    // create item with new info content
    let msgItem = document.createElement('li');
    msgItem.classList.add('alert', 'alert-dismissible', msgClass);

    // add closable link
    let msgCloseLink = this.createCloseLink();
    msgItem.appendChild(msgCloseLink);

    // add message content
    let msgSpan = document.createElement('span');
    msgSpan.textContent = msg;
    msgItem.appendChild(msgSpan);

    // get or create list in feedbackPanel and add new message item to it
    var feedbackMsgList = this.feedbackPanel.querySelector('ul');
    if (feedbackMsgList == null){
      feedbackMsgList = document.createElement('ul');
      feedbackMsgList.className = 'feedbackPanel';
      this.feedbackPanel.appendChild(feedbackMsgList);
    }

    feedbackMsgList.appendChild(msgItem);
    bootstrapFeedbackPanelFade();
  }
  
  createCloseLink() {
    var closeLink = document.createElement('button');
    closeLink.classList.add('btn-close');
    closeLink.setAttribute('type', 'button');
    closeLink.setAttribute('data-bs-dismiss', 'alert');
    closeLink.setAttribute('aria-label', 'close');
    closeLink.href = "#";
    return closeLink;
  }
}
 