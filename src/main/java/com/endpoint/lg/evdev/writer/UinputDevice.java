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

import com.endpoint.lg.support.evdev.InputEvent;
import interactivespaces.SimpleInteractiveSpacesException;

import java.io.File;
import java.util.Vector;

/**
 * Interface for a native uinput virtual device.
 */
public class UinputDevice {
  /**
   * Name of the required native library.
   */
  private static final String UINPUT_NATIVE_LIB = "ispaces-uinput";

  /**
   * Name of the required native library on the filesystem.
   */
  private static final String UINPUT_NATIVE_FILE = "lib" + UINPUT_NATIVE_LIB + ".so";

  /**
   * Default, uninitialized handle value.
   */
  private static final int NULL_HANDLE = -1;

  /**
   * Reference to a native uinput device.
   */
  private int handle = NULL_HANDLE;

  /**
   * Creates a native uinput device.
   */
  private native static int createDevice(String uinput_path, String device_name, int id_vendor,
      int id_product, int version, int[] abs_min, int[] abs_max);

  /**
   * Writes an event to a native uinput device.
   */
  private native static boolean writeEvent(int fd_handle, int type, int code, int value);

  /**
   * Destroys a native uinput device.
   */
  private native static boolean destroyDevice(int fd_handle);

  /**
   * Initializes a uinput device with the given configuration.
   * 
   * @param uinput_path
   *          path to the uinput filesystem node
   * @param uinput_lib
   *          path to the native library
   * @param device_name
   *          display name for the virtual device
   * @param id_vendor
   *          unique id of the virtual device vendor
   * @param id_product
   *          unique id of the virtual device product
   * @param version
   *          version of the virtual device
   * @param abs_min
   *          minimum ABS values for each axis
   * @param abs_max
   *          maximum ABS values for each axis
   * @throws SimpleInteractiveSpacesException
   */
  public UinputDevice(String uinput_path, String uinput_lib, String device_name, int id_vendor,
      int id_product, int version, int[] abs_min, int[] abs_max)
      throws SimpleInteractiveSpacesException {

    File uinputFile = new File(uinput_path);

    if (!uinputFile.canWrite())
      throw new SimpleInteractiveSpacesException("No write access to Uinput file!");

    loadNativeLibs(uinput_lib);

    this.handle =
        createDevice(uinput_path, device_name, id_vendor, id_product, version, abs_min, abs_max);

    if (this.handle < 0) {
      this.handle = NULL_HANDLE;
      throw new SimpleInteractiveSpacesException(
          "Error creating Uinput device: invalid file descriptor!");
    }
  }

  /**
   * Writes an event to the virtual device.
   * <p>
   * See linux/input.h for type and code defines.
   * 
   * @param type
   *          event type
   * @param code
   *          event code
   * @param value
   *          event value
   * @throws SimpleInteractiveSpacesException
   */
  public void write(int type, int code, int value) throws SimpleInteractiveSpacesException {
    if (!this.ready())
      throw new SimpleInteractiveSpacesException(
          "Tried to write to an uninitialized Uinput device!");

    if (!writeEvent(this.handle, type, code, value))
      throw new SimpleInteractiveSpacesException("Error while writing to Uinput device!");
  }
  
  /**
   * Writes an <code>InputEvent</code> to the virtual device.
   * <p>
   * See linux/input.h for type and code defines.
   * 
   * @param type
   *          event type
   * @param code
   *          event code
   * @param value
   *          event value
   * @throws SimpleInteractiveSpacesException
   */
  public void write(InputEvent event) throws SimpleInteractiveSpacesException {
    if (!this.ready())
      throw new SimpleInteractiveSpacesException(
          "Tried to write to an uninitialized Uinput device!");

    if (!writeEvent(this.handle, event.getType(), event.getCode(), event.getValue()))
      throw new SimpleInteractiveSpacesException("Error while writing to Uinput device!");
  }

  /**
   * Destroys the virtual device.
   * 
   * @throws SimpleInteractiveSpacesException
   */
  public void destroy() throws SimpleInteractiveSpacesException {
    if (!this.ready())
      throw new SimpleInteractiveSpacesException("Tried to destroy an uninitialized Uinput device!");

    if (!destroyDevice(this.handle))
      throw new SimpleInteractiveSpacesException("Error while destroying Uinput device!");
  }

  /**
   * Returns true if the device is ready to write events.
   * 
   * @return true if device is initialized
   */
  public boolean ready() {
    return (this.handle != NULL_HANDLE);
  }

  /**
   * Helper class for discovering which native libraries are already loaded.
   * <p>
   * http://stackoverflow.com/questions/1007861/how-do-i-get-a-list-of-jni-
   * libraries-which-are-loaded/1008631#1008631
   */
  public static class ClassScope {
    private static java.lang.reflect.Field LIBRARIES;
    static {
      try {
        LIBRARIES = ClassLoader.class.getDeclaredField("loadedLibraryNames");
        LIBRARIES.setAccessible(true);
      } catch (NoSuchFieldException e) {
      }
    }

    public static String[] getLoadedLibraries(final ClassLoader loader) {
      try {
        final Vector<String> libraries = (Vector<String>) LIBRARIES.get(loader);
        return libraries.toArray(new String[] {});
      } catch (IllegalAccessException e) {
        return new String[0];
      }
    }
  }

  /**
   * Loads native libraries, if necessary.
   */
  private void loadNativeLibs(String uinputLib) {
    String jniLib = uinputLib;
    String[] libraries = ClassScope.getLoadedLibraries(ClassLoader.getSystemClassLoader());

    boolean alreadyLoaded = false;
    for (String lib : libraries) {
      if (lib.contains(UINPUT_NATIVE_FILE)) {
        alreadyLoaded = true;
      }
    }

    if (!alreadyLoaded)
      System.load(jniLib);
  }
}
