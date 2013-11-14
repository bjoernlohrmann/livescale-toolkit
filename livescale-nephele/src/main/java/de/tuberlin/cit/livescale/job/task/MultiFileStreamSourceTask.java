package de.tuberlin.cit.livescale.job.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupedPacketChannelSelector;
import de.tuberlin.cit.livescale.job.util.source.PrioritizedLivestream;
import de.tuberlin.cit.livescale.job.util.source.VideoFile;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractGenericInputTask;

public class MultiFileStreamSourceTask extends AbstractGenericInputTask {

	private static final Log LOG = LogFactory.getLog(MultiFileStreamSourceTask.class);

	public static final String NO_OF_STREAMS_PER_SUBTASK = "NO_OF_STREAMS_PER_TASK";

	public static final String NO_OF_STREAMS_PER_GROUP = "NO_OF_STREAMS_PER_GROUP";

	public static final String VIDEO_FILE_DIRECTORY = "VIDEO_FILE_DIRECTORY";

	private RecordWriter<Packet> out = null;

	private int noOfStreamsPerTask;

	private int noOfStreamsPerGroup;

	private PriorityQueue<PrioritizedLivestream> pendingStreams = new PriorityQueue<PrioritizedLivestream>();

	private GroupedPacketChannelSelector channelSelector = new GroupedPacketChannelSelector();

	private ArrayList<VideoFile> videoFiles = new ArrayList<VideoFile>();

	@Override
	public void registerInputOutput() {
		out = new RecordWriter<Packet>(this, Packet.class, channelSelector);
	}

	@Override
	public void invoke() throws Exception {
		this.noOfStreamsPerTask = getTaskConfiguration().getInteger(NO_OF_STREAMS_PER_SUBTASK, 2);
		this.noOfStreamsPerGroup = getTaskConfiguration().getInteger(NO_OF_STREAMS_PER_GROUP, 2);

		try {
			loadVideoFiles();
			initLivestreams();

			long timeOfNextWrite = 0;

			while (true) {
				long now = System.currentTimeMillis();

				timeOfNextWrite = pendingStreams.peek().getTimeOfWriteForCurrentPacket();
				if (timeOfNextWrite > now) {
					Thread.sleep(timeOfNextWrite - now);
					now = System.currentTimeMillis();
				}

				while (pendingStreams.peek().getTimeOfWriteForCurrentPacket() <= now) {
					
					if (now - pendingStreams.peek().getTimeOfWriteForCurrentPacket() > 1000) {
						System.out.println("stream source is behind by " + (now - pendingStreams.peek().getTimeOfWriteForCurrentPacket()));
					}
					
					PrioritizedLivestream livestream = pendingStreams.remove();

					Packet packet = createNextPacket(livestream);
					out.emit(packet);

					livestream.shiftToNextPacket();

					if (livestream.isEOF()) {
						Packet endOfStreamPacket = createEndOfStreamPacket(livestream);
						out.emit(endOfStreamPacket);
						LOG.info("Sent end of stream packet for stream " + livestream.getStreamId());

						// restart stream immediately
						livestream.rewind(now + 100);
					}
					pendingStreams.add(livestream);
				}
			}
		} catch (InterruptedException e) {
		}
	}

	private Packet createEndOfStreamPacket(PrioritizedLivestream livestream) {
		Packet endOfStreamPacket = new Packet(livestream.getStreamId(), livestream.getGroupId(), 0, null);
		endOfStreamPacket.markAsEndOfStreamPacket();
		return endOfStreamPacket;
	}

	private Packet createNextPacket(PrioritizedLivestream livestream) throws IOException, InterruptedException {
		byte[] packetData = new byte[livestream.getPacketSizeForCurrentPacket()];
		livestream.fillArrayWithCurrentPacketPayload(packetData, 0);

		return new Packet(livestream.getStreamId(), livestream.getGroupId(),
			livestream.getPacketIdInStreamForCurrentPacket(), packetData);
	}

	private void initLivestreams() {
		int nextVideoFileIndex = (int) (Math.random() * videoFiles.size());
		int nextStreamId = getIndexInSubtaskGroup() * this.noOfStreamsPerTask;

		int noOfGroups = (getCurrentNumberOfSubtasks() * noOfStreamsPerTask) / noOfStreamsPerGroup;

		for (int i = 0; i < this.noOfStreamsPerTask; i++) {
			int groupId = nextStreamId % noOfGroups;
			long startTime = System.currentTimeMillis() + ((long) (Math.random() * 10000));
			PrioritizedLivestream livestream = new PrioritizedLivestream(videoFiles.get(nextVideoFileIndex),
				true, nextStreamId, groupId, startTime);
			
			nextStreamId++;
			nextVideoFileIndex = (int) (Math.random() * videoFiles.size());
			pendingStreams.add(livestream);
		}
	}

	private void loadVideoFiles() throws IOException {
		File videoDir = new File(getTaskConfiguration().getString(VIDEO_FILE_DIRECTORY, "videos"));

		if (!videoDir.exists() || !videoDir.canRead()) {
			throw new IOException("Cannot read video directory " + videoDir.getAbsolutePath());
		}

		for (String file : videoDir.list()) {
			if (file.endsWith(".packetized")) {
				VideoFile videoFile = new VideoFile(videoDir.getAbsolutePath() + File.separator + file);
				videoFile.loadIntoMemory();
				videoFiles.add(videoFile);
			}
		}

		if (videoFiles.isEmpty()) {
			throw new IOException("Found not .packetized files in " + videoDir);
		}
	}
}
