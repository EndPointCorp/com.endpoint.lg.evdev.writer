Event Device Writer
===================

Java package: com.endpoint.lg.evdev.writer

Liquid Galaxy Interactive Spaces activity that creates a virtual evdev input device and synthesizes input events from a route.


Copyright (C) 2015 Google Inc.
Copyright (C) 2015 End Point Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.

Unique Build Requirements
=========================
This activity has unique build requirements due to the native C++ libary it uses to interface with the UInput device. We are working with the Interactive Spaces developers to automate this process within the workbench. In the mean time, proceed as follows:

* Build the activity once (this will fail):
```
$ path/to/workbench/bin/isworkbench.bash path/to/workbench/com.endpoint.lg.evdev.writer build
```
* Build the native library (if this fails, check CLASSPATH and JAVA_PATH within):
```
$ cd path/to/workbench/com.endpoint.lg.evdev.writer/src/main/native
$ ./build-libs.sh
```
* Build the Activity again (now this should succeed):
```
$ path/to/workbench/bin/isworkbench.bash path/to/workbench/com.endpoint.lg.evdev.writer build
```
