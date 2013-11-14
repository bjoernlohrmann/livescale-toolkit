package de.tuberlin.cit.livescale.job.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class StreamAnnounceEvent extends StreamingEvent {

	private String sendEndpointToken;

	private String receiveEndpointToken;

	public StreamAnnounceEvent(long streamId, long groupId) {
		super(streamId, groupId);
	}

	public String getReceiveEndpointToken() {
		return receiveEndpointToken;
	}

	public void setReceiveEndpointToken(String receiveEndpointToken) {
		this.receiveEndpointToken = receiveEndpointToken;
	}

	public String getSendEndpointToken() {
		return sendEndpointToken;
	}

	public void setSendEndpointToken(String sendEndpointToken) {
		this.sendEndpointToken = sendEndpointToken;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		out.writeUTF(sendEndpointToken);
		out.writeUTF(receiveEndpointToken);
	}

	@Override
	public void read(DataInput in) throws IOException {
		super.read(in);
		this.sendEndpointToken = in.readUTF();
		this.receiveEndpointToken = in.readUTF();
	}
}
