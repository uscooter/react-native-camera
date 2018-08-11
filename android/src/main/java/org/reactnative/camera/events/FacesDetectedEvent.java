package org.reactnative.camera.events;

import android.support.v4.util.Pools;
import android.util.SparseArray;

import org.reactnative.camera.CameraViewManager;
import org.reactnative.camera.utils.ImageDimensions;
import org.reactnative.facedetector.FaceDetectorUtils;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.cameraview.CameraView;
import com.google.android.gms.vision.face.Face;

public class FacesDetectedEvent extends Event<FacesDetectedEvent> {
  private static final Pools.SynchronizedPool<FacesDetectedEvent> EVENTS_POOL =
      new Pools.SynchronizedPool<>(3);

  private double mScale;
  private int mShiftX;
  private int mShiftY;
  private SparseArray<Face> mFaces;
  private ImageDimensions mImageDimensions;

  private FacesDetectedEvent() {}

  public static FacesDetectedEvent obtain(
      int viewTag,
      SparseArray<Face> faces,
      ImageDimensions dimensions,
      double scale,
      int shiftX,
      int shiftY
  ) {
    FacesDetectedEvent event = EVENTS_POOL.acquire();
    if (event == null) {
      event = new FacesDetectedEvent();
    }
    event.init(viewTag, faces, dimensions, scale, shiftX, shiftY);
    return event;
  }

  private void init(
      int viewTag,
      SparseArray<Face> faces,
      ImageDimensions dimensions,
      double scale,
      int shiftX,
      int shiftY
  ) {
    super.init(viewTag);
    mFaces = faces;
    mImageDimensions = dimensions;
    mScale = scale;
    mShiftX = shiftX;
    mShiftY = shiftY;
  }

  /**
   * note(@sjchmiela)
   * Should the events about detected faces coalesce, the best strategy will be
   * to ensure that events with different faces count are always being transmitted.
   */
  @Override
  public short getCoalescingKey() {
    if (mFaces.size() > Short.MAX_VALUE) {
      return Short.MAX_VALUE;
    }

    return (short) mFaces.size();
  }

  @Override
  public String getEventName() {
    return CameraViewManager.Events.EVENT_ON_FACES_DETECTED.toString();
  }

  @Override
  public void dispatch(RCTEventEmitter rctEventEmitter) {
    rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
  }

  private WritableMap serializeEventData() {
    WritableArray facesList = Arguments.createArray();

    for(int i = 0; i < mFaces.size(); i++) {
      Face face = mFaces.valueAt(i);
      WritableMap serializedFace = FaceDetectorUtils.serializeFace(face, mScale, mShiftX, mShiftY);
      if (mImageDimensions.getFacing() == CameraView.FACING_FRONT) {
        serializedFace = FaceDetectorUtils.rotateFaceX(serializedFace, mImageDimensions.getWidth(), mScale);
      } else {
        serializedFace = FaceDetectorUtils.changeAnglesDirection(serializedFace);
      }
      facesList.pushMap(serializedFace);
    }

    WritableMap event = Arguments.createMap();
    event.putString("type", "face");
    event.putArray("faces", facesList);
    event.putInt("target", getViewTag());
    return event;
  }
}
