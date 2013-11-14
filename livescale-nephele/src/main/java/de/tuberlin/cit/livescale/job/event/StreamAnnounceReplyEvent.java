package de.tuberlin.cit.livescale.job.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class StreamAnnounceReplyEvent extends StreamingEvent {

	public StreamAnnounceReplyEvent(long streamId, long groupId) {
		super(streamId, groupId);
	}

	private String sendEndpointToken;

	private String receiveEndpointUrl;

	public String getReceiveEndpointUrl() {
		return receiveEndpointUrl;
	}

	public void setReceiveEndpointUrl(String receiveEndpointUrl) {
		this.receiveEndpointUrl = receiveEndpointUrl;
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
		out.writeUTF(receiveEndpointUrl);
	}

	@Override
	public void read(DataInput in) throws IOException {
		super.read(in);
		this.sendEndpointToken = in.readUTF();
		this.receiveEndpointUrl = in.readUTF();
	}
}
