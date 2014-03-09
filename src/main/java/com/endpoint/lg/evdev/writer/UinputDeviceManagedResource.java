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

import com.endpoint.lg.support.evdev.EventCodes;
import interactivespaces.InteractiveSpacesException;
import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.configuration.Configuration;
import interactivespaces.util.resource.ManagedResource;

/**
 * @author Keith M. Hughes
 */
public class UinputDeviceManagedResource implements ManagedResource {
  /**
   * Configuration key for the location of the uinput file node.
   */
  private static final String CONFIGURATION_NAME_UINPUT_LOCATION = "lg.evdev.uinput.location";

  /**
   * Configuration key for the location of the uinput jni library.
   */
  private static final String CONFIGURATION_NAME_UINPUT_JNILIB = "lg.evdev.uinput.jnilib";

  /**
   * Name of the required native library on the filesystem.
   */
  private static final String DEFAULT_UINPUT_NATIVE_FILE = "libispaces-uinput.so";

  /**
   * Configuration key for the device name as seen by xinput.
   */
  private static final String CONFIGURATION_NAME_DEVICE_XINPUT_NAME = "lg.evdev.device.xinputName";

  /**
   * Configuration key for the device vendor ID.
   */
  private static final String CONFIGURATION_NAME_DEVICE_VENDOR = "lg.evdev.device.vendor";

  /**
   * Configuration key for the device product ID.
   */
  private static final String CONFIGURATION_NAME_DEVICE_PRODUCT = "lg.evdev.device.product";

  /**
   * Configuration key for the device version.
   */
  private static final String CONFIGURATION_NAME_DEVICE_VERSION = "lg.evdev.device.version";

  /**
   * Configuration key base for the minimum ABS values.
   */
  private static final String CONFIGURATION_NAME_DEVICE_ABS_MIN = "lg.evdev.device.abs.min";

  /**
   * Configuration key base for the maximum ABS values.
   */
  private static final String CONFIGURATION_NAME_DEVICE_ABS_MAX = "lg.evdev.device.abs.max";

  /**
   * The configuration for the input device.
   */
  private final Configuration config;

  /**
   * The input device.
   */
  private UinputDevice inputDevice;

  /**
   * Instantiates a new uinput device manager with the given configuration.
   * 
   * @param config the activity configuration
   */
  public UinputDeviceManagedResource(Configuration config) {
    this.config = config;
  }

  /**
   * Creates a new uinput device, using settings from the activity configuration.
   */
  @Override
  public void startup() {
    String uinputLib =
        config.getPropertyString(CONFIGURATION_NAME_UINPUT_JNILIB,
            config.getPropertyString("activity.installdir") + "/" + DEFAULT_UINPUT_NATIVE_FILE);
    String uinputPath = config.getRequiredPropertyString(CONFIGURATION_NAME_UINPUT_LOCATION);
    String device_name = config.getRequiredPropertyString(CONFIGURATION_NAME_DEVICE_XINPUT_NAME);
    int id_vendor = config.getRequiredPropertyInteger(CONFIGURATION_NAME_DEVICE_VENDOR);
    int id_product = config.getRequiredPropertyInteger(CONFIGURATION_NAME_DEVICE_PRODUCT);
    int version = config.getRequiredPropertyInteger(CONFIGURATION_NAME_DEVICE_VERSION);

    // read all available ABS min/max configuration values
    int[] abs_min = new int[EventCodes.ABS_CNT];
    int[] abs_max = new int[EventCodes.ABS_CNT];

    for (int i = 0; i < EventCodes.ABS_MAX; i++) {
      abs_min[i] = config.getPropertyInteger(CONFIGURATION_NAME_DEVICE_ABS_MIN + "." + i, 0);
      abs_max[i] = config.getPropertyInteger(CONFIGURATION_NAME_DEVICE_ABS_MAX + "." + i, 0);
    }

    try {
      inputDevice =
          new UinputDevice(uinputPath, uinputLib, device_name, id_vendor, id_product, version,
              abs_min, abs_max);
    } catch (SimpleInteractiveSpacesException e) {
      throw new InteractiveSpacesException("Could not allocate a UinputDevice", e);
    }
  }

  /**
   * Cleans up the input device.
   */
  @Override
  public void shutdown() {
    try {
      inputDevice.destroy();
      inputDevice = null;
    } catch (Exception e) {
      throw new InteractiveSpacesException("Could not shut down UinputDevice", e);
    }
  }

  /**
   * Get the input device.
   * 
   * @return the input device
   */
  public UinputDevice getInputDevice() {
    return inputDevice;
  }
}
