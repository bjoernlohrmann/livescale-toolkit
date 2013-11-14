package de.tuberlin.cit.livescale.job.task;

import java.nio.ByteBuffer;
import java.util.Iterator;

import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.task.channelselectors.PacketChannelSelector;
import eu.stratosphere.nephele.fs.FSDataInputStream;
import eu.stratosphere.nephele.fs.FileInputSplit;
import eu.stratosphere.nephele.fs.FileSystem;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractFileInputTask;

public class SingleFileStreamSourceTask extends AbstractFileInputTask {

	private RecordWriter<Packet> out = null;

	private PacketChannelSelector channelSelector = new PacketChannelSelector();

	@Override
	public void registerInputOutput() {

		out = new RecordWriter<Packet>(this, Packet.class, channelSelector);
	}

	@Override
	public void invoke() throws Exception {

		final long startOfStream = System.currentTimeMillis();

		final Iterator<FileInputSplit> it = getFileInputSplits();
		while (it.hasNext()) {

			final FileInputSplit fis = it.next();

			final FileSystem fs = fis.getPath().getFileSystem();

			final byte[] len = new byte[4];

			final byte[] data = new byte[256 * 1024];

			int packetID = 0;

			final Packet packet = new Packet();

			final FSDataInputStream inputStream = fs.open(fis.getPath());

			final int streamID = inputStream.hashCode();

			while (true) {

				int read = 0;
				while (read < len.length) {
					final int r = inputStream.read(len);
					if (r == -1) {
						break;
					} else {
						read += r;
					}
				}

				if (read < len.length) {
					break;
				}

				final int lengthOfPacket = ByteBuffer.wrap(len).getInt();
				read = 0;
				while (read < lengthOfPacket) {
					final int r = inputStream.read(data, 0, lengthOfPacket);
					if (r == -1) {
						break;
					} else {
						read += r;
					}
				}

				if (read < lengthOfPacket) {
					break;
				}

				// FIXME: groupId broken
				packet.set(streamID, 0, packetID, data, 0, lengthOfPacket);

				if (packetID > 0) {

					final long now = System.currentTimeMillis();
					final long timeToSendPacket = startOfStream + extractTimestamp(data);

					if (timeToSendPacket > now) {
						Thread.sleep(timeToSendPacket - now);
					}
				}

				this.out.emit(packet);

				// Increase packet ID
				++packetID;
			}

			// FIXME: groupId broken
			Packet endOfStreamPacket = new Packet(streamID, 0, 0, null);
			endOfStreamPacket.markAsEndOfStreamPacket();
			this.out.emit(packet);
			channelSelector.unregisterStreamId(streamID);
		}
	}

	private long extractTimestamp(final byte[] packet) {
		// frame_tag(1) + frame_data_length(3)
		int offset = 1 + 3;
		byte[] reordered = new byte[8];
		reordered[4] = packet[offset + 3];
		reordered[5] = packet[offset + 0];
		reordered[6] = packet[offset + 1];
		reordered[7] = packet[offset + 2];

		long streamTimestamp = ByteBuffer.wrap(reordered).getLong();
		return streamTimestamp;
	}
}
