
# Time Warp<br>Virtual Time for the JVM

This library lets you create virtual-time clocks and install them as the JVM's clock to help with testing.

A virtual-time `Clock` will modify the operation of:
* `System.currentTimeMillis()`
* `RuntimeMXBean.getStartTime()`
* `System.nanoTime()`
* `Thread.sleep`
* `Object.wait(long)`
* `LockSupport.parkNanos`
* and any other operation relying on timeouts.

Use this library to slow-down/speed-up/manually control the JVM's clock to make your
timing-sensitive tests less flaky.

## Status

Early days.
We've begun using TimeWarp's `ScaledClock` (and `SystemClock`) in [Quasar](https://github.com/puniverse/quasar) tests.
`ManualClock` hasn't been tested, so it probably doesn't work yet.

**Fork note:** refactor and rename the fixed epcoh clock to offset clock and made some refactor. Manual and scaled clock where not much tested then. Offset
clock works fine and has been used on large application simulation and benchmarks.

## Usage

1. Clone and build the repository with `./gradlew` or use Maven artifact `co.paralleluniverse:timewarp:0.1.0-SNAPSHOT`
from the Sonatype snapshot repository (`https://oss.sonatype.org/content/repositories/snapshots`)

2. Add the JAR file to your bootstrap classpath with `-Xbootclasspath/a:[timewarp jar]` and as an agent
with `-javaagent:[timewarp jar]`

3. Install one of the provided clocks, `SystemClock`, `OffsetClock`,  `ScaledClock`, or `ManualClock`. This can be done either:
   * By code using the `VirtualClock` class. Please consult the [Javadocs](http://docs.paralleluniverse.co/timewarp/javadoc/) for detailed information.
   * By configuration, using agent command-line argument: `-javaagent:[timewarp jar]=[clock conf]`. See below for more information.

### Offset clock

This clock apply a constant offset to the system clock. This offset can be defined either by:
* the number of milliseconds to add : `-javaagent:[timewarp jar]=offset=[millis]`
* with an absolute date, the offset is calculated at the JVM start as the difference between the date and the JVM start date :
`-javaagent:[timewarp jar]=offset=@[ISO date]`
* a state file. The state file is a text file that contains the absolute date, like the previous option. The state file is periodically written with a date a
few minute ahead in the future to ensure that in case ensure that in case of JVM carsh or restart the next JVM execution won't see time that overlaps previous
execution.
Usage : `-javaagent:[timewarp jar]=offset=#[state file path]`

Since these clock has no impact on time relative function, you can use the method name filter in order to wrap only the needed methods :
`-javaagent:[timewarp jar]=includesMethods=System_currentTimeMillis:RuntimeMXBean_getStartTime,offset=...`

Examples:
* one day past in time : `-javaagent:[timewarp jar]=includesMethods=System_currentTimeMillis:RuntimeMXBean_getStartTime,offset=-86400000`
* simulate a JVM start at 2016-08-30 18:45:00 : `-javaagent:[timewarp jar]=includesMethods=System_currentTimeMillis:RuntimeMXBean_getStartTime,offset=@20160830T184500`
* use a state file in `~/.faketime` : `-javaagent:[timewarp jar]=includesMethods=System_currentTimeMillis:RuntimeMXBean_getStartTime,offset=#~/.faketime`
** initialize the file `~/.faketime` with the initial date, e.g. `20160830T184500`


## License

MIT

Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
