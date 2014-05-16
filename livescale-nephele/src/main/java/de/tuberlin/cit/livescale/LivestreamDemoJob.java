/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package de.tuberlin.cit.livescale;

import java.io.IOException;

import de.tuberlin.cit.livescale.job.task.DecoderTask;
import de.tuberlin.cit.livescale.job.task.EncoderTask;
import de.tuberlin.cit.livescale.job.task.OverlayTask;
import de.tuberlin.cit.livescale.job.task.NetworkStreamSourceTask;
import de.tuberlin.cit.livescale.job.task.BroadcasterTask;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import de.tuberlin.cit.livescale.job.util.overlay.LogoOverlayProvider;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.client.JobExecutionException;
import eu.stratosphere.nephele.configuration.ConfigConstants;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.fs.Path;
import eu.stratosphere.nephele.io.DistributionPattern;
import eu.stratosphere.nephele.io.channels.ChannelType;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.jobgraph.JobGraphDefinitionException;
import eu.stratosphere.nephele.jobgraph.JobInputVertex;
import eu.stratosphere.nephele.jobgraph.JobOutputVertex;
import eu.stratosphere.nephele.jobgraph.JobTaskVertex;
import eu.stratosphere.nephele.streaming.ConstraintUtil;

/**
 * This version of the Nephele job is to be used as part of an
 * interactive demo deployment of the Livestream example, showcasing how to use
 * the Livescale toolkit to assemble large-scale, interactive video stream
 * applications.
 * 
 * Such a demo deployment consists of: (1) the Livestream Android
 * App (2) an AMQP broker (e.g. RabbitMQ) (3) a dispatcher server and (4) this
 * Nephele job. 
 * 
 * This Nephele job opens a TCP server port to receive live video from the
 * Livestream Android App, manipulates the video, reencodes it and makes it
 * available as mpegts over http. It coordinates with the Android App and the
 * dispatcher server via AMQP messaging.
 * 
 * @author Bjoern Lohrmann
 */
public class LivestreamDemoJob {

	public static void main(String[] args) {
		try {
			final JobGraph graph = new JobGraph("Livestream Demo Job");

			if (args.length != 1) {
				System.err.println("Parameters: <logoOverlayImage>");
				System.exit(1);
				return;
			}

			String logoOverlayImage = args[0];

			int degreeOfParallelism = 1;

			final JobInputVertex receiver = new JobInputVertex("Receiver",
					graph);
			receiver.setInputClass(NetworkStreamSourceTask.class);
			receiver.setNumberOfSubtasks(1);
			receiver.getConfiguration().setInteger(
					NetworkStreamSourceTask.TCP_SERVER_PORT, 43000);

			JobTaskVertex decoder = new JobTaskVertex("Decoder", graph);
			decoder.setNumberOfSubtasks(degreeOfParallelism);
			decoder.setTaskClass(DecoderTask.class);

			final JobTaskVertex overlay = new JobTaskVertex("Overlay", graph);
			overlay.setTaskClass(OverlayTask.class);
			overlay.getConfiguration().setString(
					LogoOverlayProvider.LOGO_OVERLAY_IMAGE, logoOverlayImage);

			final JobTaskVertex encoder = new JobTaskVertex("Encoder", graph);
			encoder.setNumberOfSubtasks(degreeOfParallelism);
			encoder.setTaskClass(EncoderTask.class);
			encoder.getConfiguration().setString(
					VideoEncoder.ENCODER_OUTPUT_FORMAT, "mpegts");

			final JobOutputVertex broadcaster = new JobOutputVertex(
					"Broadcaster", graph);
			broadcaster.setNumberOfSubtasks(1);
			broadcaster.setOutputClass(BroadcasterTask.class);
			broadcaster.getConfiguration().setString(
					BroadcasterTask.BROADCAST_TRANSPORT, "http");

			receiver.connectTo(decoder, ChannelType.NETWORK,
					DistributionPattern.BIPARTITE);
			decoder.connectTo(overlay, ChannelType.NETWORK,
					DistributionPattern.POINTWISE);
			overlay.connectTo(encoder, ChannelType.NETWORK,
					DistributionPattern.POINTWISE);
			encoder.connectTo(broadcaster, ChannelType.NETWORK,
					DistributionPattern.BIPARTITE);

			ConstraintUtil.defineAllLatencyConstraintsBetween(
					receiver.getForwardConnection(0),
					encoder.getForwardConnection(0), 100);

			// Configure instance sharing
			decoder.setVertexToShareInstancesWith(receiver);
			overlay.setVertexToShareInstancesWith(receiver);
			encoder.setVertexToShareInstancesWith(receiver);
			broadcaster.setVertexToShareInstancesWith(receiver);

			// decoder.setVertexToShareInstancesWith(fileStreamSource);
			// merger.setVertexToShareInstancesWith(decoder);
			// overlay.setVertexToShareInstancesWith(merger);
			// encoder.setVertexToShareInstancesWith(overlay);
			// output.setVertexToShareInstancesWith(encoder);

			// Create jar file for job deployment
			Process p = Runtime.getRuntime().exec("mvn clean package");
			if (p.waitFor() != 0) {
				System.out.println("Failed to build livescale-nephele jar");
				System.exit(1);
			}

			String userHome = System.getProperty("user.home");

			graph.addJar(new Path("target/livescale-nephele-git.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/com/rabbitmq/amqp-client/2.8.4/amqp-client-2.8.4.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/de/tuberlin/cit/livescale-messaging/git/livescale-messaging-git.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/xuggle/xuggle-xuggler/5.4/xuggle-xuggler-5.4.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/org/slf4j/slf4j-api/1.6.4/slf4j-api-1.6.4.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/commons-cli/commons-cli/1.1/commons-cli-1.1.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/ch/qos/logback/logback-classic/1.0.0/logback-classic-1.0.0.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/ch/qos/logback/logback-core/1.0.0/logback-core-1.0.0.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/org/twitter4j/twitter4j-core/3.0.5/twitter4j-core-3.0.5.jar"));
			graph.addJar(new Path(
					userHome
							+ "/.m2/repository/org/twitter4j/twitter4j-stream/3.0.5/twitter4j-stream-3.0.5.jar"));

			Configuration conf = new Configuration();
			conf.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY,
					"127.0.0.1");
			conf.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY,
					ConfigConstants.DEFAULT_JOB_MANAGER_IPC_PORT);

			final JobClient jobClient = new JobClient(graph, conf);
			jobClient.submitJobAndWait();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (JobGraphDefinitionException e) {
			e.printStackTrace();
		} catch (JobExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}

	}

}
