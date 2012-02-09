package my.pack.utils;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Wrapper over android.graphics.Point which implements Parcelable.
 * Used in SurfaceNavigator to be written to and restored from the
 * savedInstanceState Bundle.
 */
public class MySurfacePoint extends PointF implements Parcelable {

	public MySurfacePoint(float x, float y) {
		super(x, y);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	// Marshaler
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloat(x);
		dest.writeFloat(y);
		
	}
	
	// Unmarshaler
	public static final Parcelable.Creator<MySurfacePoint> CREATOR = new Parcelable.Creator<MySurfacePoint>() {
		@Override
		public MySurfacePoint createFromParcel(Parcel source) {
			return new MySurfacePoint(
					source.readFloat(),
					source.readFloat());
		}

		@Override
		public MySurfacePoint[] newArray(int size) {
			return new MySurfacePoint[size];
		}
		
	};

	@Override
	public String toString() {
		return x + "," + y;
	}

}
