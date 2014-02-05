#include "com_endpoint_lg_evdev_writer_UinputDevice.h"
#include <linux/input.h>
#include <linux/uinput.h>
#include <linux/types.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h> // TODO: switch to JVM-friendly assert

// safe copy of Java array to native array
void copy_native_int_array(JNIEnv *env, int *nv_array, jintArray jni_array, size_t maxlen) {
  size_t sz = env->GetArrayLength(jni_array);
  assert(sz == maxlen);

  jint* buffer = env->GetIntArrayElements(jni_array, 0);
  for(int i = 0; i < sz; i++) {
    nv_array[i] = buffer[i];
  }
  env->ReleaseIntArrayElements(jni_array, buffer, 0);
}

// helper for errors in device creation
int creation_error(const char* message) {
  // TODO: throw a Java exception
  #ifdef DEBUG
  fputs(message, stderr);
  fputs("\n", stderr);
  #endif

  return -1;
}

// helper for enabling uinput event codes en masse
int allow_all_codes(int fd, int ui_bit, int max) {
  int status;

  for(int code = 0; code < max; code++) {
    status = ioctl(fd, ui_bit, code);
    if (status != 0)
      return -1;
  }

  return 0;
}

// create a UInput device and return the file descriptor as a handle
JNIEXPORT jint JNICALL Java_com_endpoint_lg_evdev_writer_UinputDevice_createDevice(JNIEnv *env, jclass cls, jstring uinput_path, jstring dev_name, jint id_vendor, jint id_product, jint version, jintArray abs_min, jintArray abs_max) {
  struct uinput_user_dev uidev;
  int status;

  // open the uinput node at the provided path
  const char *nv_uinput_path = env->GetStringUTFChars(uinput_path, 0);

  int fd = open(nv_uinput_path, O_WRONLY | O_NONBLOCK);
  if (fd < 0) {
    #ifdef DEBUG
    perror("opening the uinput node");
    #endif
    return -1;
  }

  env->ReleaseStringUTFChars(uinput_path, nv_uinput_path);

  /*** specify allowed event types and codes ***/

  // EV_KEY
  status = ioctl(fd, UI_SET_EVBIT, EV_KEY);
  if (status != 0)
    return creation_error("error activating EV_KEY type");

  status = allow_all_codes(fd, UI_SET_KEYBIT, KEY_MAX);
  if (status != 0)
    return creation_error("error activating EV_KEY codes");

  // EV_REL
  status = ioctl(fd, UI_SET_EVBIT, EV_REL);
  if (status != 0)
    return creation_error("error activating EV_REL type");

  allow_all_codes(fd, UI_SET_RELBIT, REL_MAX);
  if (status != 0)
    return creation_error("error activating EV_REL codes");

  // EV_ABS
  status = ioctl(fd, UI_SET_EVBIT, EV_ABS);
  if (status != 0)
    return creation_error("error activating EV_ABS type");

  allow_all_codes(fd, UI_SET_ABSBIT, ABS_MAX);
  if (status != 0)
    return creation_error("error activating EV_ABS codes");

  // EV_SYN
  status = ioctl(fd, UI_SET_EVBIT, EV_SYN);
  if (status != 0)
    return creation_error("error activating EV_SYN type");

  // initialize the user device struct
  memset(&uidev, 0, sizeof(uidev));

  // set the device name -- grab the string from JVM
  const char *nv_dev_name = env->GetStringUTFChars(dev_name, 0);

  strncpy(uidev.name, nv_dev_name, UINPUT_MAX_NAME_SIZE);

  env->ReleaseStringUTFChars(dev_name, nv_dev_name);

  // set more device attributes
  uidev.id.bustype = BUS_USB; // XXX: this is a big assumption
  uidev.id.vendor  = (__u16)id_vendor;
  uidev.id.product = (__u16)id_product;
  uidev.id.version = (__u16)version;

  // set ABS min/max values
  copy_native_int_array(env, uidev.absmin, abs_min, ABS_CNT);
  copy_native_int_array(env, uidev.absmax, abs_max, ABS_CNT);

  // write our device information
  status = write(fd, &uidev, sizeof(uidev));
  if (status != sizeof(uidev))
    return creation_error("error writing uinput_user_dev");

  // create the device
  status = ioctl(fd, UI_DEV_CREATE);
  if (status != 0)
    return creation_error("error on ioctl UI_DEV_CREATE");

  #ifdef DEBUG
  fprintf(
    stderr,
    "created device: %s vendor: %d product: %d version: %d\n",
    uidev.name, uidev.id.vendor, uidev.id.product, uidev.id.version
  );
  #endif

  // fd is now a handle for the user device
  return fd;
}

// write and sync an event to the provided fd handle
JNIEXPORT jboolean JNICALL Java_com_endpoint_lg_evdev_writer_UinputDevice_writeEvent(JNIEnv *env, jclass cls, jint fd, jint type, jint code, jint value) {
  struct input_event ev;

  // initialize the event buffer
  memset(&ev, 0, sizeof(ev));

  // timestamp is ignored by uinput, so skip this part
  //gettimeofday(&ev.time, NULL);

  // send out the event
  ev.type = type;
  ev.code = code;
  ev.value = value;

  int num_wrote = write(fd, &ev, sizeof(ev));

  if (num_wrote != sizeof(ev))
    return false;

  #ifdef DEBUG
  fprintf(
    stderr,
    "wrote %d bytes.  type: %d code: %d value: %d\n",
        num_wrote, ev.type, ev.code, ev.value
  );
  #endif

  return true;
}

// clean up the ioctl and close the file
JNIEXPORT jboolean JNICALL Java_com_endpoint_lg_evdev_writer_UinputDevice_destroyDevice(JNIEnv *env, jclass cls, jint fd) {
  int status;

  status = ioctl(fd, UI_DEV_DESTROY);
  if (status != 0)
    return false;

  status = close(fd);
  if (status != 0)
    return false;

  return true;
}
