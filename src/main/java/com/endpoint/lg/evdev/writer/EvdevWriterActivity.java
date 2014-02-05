/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.endpoint.lg.evdev.writer;

import com.endpoint.lg.support.domain.evdev.InputEvent;
import interactivespaces.InteractiveSpacesException;
import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.data.json.JsonNavigator;

import java.util.Map;

/**
 * An activity for replaying input events on a virtual device.
 */
public class EvdevWriterActivity extends BaseRoutableRosActivity {

  /**
   * The one and only device.
   */
  private UinputDeviceManagedResource device;

  /**
   * Handles an input event.
   * 
   * @param inputMessage
   *          an incoming input event message
   */
  private synchronized void handleInputEventMessage(final JsonNavigator message) {
    InputEvent event = new InputEvent(message);

    try {
      getLog().debug("Writing input message to uinput device: " + this.device.getInputDevice());
      this.device.getInputDevice().write(event);
    } catch (SimpleInteractiveSpacesException e) {
      throw new InteractiveSpacesException("Error writing input message:", e);
    }
  }

  /**
   * Handles incoming messages.
   * <p>
   * Empty/missing values default to 0.
   * <p>
   * Messages are ignored if the activity is not ACTIVE.
   */
  @Override
  public void onNewInputJson(String channelName, Map<String, Object> m) {
    if (!isActivated())
      return;

    getLog().debug("Got message on input channel " + channelName);
    getLog().debug(m);

    JsonNavigator message = new JsonNavigator(m);

    handleInputEventMessage(message);
  }

  /**
   * Instantiates a managed Uinput device.
   */
  @Override
  public void onActivitySetup() {
    device = new UinputDeviceManagedResource(getConfiguration());
    addManagedResource(device);
  }
}
