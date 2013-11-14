package de.tuberlin.cit.livescale;

import java.io.IOException;

import de.tuberlin.cit.livescale.job.task.DecoderTask;
import de.tuberlin.cit.livescale.job.task.EncoderTask;
import de.tuberlin.cit.livescale.job.task.OverlayTask;
import de.tuberlin.cit.livescale.job.task.SingleFileStreamSourceTask;
import de.tuberlin.cit.livescale.job.task.VideoReceiverTask;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import de.tuberlin.cit.livescale.job.util.overlay.TwitterOverlayProvider;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.client.JobExecutionException;
import eu.stratosphere.nephele.configuration.ConfigConstants;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.fs.Path;
import eu.stratosphere.nephele.io.channels.ChannelType;
import eu.stratosphere.nephele.io.compression.CompressionLevel;
import eu.stratosphere.nephele.jobgraph.JobFileInputVertex;
import eu.stratosphere.nephele.jobgraph.JobFileOutputVertex;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.jobgraph.JobGraphDefinitionException;
import eu.stratosphere.nephele.jobgraph.JobTaskVertex;

public final class JobWithFileInput {

	public static void main(final String[] args) {

		if (args.length != 2) {
			System.out
				.println("Parameters: <input file> <output file>");
			System.exit(1);
		}

		try {
			final JobGraph graph = new JobGraph("Streaming Job with File Input");

			final JobFileInputVertex fileStreamSource = new JobFileInputVertex("FileStreamSource", graph);
			fileStreamSource.setFileInputClass(SingleFileStreamSourceTask.class);
			fileStreamSource.setNumberOfSubtasks(1);
			fileStreamSource.setFilePath(new Path("file://" + args[0]));

			final JobTaskVertex decoder = new JobTaskVertex("Decoder", graph);
			decoder.setTaskClass(DecoderTask.class);

			final JobTaskVertex overlay = new JobTaskVertex("Stream Overlay", graph);
			overlay.setTaskClass(OverlayTask.class);
			overlay.getConfiguration().setString(TwitterOverlayProvider.TWITTER_USERNAME_KEY, "danielwarneke");
			overlay.getConfiguration().setString(TwitterOverlayProvider.TWITTER_PASSWORD_KEY, "T7$dDp1");
			overlay.getConfiguration().setString(TwitterOverlayProvider.TWITTER_KEYWORD_KEY, "berlin");

			final JobTaskVertex encoder = new JobTaskVertex("Encoder", graph);
			encoder.setTaskClass(EncoderTask.class);
			encoder.getConfiguration().setString(VideoEncoder.ENCODER_OUTPUT_FORMAT, "flv");
			
			final JobFileOutputVertex output = new JobFileOutputVertex("Receiver", graph);
			output.setFileOutputClass(VideoReceiverTask.class);
			output.setFilePath(new Path("file://" + args[1]));

			fileStreamSource.connectTo(decoder, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION);
			decoder.connectTo(overlay, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION);
			overlay.connectTo(encoder, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION);
			encoder.connectTo(output, ChannelType.INMEMORY, CompressionLevel.NO_COMPRESSION);
			
			Process p = Runtime.getRuntime().exec("mvn package");
			p.waitFor();

			String userHome = System.getProperty("user.home");
			
			graph.addJar(new Path("target/livestream-0.0.1.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/xuggle/xuggle-xuggler/5.4/xuggle-xuggler-5.4.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/org/slf4j/slf4j-api/1.6.4/slf4j-api-1.6.4.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/commons-cli/commons-cli/1.1/commons-cli-1.1.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/ch/qos/logback/logback-classic/1.0.0/logback-classic-1.0.0.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/ch/qos/logback/logback-core/1.0.0/logback-core-1.0.0.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/org/twitter4j/twitter4j-core/2.2.5/twitter4j-core-2.2.5.jar"));
			graph.addJar(new Path(userHome + "/.m2/repository/org/twitter4j/twitter4j-stream/2.2.5/twitter4j-stream-2.2.5.jar"));

			Configuration conf = new Configuration();
			conf.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, "127.0.0.1");
			conf.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, ConfigConstants.DEFAULT_JOB_MANAGER_IPC_PORT);

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
