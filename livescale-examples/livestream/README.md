# The Livestream Application

Livestream is a demo application to showcase the Livescale toolkit.

## Requirements
* Maven (to build)
* Nephele with support for stream processing with latency constraints:
 * get it from https://github.com/bjoernlohrmann/stratosphere
* Maven Dependencies (maven will take care of these)
* Optional: An AMQP broker such as RabbitMQ

## How to build
Run 'mvn package' to compile. This builds everything but the Android app. The Android app must be built with Google's Android SDK.

## How to run
Livestream can be set up for an interactive demo or for a non-interactive benchmarking. 

### Option 1: Run as interactive demo
This is a complex deployment consisting of:
 * livestream-android: An Android app to record and upload live video to Nephele.
 * Nephele with support for stream processing with latency constraints (see above)
 * livestream-nephele: LivestreamDemoJob.java is a Nephele job that opens a TCP server socket to receive live video from the Android App, manipulate and reencode the video and broadcast it available as mpegts over http
 * livestream-dispatcher: The dispatcher is a Java application with a database, that coordinates between the Nephele job and the Android app via AMQP messaging.
 * An AMQP broker such as RabbitMQ

### Option 2: Run as non-interactive Nephele job
This deployment is useful for benchmarking and experimentation. It is much simpler and consists of:
 * Nephele with support for stream processing with latency constraints: https://github.com/bjoernlohrmann/stratosphere
 * livestream-nephele: LivescaleParallelJob.java is a Nephele job that reads "packetized" video files from local disks, decodes, manipulates and reencodes the video, and makes it available as mpegts over http.
 * A set of packetized video files.


