package de.tuberlin.cit.livescale;

import java.io.IOException;

import de.tuberlin.cit.livescale.job.task.DecoderTask;
import de.tuberlin.cit.livescale.job.task.EncoderTask;
import de.tuberlin.cit.livescale.job.task.MergeTask;
import de.tuberlin.cit.livescale.job.task.MultiFileStreamSourceTask;
import de.tuberlin.cit.livescale.job.task.OverlayTask;
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
import eu.stratosphere.nephele.jobgraph.JobFileOutputVertex;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.jobgraph.JobGraphDefinitionException;
import eu.stratosphere.nephele.jobgraph.JobInputVertex;
import eu.stratosphere.nephele.jobgraph.JobTaskVertex;

public class ParallelJob {

	private static final String INSTANCE_TYPE = "default";

	private static final int NUMBER_OF_SUBTASKS_PER_INSTANCE = 1;

	public static void main(final String[] args) {

		if (args.length != 4) {
			System.err
				.println("Parameters: <video-directory> <degree of parallelism> <number of streams> <number of stream per group>");
			System.exit(1);
			return;
		}

		int degreeOfParallelism;
		try {
			degreeOfParallelism = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		if (degreeOfParallelism < 1) {
			System.err.println("Degree of parallelism must be greater than 0");
			System.exit(1);
			return;
		}

		int numberOfStreams;
		try {
			numberOfStreams = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		if (numberOfStreams < 1) {
			System.err.println("Number of streams must be greater than 0");
			System.exit(1);
			return;
		}

		int numberOfStreamsPerGroup;
		try {
			numberOfStreamsPerGroup = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		if (numberOfStreamsPerGroup < 1) {
			System.err.println("Number of streams per group must be greater than 0");
			System.exit(1);
			return;
		}

		try {
			final JobGraph graph = new JobGraph("Streaming Job with File Input");

			final int numberOfSourceTasks = Math.max(1, degreeOfParallelism / 8);

			final JobInputVertex fileStreamSource = new JobInputVertex("MultiFileStreamSource", graph);
			fileStreamSource.setInputClass(MultiFileStreamSourceTask.class);
			fileStreamSource.setNumberOfSubtasks(numberOfSourceTasks);
			final int streamsPerSubtask = Math.max(1, numberOfStreams / numberOfSourceTasks);
			fileStreamSource.getConfiguration().setInteger(MultiFileStreamSourceTask.NO_OF_STREAMS_PER_SUBTASK,
				streamsPerSubtask);
			fileStreamSource.getConfiguration().setInteger(MultiFileStreamSourceTask.NO_OF_STREAMS_PER_GROUP,
				numberOfStreamsPerGroup);
			fileStreamSource.getConfiguration().setString(MultiFileStreamSourceTask.VIDEO_FILE_DIRECTORY, args[0]);
			fileStreamSource.setInstanceType(INSTANCE_TYPE);
			fileStreamSource.setNumberOfSubtasksPerInstance(1);

			final JobTaskVertex decoder = new JobTaskVertex("Decoder", graph);
			decoder.setNumberOfSubtasks(degreeOfParallelism);
			decoder.setTaskClass(DecoderTask.class);
			decoder.setInstanceType(INSTANCE_TYPE);
			decoder.setNumberOfSubtasksPerInstance(NUMBER_OF_SUBTASKS_PER_INSTANCE);

			final JobTaskVertex merger = new JobTaskVertex("Merger", graph);
			merger.setNumberOfSubtasks(degreeOfParallelism);
			merger.setTaskClass(MergeTask.class);
			merger.setInstanceType(INSTANCE_TYPE);
			merger.setNumberOfSubtasksPerInstance(NUMBER_OF_SUBTASKS_PER_INSTANCE);

			final JobTaskVertex overlay = new JobTaskVertex("Overlay", graph);
			overlay.setTaskClass(OverlayTask.class);
			overlay.setNumberOfSubtasks(degreeOfParallelism);
			overlay.getConfiguration().setString(TwitterOverlayProvider.TWITTER_USERNAME_KEY, "danielwarneke");
			overlay.getConfiguration().setString(TwitterOverlayProvider.TWITTER_PASSWORD_KEY, "T7$dDp1");
			overlay.getConfiguration().setString(TwitterOverlayProvider.TWITTER_KEYWORD_KEY, "berlin");
			overlay.setInstanceType(INSTANCE_TYPE);
			overlay.setNumberOfSubtasksPerInstance(NUMBER_OF_SUBTASKS_PER_INSTANCE);

			final JobTaskVertex encoder = new JobTaskVertex("Encoder", graph);
			encoder.setNumberOfSubtasks(degreeOfParallelism);
			encoder.setTaskClass(EncoderTask.class);
			encoder.getConfiguration().setString(VideoEncoder.ENCODER_OUTPUT_FORMAT, "flv");
			encoder.setInstanceType(INSTANCE_TYPE);
			encoder.setNumberOfSubtasksPerInstance(NUMBER_OF_SUBTASKS_PER_INSTANCE);

			final JobFileOutputVertex output = new JobFileOutputVertex("Receiver", graph);
			output.setFileOutputClass(VideoReceiverTask.class);
			output.setFilePath(new Path("file:///home/warneke/out.flv"));
			output.setInstanceType(INSTANCE_TYPE);
			output.setNumberOfSubtasksPerInstance(NUMBER_OF_SUBTASKS_PER_INSTANCE);

			fileStreamSource.connectTo(decoder, ChannelType.NETWORK, CompressionLevel.NO_COMPRESSION);
			decoder.connectTo(merger, ChannelType.NETWORK, CompressionLevel.NO_COMPRESSION);
			merger.connectTo(overlay, ChannelType.NETWORK, CompressionLevel.NO_COMPRESSION);
			overlay.connectTo(encoder, ChannelType.NETWORK, CompressionLevel.NO_COMPRESSION);
			encoder.connectTo(output, ChannelType.NETWORK, CompressionLevel.NO_COMPRESSION);

			// Configure instance sharing
			decoder.setVertexToShareInstancesWith(fileStreamSource);
			merger.setVertexToShareInstancesWith(fileStreamSource);
			overlay.setVertexToShareInstancesWith(fileStreamSource);
			encoder.setVertexToShareInstancesWith(fileStreamSource);
			output.setVertexToShareInstancesWith(fileStreamSource);
			
			//decoder.setVertexToShareInstancesWith(fileStreamSource);
			/*merger.setVertexToShareInstancesWith(decoder);
			overlay.setVertexToShareInstancesWith(decoder);
			encoder.setVertexToShareInstancesWith(decoder);
			output.setVertexToShareInstancesWith(decoder);*/

			// Create jar file for job deployment
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
