package de.tuberlin.cit.livescale;

import de.tuberlin.cit.livescale.job.task.DecoderTask;
import de.tuberlin.cit.livescale.job.task.EncoderTask;
import de.tuberlin.cit.livescale.job.task.PacketizedStreamHandler;
import de.tuberlin.cit.livescale.job.task.VideoReceiverTask;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.configuration.ConfigConstants;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.fs.Path;
import eu.stratosphere.nephele.io.DistributionPattern;
import eu.stratosphere.nephele.io.channels.ChannelType;
import eu.stratosphere.nephele.io.compression.CompressionLevel;
import eu.stratosphere.nephele.jobgraph.JobFileOutputVertex;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.jobgraph.JobInputVertex;
import eu.stratosphere.nephele.jobgraph.JobTaskVertex;

public final class CitstreamerPrototypeJob {

	public static void main(final String[] args) {
		try {
			final JobGraph graph = new JobGraph("CitStreamer Job with network input");

			final JobInputVertex networkStreamSource = new JobInputVertex("StreamHandler", graph);
			networkStreamSource.setInputClass(PacketizedStreamHandler.class);
			networkStreamSource.setNumberOfSubtasks(1);
			networkStreamSource.getConfiguration()
				.setInteger(PacketizedStreamHandler.STREAM_HANDLER_SERVER_PORT, 43000);

			final JobTaskVertex decoder = new JobTaskVertex("Decoder", graph);
			decoder.setTaskClass(DecoderTask.class);

			final JobTaskVertex encoder = new JobTaskVertex("Encoder", graph);
			encoder.setTaskClass(EncoderTask.class);
			encoder.getConfiguration().setString(VideoEncoder.ENCODER_OUTPUT_FORMAT, "mpegts");

			final JobFileOutputVertex output = new JobFileOutputVertex("Receiver", graph);
			output.setFileOutputClass(VideoReceiverTask.class);
			output.setFilePath(new Path("http://foobar"));

			networkStreamSource.connectTo(decoder, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION, DistributionPattern.BIPARTITE);
			decoder.connectTo(encoder, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION, DistributionPattern.POINTWISE);
			encoder.connectTo(output, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION, DistributionPattern.BIPARTITE);

			Process p = Runtime.getRuntime().exec("mvn package");
			p.waitFor();

			String userHome = System.getProperty("user.home");

//			graph.addJar(new Path("target/livestream-streamserver-0.0.1.jar"));
			graph.addJar(new Path("target/livestream-streamserver-git.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/xuggle/xuggle-xuggler/5.4/xuggle-xuggler-5.4.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/org/slf4j/slf4j-api/1.6.4/slf4j-api-1.6.4.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/commons-cli/commons-cli/1.1/commons-cli-1.1.jar"));
			graph.addJar(new Path(userHome
				+ "/.m2/repository/ch/qos/logback/logback-classic/1.0.0/logback-classic-1.0.0.jar"));
			graph
				.addJar(new Path(userHome + "/.m2/repository/ch/qos/logback/logback-core/1.0.0/logback-core-1.0.0.jar"));
			graph.addJar(new Path(userHome +
				"/.m2/repository/com/rabbitmq/amqp-client/2.8.4/amqp-client-2.8.4.jar"));
//			graph.addJar(new Path(userHome
//				+ "/.m2/repository/de/tuberlin/cit/livestream-messaging/0.0.1/livestream-messaging-0.0.1.jar"));
			graph.addJar(new Path("../livestream-messaging/target/livestream-messaging-git.jar"));

//			 graph.addJar(new Path(args[0] + "/lib/livestream-streamserver-0.0.1.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/xuggle-xuggler-5.4.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/slf4j-api-1.6.4.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/commons-cli-1.1.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/logback-classic-1.0.0.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/logback-core-1.0.0.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/amqp-client-2.8.4.jar"));
//			 graph.addJar(new Path(args[0] + "/lib/livestream-messaging-0.0.1.jar"));

			Configuration conf = new Configuration();
			conf.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, "127.0.0.1");
			conf.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, ConfigConstants.DEFAULT_JOB_MANAGER_IPC_PORT);

			final JobClient jobClient = new JobClient(graph, conf);
			jobClient.submitJobAndWait();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
