package my.pack;


import my.pack.utils.MyPoint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.LinearLayout;

public class SurfaceNavigator extends Activity {
	// Debugging
	public static final String TAG = "SurfaceNavigator";
	private static final boolean D = true;
	
	public static final String KEY_MY_MARKS = "my.pack.surfacenavigator.mymarks";
	public static final String KEY_SCALE_FACTOR = "my.pack.surfacenavigator.scalefactor";
	
	public static int SCREEN_WIDTH;
	public static int SCREEN_HEIGHT;
	public static int NAVIGATION_BAR_HEIGHT = 48;
	// Size of my surface, calculated depending on the screen size
	static int w;
	static int h;
	int extendedW;
	int extendedH;
	
	/* Cell size is related to screen size, being calculated as
	 * fraction of it. 
	 */
	private static final int CELL_FRACTION = 10;
<<<<<<< HEAD
	public static final int CELL_COUNT = 5; // 30x30 cells, the rest of 2 will be used for margins drawing
=======
	public static final int CELL_COUNT = 32; // 30x30 cells, the rest of 2 will be used for margins drawing
>>>>>>> Pinch-to-Zoom added
	
	private MySurface mySurface;
	
	private int currentApiVersion;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        // Let's fetch here width and height of the screen
      	Display display = getWindowManager().getDefaultDisplay();
      	SCREEN_WIDTH = display.getWidth();
      	SCREEN_HEIGHT = display.getHeight();
      	currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
        	SCREEN_HEIGHT -= NAVIGATION_BAR_HEIGHT;
        } else {
	        DisplayMetrics displayMetrics = new DisplayMetrics();
	        display.getMetrics(displayMetrics);
	        int statusBarHeight;
	
	        switch (displayMetrics.densityDpi) {
	            case DisplayMetrics.DENSITY_HIGH:
	                statusBarHeight = 38;
	                break;
	            case DisplayMetrics.DENSITY_MEDIUM:
	                statusBarHeight = 25;
	                break;
	            case DisplayMetrics.DENSITY_LOW:
	                statusBarHeight = 19;
	                break;
	            default:
	                statusBarHeight = 25;
	        }
	        if (D) Log.d(TAG, "statusBarHeight = " + statusBarHeight + " getWindow().getDecorView().isShown() " + getWindow().getDecorView().isShown() );
	        SCREEN_HEIGHT -= statusBarHeight;
        }
      	if (D) Log.d(TAG, "SCREEN_WIDTH = " + SCREEN_WIDTH + ", SCREEN_HEIGHT = " + SCREEN_HEIGHT);

      	calculateMySurfaceSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        //workaround
//      	if (w < SurfaceNavigator.SCREEN_WIDTH) {
//      		extendedW = SurfaceNavigator.SCREEN_WIDTH;
//      		extendedH = extendedW;
//      	}
      	Log.i(TAG, "My surface size : width = " + w + ", height = " + h);
      	// Create my surface
		if (savedInstanceState != null) {
			MyPoint[] myMarks = (MyPoint[]) savedInstanceState.getParcelableArray(KEY_MY_MARKS);
			float scaleFactor = savedInstanceState.getFloat(KEY_SCALE_FACTOR, 1f);
			mySurface = new MySurface(this, w, h, myMarks, scaleFactor);
			if (D) Log.d(TAG, "Marks and scale factor retrieved from savedInstanceState");
		} else {
			mySurface = new MySurface(this, w, h);
		}
		

//		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
//		int dim = 0;
//		if (!mySurface.isWholeSurfaceDisplayed()) {
//			if (mySurface.isWholeXSurfaceDisplayed() || mySurface.isWholeYSurfaceDisplayed()) {
//				if (mySurface.isWholeXSurfaceDisplayed()) {
//					dim = SCREEN_WIDTH;
//				} else {
//					if (mySurface.isWholeYSurfaceDisplayed()) {
//						dim = SCREEN_HEIGHT;
//					}
//				}
//				p = new LinearLayout.LayoutParams(dim, dim);
//			}
//		}
		
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
										Math.max(w, SCREEN_WIDTH), // Math.max(SCREEN_WIDTH, SCREEN_WIDTH)), 
										Math.max(h, SCREEN_HEIGHT)); // Math.max(SCREEN_WIDTH, SCREEN_WIDTH)));
		mySurface.setLayoutParams(p);
		mySurface.requestFocus();
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        layout.addView(mySurface);
		
    }
    
    
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// convert ArrayList to array
		MyPoint[] myMarks = new MyPoint[mySurface.getMyMarks().size()];
		mySurface.getMyMarks().toArray(myMarks);
		
		outState.putParcelableArray(KEY_MY_MARKS, myMarks);
		
		outState.putFloat(KEY_SCALE_FACTOR, mySurface.getScaleFactor());
	}	

	private void calculateMySurfaceSize(int screenWidth, int screenHeight) {
		int cellWidth = Math.min(screenWidth, screenHeight) / CELL_FRACTION;
		w = cellWidth * CELL_COUNT;
		h = w; // my surface will be square-built
	}
	
}