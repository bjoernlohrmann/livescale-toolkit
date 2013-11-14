package de.tuberlin.cit.livescale;

import java.io.File;
import java.io.IOException;

import de.tuberlin.cit.livescale.job.mapper.DecoderMapper;
import de.tuberlin.cit.livescale.job.mapper.EncoderMapper;
import de.tuberlin.cit.livescale.job.mapper.MergeMapper;
import de.tuberlin.cit.livescale.job.mapper.OverlayMapper;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.DecoderTask;
import de.tuberlin.cit.livescale.job.task.EncoderTask;
import de.tuberlin.cit.livescale.job.task.MergeTask;
import de.tuberlin.cit.livescale.job.task.MultiFileStreamSourceTask;
import de.tuberlin.cit.livescale.job.task.OverlayTask;
import de.tuberlin.cit.livescale.job.task.PacketizedStreamHandler;
import de.tuberlin.cit.livescale.job.task.SingleFileStreamSourceTask;
import de.tuberlin.cit.livescale.job.task.VideoReceiverTask;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupedPacketChannelSelector;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupedVideoFrameChannelSelector;
import de.tuberlin.cit.livescale.job.task.channelselectors.PacketChannelSelector;
import de.tuberlin.cit.livescale.job.task.channelselectors.VideoFrameChannelSelector;
import de.tuberlin.cit.livescale.job.util.decoder.ByteBufferedURLProtocolHandler;
import de.tuberlin.cit.livescale.job.util.decoder.IOBuffer;
import de.tuberlin.cit.livescale.job.util.decoder.VideoDecoder;
import de.tuberlin.cit.livescale.job.util.encoder.PortBinder;
import de.tuberlin.cit.livescale.job.util.encoder.ServerPortManager;
import de.tuberlin.cit.livescale.job.util.encoder.TCPReceiver;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import de.tuberlin.cit.livescale.job.util.merge.MergeGroup;
import de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TimeOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TimeVideoOverlay;
import de.tuberlin.cit.livescale.job.util.overlay.TwitterOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TwitterVideoOverlay;
import de.tuberlin.cit.livescale.job.util.overlay.VideoOverlay;
import de.tuberlin.cit.livescale.job.util.receiver.FileVideoReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.FlvOverTcpForwardingReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.UdpForwardingReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.VideoReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.BufferedFlvFile;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvFileAtom;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvFileHeader;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvStreamForwarderState;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvStreamForwarderThread;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvTag;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvTagState;
import de.tuberlin.cit.livescale.job.util.receiver.flv2rtsp.FlvTagType;
import de.tuberlin.cit.livescale.job.util.receiver.udpmulticast.UdpClient;
import de.tuberlin.cit.livescale.job.util.source.Livestream;
import de.tuberlin.cit.livescale.job.util.source.PacketFactory;
import de.tuberlin.cit.livescale.job.util.source.PacketState;
import de.tuberlin.cit.livescale.job.util.source.PrioritizedLivestream;
import de.tuberlin.cit.livescale.job.util.source.Stream;
import de.tuberlin.cit.livescale.job.util.source.VideoFile;
import eu.stratosphere.nephele.util.JarFileCreator;

@Deprecated
public class JarFileUtil {

	public static File createJarFile() throws IOException {
		File jarFile = File.createTempFile("stream", ".jar");

		JarFileCreator jfc = new JarFileCreator(jarFile);

		jfc.addClass(BufferedFlvFile.class);
		jfc.addClass(ByteBufferedURLProtocolHandler.class);
		jfc.addClass(DecoderTask.class);
		jfc.addClass(DecoderMapper.class);
		jfc.addClass(EncoderMapper.class);
		jfc.addClass(EncoderTask.class);
		jfc.addClass(FileVideoReceiver.class);
		jfc.addClass(FlvFileAtom.class);
		jfc.addClass(FlvFileHeader.class);
		jfc.addClass(FlvStreamForwarderThread.class);
		jfc.addClass(FlvStreamForwarderState.class);
		jfc.addClass(FlvTag.class);
		jfc.addClass(FlvOverTcpForwardingReceiver.class);
		jfc.addClass(GroupedPacketChannelSelector.class);
		jfc.addClass(GroupedVideoFrameChannelSelector.class);
		jfc.addClass(IOBuffer.class);
		jfc.addClass(Livestream.class);
		jfc.addClass(MergeGroup.class);
		jfc.addClass(MergeMapper.class);
		jfc.addClass(MergeTask.class);
		jfc.addClass(MultiFileStreamSourceTask.class);
		jfc.addClass(OverlayMapper.class);
		jfc.addClass(OverlayProvider.class);
		jfc.addClass(OverlayTask.class);
		jfc.addClass(Packet.class);
		jfc.addClass(PacketChannelSelector.class);
		jfc.addClass(PacketizedStreamHandler.class);
		jfc.addClass(PacketFactory.class);
		jfc.addClass(PacketState.class);
		jfc.addClass(PortBinder.class);
		jfc.addClass(PrioritizedLivestream.class);
		jfc.addClass(ServerPortManager.class);
		jfc.addClass(SingleFileStreamSourceTask.class);
		jfc.addClass(Stream.class);
		jfc.addClass(FlvTagState.class);
		jfc.addClass(FlvTagType.class);
		jfc.addClass(TCPReceiver.class);
		jfc.addClass(TimeOverlayProvider.class);
		jfc.addClass(TimeVideoOverlay.class);
		jfc.addClass(TwitterOverlayProvider.class);
		jfc.addClass(TwitterVideoOverlay.class);
		jfc.addClass(UdpClient.class);
		jfc.addClass(UdpForwardingReceiver.class);
		jfc.addClass(VideoReceiverTask.class);
		jfc.addClass(VideoEncoder.class);
		jfc.addClass(VideoFile.class);
		jfc.addClass(VideoFrame.class);
		jfc.addClass(VideoDecoder.class);
		jfc.addClass(VideoFrameChannelSelector.class);
		jfc.addClass(VideoOverlay.class);
		jfc.addClass(VideoReceiver.class);
		jfc.createJarFile();

		return jarFile;
	}

}
