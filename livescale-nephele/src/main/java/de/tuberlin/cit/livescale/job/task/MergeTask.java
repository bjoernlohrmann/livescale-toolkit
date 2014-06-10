package de.tuberlin.cit.livescale.job.task;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.merge.MergeGroup;
import eu.stratosphere.nephele.template.Collector;
import eu.stratosphere.nephele.template.IoCTask;
import eu.stratosphere.nephele.template.LastRecordReadFromWriteTo;
import eu.stratosphere.nephele.template.ReadFromWriteTo;

import java.util.HashMap;
import java.util.Map;

public final class MergeTask extends IoCTask {

  // Merge Mapper member
  private final Map<Long, MergeGroup> groupMap = new HashMap<Long, MergeGroup>();


  @Override
  protected void setup() {
    initReader(0, VideoFrame.class);
    initWriter(0, VideoFrame.class);
  }

  @ReadFromWriteTo(readerIndex = 0, writerIndex = 0)
  public void merge(VideoFrame frame, Collector<VideoFrame> out) {

    final Long groupId = frame.groupId;
    MergeGroup mergeGroup = groupMap.get(groupId);
    if (mergeGroup == null) {
      mergeGroup = new MergeGroup();
      groupMap.put(groupId, mergeGroup);
    }

    mergeGroup.addFrame(frame);

    VideoFrame mergedFrame = mergeGroup.mergedFrameAvailable();
    while (mergedFrame != null) {
      out.emit(mergedFrame);
      if (mergedFrame.isEndOfStreamFrame()) {
        out.flush();
      }
      mergedFrame = mergeGroup.mergedFrameAvailable();
    }
  }


  @LastRecordReadFromWriteTo(readerIndex = 0, writerIndex = 0)
  public void last(Collector<VideoFrame> out) {
    groupMap.clear();
  }
}
