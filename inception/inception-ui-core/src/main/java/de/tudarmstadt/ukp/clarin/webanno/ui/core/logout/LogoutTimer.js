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
function LogoutTimer(sessionTimeoutSeconds) {
  var instance = this;

  var timeoutSeconds = sessionTimeoutSeconds;
  var expirationTime = null;
  var timer = null;

  this.tick = function () {
    var now = new Date();
    var remaining = Math.floor((expirationTime - now) / 1000);
    var remainingClamped = Math.max(0, remaining);
    var min = Math.floor(remainingClamped / 60).toString();

    var timerElement = $('#timer');
    if (timerElement.text() !== min) {
      timerElement.text(min);
    }

    if (remaining <= -10) {
      clearInterval(instance.timer);
      location.reload(true);
    }
  };

  this.start = function () {
    const TICK_RATE = 1000; // every second
    if (instance.timer) {
      clearInterval(instance.timer);
    }
    expirationTime = new Date(Date.now() + timeoutSeconds * 1000);
    instance.timer = setInterval(instance.tick, TICK_RATE);
    instance.tick();
  };

  if (typeof Wicket !== 'undefined') {
    // Restart timer on Wicket AJAX calls
    Wicket.Event.subscribe('/ajax/call/beforeSend', function () {
      instance.start();
    });
  }

  instance.start();
  
  // Also check immediately when the tab becomes visible again
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      instance.tick();
    }
  });
}
