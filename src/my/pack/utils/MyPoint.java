package my.pack.utils;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Wrapper over android.graphics.Point which implements Parcelable.
 * Used in SurfaceNavigator to be written to and restored from the
 * savedInstanceState Bundle.
 */
public class MyPoint extends Point implements Parcelable {

	public MyPoint(int x, int y) {
		super(x, y);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	// Marshaler
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(x);
		dest.writeInt(y);
		
	}
	
	// Unmarshaler
	public static final Parcelable.Creator<MyPoint> CREATOR = new Parcelable.Creator<MyPoint>() {
		@Override
		public MyPoint createFromParcel(Parcel source) {
			return new MyPoint(
					source.readInt(),
					source.readInt());
		}

		@Override
		public MyPoint[] newArray(int size) {
			return new MyPoint[size];
		}
		
	};

	@Override
	public String toString() {
		return x + "," + y;
	}

}
