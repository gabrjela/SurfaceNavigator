package my.pack;


import my.pack.utils.MyPoint;
import android.app.Activity;
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
	
	public static int SCREEN_WIDTH;
	public static int SCREEN_HEIGHT;
	static int NAVIGATION_BAR_HEIGHT = 48;
	// Size of my surface, calculated depending on the screen size
	int w;
	int h;
	
	/* Cell size is related to screen size, being calculated as
	 * fraction of it. 
	 */
	private static final int CELL_FRACTION = 9;
	public static final int CELL_COUNT = 32; // 30x30 cells, the rest of 2 will be used for margins drawing
	
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
	        if (D) Log.d(TAG, "statusBarHeight = " + statusBarHeight);
	        SCREEN_HEIGHT -= statusBarHeight;
        }
        
        if (D) Log.d(TAG, "SCREEN_WIDTH = " + SCREEN_WIDTH + ", SCREEN_HEIGHT = " + SCREEN_HEIGHT);
        
      	calculateMySurfaceSize(SCREEN_WIDTH, SCREEN_HEIGHT);
      	Log.i(TAG, "My surface size : width = " + w + ", height = " + h);
      	// Create my surface
		if (savedInstanceState != null) {
			MyPoint[] myMarks = (MyPoint[]) savedInstanceState.getParcelableArray(KEY_MY_MARKS);
			mySurface = new MySurface(this, w, h, myMarks);
			if (D) Log.d(TAG, "Marks retrieved from savedInstanceState");
		} else {
			mySurface = new MySurface(this, w, h);
		}

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
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
	}	

	private void calculateMySurfaceSize(int screenWidth, int screenHeight) {
		int cellWidth = Math.min(screenWidth, screenHeight) / CELL_FRACTION;
		w = cellWidth * CELL_COUNT;
		h = w; // my surface will be square-built
	}

}