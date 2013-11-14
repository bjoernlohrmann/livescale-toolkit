package de.tuberlin.cit.livescale.job.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import eu.stratosphere.nephele.event.task.AbstractTaskEvent;

public class StreamingEvent extends AbstractTaskEvent {

	private long streamId;

	private long groupId;

	public StreamingEvent(long streamId, long groupId) {
		this.streamId = streamId;
		this.groupId = groupId;
	}
	
	public long getStreamId() {
		return streamId;
	}

	public void setStreamId(long streamId) {
		this.streamId = streamId;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(streamId);
		out.writeLong(groupId);
	}

	@Override
	public void read(DataInput in) throws IOException {
		this.streamId = in.readLong();
		this.groupId = in.readLong();
	}

}
