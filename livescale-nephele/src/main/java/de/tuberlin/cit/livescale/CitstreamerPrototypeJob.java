package de.tuberlin.cit.livescale;

import de.tuberlin.cit.livescale.job.task.DecoderTask;
import de.tuberlin.cit.livescale.job.task.EncoderTask;
import de.tuberlin.cit.livescale.job.task.OverlayTask;
import de.tuberlin.cit.livescale.job.task.PacketizedStreamHandler;
import de.tuberlin.cit.livescale.job.task.VideoReceiverTask;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import de.tuberlin.cit.livescale.job.util.overlay.LogoOverlayProvider;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.configuration.GlobalConfiguration;
import eu.stratosphere.nephele.fs.Path;
import eu.stratosphere.nephele.io.DistributionPattern;
import eu.stratosphere.nephele.io.channels.ChannelType;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.jobgraph.JobInputVertex;
import eu.stratosphere.nephele.jobgraph.JobOutputVertex;
import eu.stratosphere.nephele.jobgraph.JobTaskVertex;
import eu.stratosphere.nephele.streaming.ConstraintUtil;

public final class CitstreamerPrototypeJob {

	public static void main(final String[] args) {
		try {
			final JobGraph graph = new JobGraph(
					"CitStreamer Job with network input");

			final JobInputVertex networkStreamSource = new JobInputVertex(
					"StreamHandler", graph);
			networkStreamSource.setInputClass(PacketizedStreamHandler.class);
			networkStreamSource.setNumberOfSubtasks(1);
			networkStreamSource.getConfiguration().setInteger(
					PacketizedStreamHandler.STREAM_HANDLER_SERVER_PORT, 43000);

			final JobTaskVertex decoder = new JobTaskVertex("Decoder", graph);
			decoder.setTaskClass(DecoderTask.class);

			final JobTaskVertex overlay = new JobTaskVertex("Overlay", graph);
			overlay.setTaskClass(OverlayTask.class);
			overlay.getConfiguration().setString(
					LogoOverlayProvider.LOGO_OVERLAY_IMAGE,
					"/home/bjoern/logo.png");

			final JobTaskVertex encoder = new JobTaskVertex("Encoder", graph);
			encoder.setTaskClass(EncoderTask.class);
			encoder.getConfiguration().setString(
					VideoEncoder.ENCODER_OUTPUT_FORMAT, "mpegts");

			final JobOutputVertex output = new JobOutputVertex("Receiver",
					graph);
			output.setOutputClass(VideoReceiverTask.class);
			output.getConfiguration().setString(
					VideoReceiverTask.BROADCAST_TRANSPORT, "http");

			networkStreamSource.connectTo(decoder, ChannelType.NETWORK,
					DistributionPattern.BIPARTITE);
			overlay.connectTo(encoder, ChannelType.NETWORK,
					DistributionPattern.POINTWISE);
			encoder.connectTo(output, ChannelType.NETWORK,
					DistributionPattern.BIPARTITE);

			decoder.setVertexToShareInstancesWith(networkStreamSource);
			overlay.setVertexToShareInstancesWith(networkStreamSource);
			encoder.setVertexToShareInstancesWith(networkStreamSource);
			output.setVertexToShareInstancesWith(networkStreamSource);

			ConstraintUtil.defineAllLatencyConstraintsBetween(
					networkStreamSource, output, 100, false, -1, false, -1);

			// Process p = Runtime.getRuntime().exec("mvn package");
			// p.waitFor();
			//
			// String userHome = System.getProperty("user.home");
			//
			// graph.addJar(new Path("target/livestream-streamserver-git.jar"));
			// graph.addJar(new Path(userHome +
			// "/.m2/repository/xuggle/xuggle-xuggler/5.4/xuggle-xuggler-5.4.jar"));
			// graph.addJar(new Path(userHome +
			// "/.m2/repository/org/slf4j/slf4j-api/1.6.4/slf4j-api-1.6.4.jar"));
			// graph.addJar(new Path(userHome +
			// "/.m2/repository/commons-cli/commons-cli/1.1/commons-cli-1.1.jar"));
			// graph.addJar(new Path(userHome
			// +
			// "/.m2/repository/ch/qos/logback/logback-classic/1.0.0/logback-classic-1.0.0.jar"));
			// graph
			// .addJar(new Path(userHome +
			// "/.m2/repository/ch/qos/logback/logback-core/1.0.0/logback-core-1.0.0.jar"));
			// graph.addJar(new Path(userHome +
			// "/.m2/repository/com/rabbitmq/amqp-client/2.8.4/amqp-client-2.8.4.jar"));
			// graph.addJar(new
			// Path("../livestream-messaging/target/livestream-messaging-git.jar"));

			graph.addJar(new Path(args[0] + "/lib/livescale-nephele-git.jar"));
			graph.addJar(new Path(args[0] + "/lib/xuggle-xuggler-5.4.jar"));
			graph.addJar(new Path(args[0] + "/lib/slf4j-api-1.6.4.jar"));
			graph.addJar(new Path(args[0] + "/lib/commons-cli-1.1.jar"));
			graph.addJar(new Path(args[0] + "/lib/logback-classic-1.0.0.jar"));
			graph.addJar(new Path(args[0] + "/lib/logback-core-1.0.0.jar"));
			graph.addJar(new Path(args[0] + "/lib/amqp-client-2.8.4.jar"));
			graph.addJar(new Path(args[0] + "/lib/livescale-messaging-git.jar"));

			GlobalConfiguration.loadConfiguration(args[0] + "/conf");
			Configuration conf = GlobalConfiguration.getConfiguration();

			final JobClient jobClient = new JobClient(graph, conf);
			jobClient.submitJobAndWait();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
