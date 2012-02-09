package my.pack;


import java.util.ArrayList;
import java.util.Arrays;

import my.pack.utils.MyPoint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Region;
import android.graphics.RegionIterator;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

public class MySurface extends View {
	// Debugging
	public static final String TAG = "MySurface";
	private static final boolean D = true;
	
	// Drawing objects
	private Paint background = null;
	private Paint dark = null;
	private Paint hilite = null;
	private Paint light = null;
	private Paint foreground = null;
	private FontMetrics fm = null;
	private Paint selected = null;
	
	private int _mode = NONE;
	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2; // not used yet
	
	// Size of this object, retrieved from SurfaceNavigator
	private int w;
	private int h;
	private int cellWidth = 0;
	private int cellHeight = 0;
	// Define area to be drawn when user clicks on a cell
	private final Rect selRect = new Rect();
	private int borderSize;
	
	// Remember some things for dragging/zooming
	// start of drag operation
	private PointF start = new PointF();
	// end of drag operation or click
	private PointF end = new PointF();
	// coords of distance between previous drag point and current drag point
	private float dx = 0;  
	private float dy = 0;
	// coords of previous drag point 
	private float oldX = 0;
	private float oldY = 0;
	// coords of total distance between start point and current drag point
	PointF mid = new PointF();
	private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;
    private float scaleFactor = 1f;
    private ScaleGestureDetector zoomDetector;
    
	// Arrays of marked points on screen (drawn as X symbol)
	private ArrayList<MyPoint> myMarks = null;
    
	// Flag indicating whether a simple click on a cell or a drag operation
    private boolean onDrag = false;
    
    // Coords from where to start drawing
    int startX = 0;
    int startY = 0;
    int indexStartX;
    int indexStartY;
    
    /* Remaining space along the edges of the displayed canvas.
     * Used to stop the dragging when an edge of the canvas reached.
     */
    private static float SPACE_UP;
    private static float SPACE_BOTTOM;
    private static float SPACE_LEFT;
    private static float SPACE_RIGHT;
    
    private Matrix matrix = new Matrix();
    private float initTransX;
    private float initTransY;
    // Translation values retrieved from the canvas matrix
    private float transX;
    private float transY;
    
    // Depending on this value we'll have a thiner or thicker line separator 
    public static float BIG_CELL_SIZE = 48;
    public static final int BORDER_FRACTION = 10;
    
    private boolean extended = false;
    
	public MySurface(Context context, int width, int height) { // w = 900, h = 900
		super(context);
		this.w = width;
		this.h = height;
		if (D) Log.d(TAG, "Creating MySurface " + w + "x" + h);
		
		this.setFocusable(true);
		setFocusableInTouchMode(true);
		
		cellWidth = w / SurfaceNavigator.CELL_COUNT;
		cellHeight = h / SurfaceNavigator.CELL_COUNT;
		if (D) Log.d(TAG, "cellWidth = " + cellWidth);
		borderSize = cellWidth / BORDER_FRACTION;
		
		if (isWholeXSurfaceDisplayed()) {
			SPACE_LEFT = 0;
			initTransX = 0;
			startX = (SurfaceNavigator.SCREEN_WIDTH - w) / 2;
			indexStartX = startX / SurfaceNavigator.CELL_COUNT;
		} else {
			SPACE_LEFT = (w - SurfaceNavigator.SCREEN_WIDTH) /2;
			initTransX = SPACE_LEFT;
		}
		SPACE_RIGHT = SPACE_LEFT;
		
		if (isWholeYSurfaceDisplayed()) {
			SPACE_UP = 0;
			initTransY = 0;
			startY = (SurfaceNavigator.SCREEN_HEIGHT - h) / 2;
			indexStartX = startY / SurfaceNavigator.CELL_COUNT;
		} else {
			SPACE_UP = (h - SurfaceNavigator.SCREEN_HEIGHT) /2;
			initTransY = SPACE_UP;
		}
		SPACE_BOTTOM = SPACE_UP;
		
		if (D) Log.d(TAG, "SPACE_LEFT = " + SPACE_LEFT + ", SPACE_RIGHT = " + SPACE_RIGHT);
		if (D) Log.d(TAG, "SPACE_UP = " + SPACE_UP + ", SPACE_BOTTOM = " + SPACE_BOTTOM);
		if (D) Log.d(TAG, "initTransX = " + initTransX + ", initTransY = " + initTransY);
			
		
		myMarks = new ArrayList<MyPoint>();
		
		// draw the bkg
		background = new Paint();
		background.setColor(getResources().getColor(R.color.grid_background)); //grid_background
		
		// Define colors and styles for markers 
		// We do it here as we have now cellHeight and cellWidth values
		foreground = new Paint(Paint.ANTI_ALIAS_FLAG);
		foreground.setColor(getResources().getColor(R.color.grid_foreground));
		foreground.setStyle(Style.FILL);
		foreground.setTextSize(cellHeight * 0.75f);
		foreground.setTextScaleX(cellWidth / cellHeight);
		foreground.setTextAlign(Paint.Align.CENTER);
		fm = foreground.getFontMetrics();
		
		// Define colors for the grid lines
		dark = new Paint();
		dark.setColor(getResources().getColor(R.color.grid_dark));
		dark.setStrokeWidth(8);//dark.setStyle(Style.FILL_AND_STROKE);
		
		hilite = new Paint();
		hilite.setColor(getResources().getColor(R.color.grid_hilite));
		if (cellWidth > BIG_CELL_SIZE) hilite.setStrokeWidth(2);
		
		light = new Paint();
		light.setColor(getResources().getColor(R.color.grid_light));
		
		// Define color for the selected cell
		selected = new Paint();
		selected.setColor(getResources().getColor(R.color.grid_selected));
		
		zoomDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
		
		if (isWholeSurfaceDisplayed()) {
			MIN_ZOOM = 0.5f;// this applies to surfaces smaller than screen size
			if (D) Log.d(TAG, "isWholeSurfaceDisplayed with MIN_ZOOM = " + MIN_ZOOM);
		} else {
			if (isWholeXSurfaceDisplayed()) {
				MIN_ZOOM =  (float) SurfaceNavigator.SCREEN_HEIGHT / h;
				if (D) Log.d(TAG, "isWholeXSurfaceDisplayed with MIN_ZOOM = " + MIN_ZOOM);
			} else {
				if (isWholeYSurfaceDisplayed()) {
					MIN_ZOOM =  (float)SurfaceNavigator.SCREEN_WIDTH / w;
					if (D) Log.d(TAG, "isWholeYSurfaceDisplayed with MIN_ZOOM = " + MIN_ZOOM);
				} else { // canvas larger than screen on both directions
					// Calculate MIN_ZOOM so that minimum scaled canvas will fit to screen size
					MIN_ZOOM = ((float)Math.max(SurfaceNavigator.SCREEN_WIDTH, SurfaceNavigator.SCREEN_HEIGHT)) / ((float)Math.max(w, h));
					if (D) Log.d(TAG, "big canvas with MIN_ZOOM = " + MIN_ZOOM);
				}
			}
		}
		// Set zoom pivot point in the middle of the screen
		mid.set(startX + w / 2, startY + h / 2);
		
	}
	
	
	
	/** Called when surface info retrieved from Activity bundle */
	public MySurface(Context context, int width, int height, MyPoint[] myMarks, float scaleFactor) {
		this(context, width, height);
		this.myMarks = new ArrayList<MyPoint>(Arrays.asList(myMarks));
		this.scaleFactor = scaleFactor;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (D) Log.d(TAG, "onTouchEvent " + event.getX() + ", " + event.getY());
		
		float[] values = new float[9];
		matrix.getValues(values);
		Log.d(TAG, "Values: " + values[0] + " " + values[1] + " " + values[2] + " " + values[3] + " " + values[4] + " " + values[5] +  " " +values[6] + " " + values[7] + " " + values[8]);
		
		transX = Math.abs(values[2]);
		transY = Math.abs(values[5]);
		Log.d(TAG, "transX = " + transX + ", transY = " + transY);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: //first finger down only
				Log.d(TAG, "ACTION_DOWN");
			   	start.set(event.getX(), event.getY());
			   	//reset onMove
			   	onDrag = false;
			   			
			   	oldX = start.x;
			   	oldY = start.y;
			   	
			   	_mode = DRAG;
			   	zoomDetector.onTouchEvent(event);
				break;
			case MotionEvent.ACTION_MOVE: //finger dragging
				Log.d(TAG, "ACTION_MOVE in mode " + _mode);
				if (_mode == DRAG) {
					//if dragging done on a distance bigger than cell size, then definitely a drag operation
					if (dist(start, event) >= cellHeight) onDrag = true;
					
//					if (!isWholeSurfaceDisplayed()) {
//						if (!isWholeXSurfaceDisplayed())
							dx = -event.getX() + oldX; //we can move along X axis
//						else dx = 0;
						
//						if (!isWholeYSurfaceDisplayed()) 
							dy = -event.getY() + oldY; //we can move along Y axis
//						else dy = 0;
						
						checkBoundaries();
								
//						this.scrollBy((int)dx, (int)dy);
								
						oldX = event.getX();
					   	oldY = event.getY();
//					} // otherwise don't move the surface
				} else
					if (_mode == ZOOM) {
						Log.d(TAG, "_mode == ZOOM");
						zoomDetector.onTouchEvent(event);
					}
				break;
			case MotionEvent.ACTION_POINTER_DOWN: //second finger down
				Log.d(TAG, "ACTION_POINTER_DOWN");
			    _mode = ZOOM;
			    zoomDetector.onTouchEvent(event);
			    break;
			case MotionEvent.ACTION_POINTER_UP: //first finger lifted
				Log.d(TAG, "ACTION_POINTER_UP");
				if (_mode == ZOOM) {
					zoomDetector.onTouchEvent(event);
				}
				break;
			case MotionEvent.ACTION_UP: //second finger lifted
				Log.d(TAG, "ACTION_UP");
				if (_mode == DRAG ) {
					float xs;
					float ys;
					if (isWholeXSurfaceDisplayed()) {
						if (values[2] > 0) xs = -transX + (event.getX() - startX * scaleFactor);
						else xs = transX + event.getX() - startX * scaleFactor;
					} else xs = transX + (event.getX() - initTransX);
					
					if (isWholeYSurfaceDisplayed()) {
						if (values[5] > 0) ys = -transY + (event.getY() - startY * scaleFactor);
						else ys = transY + event.getY() - startY * scaleFactor;
					} else ys = transY + (event.getY() - initTransY);
					
					end.set(xs, ys);
					if (!onDrag) { //simple click on a cell
						int xIndex = (int) ((end.x)/ (cellWidth * scaleFactor)); 
						int yIndex = (int) ((end.y) / (cellHeight * scaleFactor));
						if (D) Log.d(TAG, "User clicked on a cell: x = " + end.x + " , y = " + end.y
									+ "(" + xIndex + ", " + yIndex + ")");
						if (validPoint(xIndex, yIndex)) { // let the margin cells as a border
							select(new MyPoint(xIndex, yIndex));
						}
					}
					_mode = NONE;
				} else
					if (_mode == ZOOM) {
						if (D) Log.d(TAG, "zoomDetector.onTouchEvent ended");
						zoomDetector.onTouchEvent(event);
						_mode = NONE;
					}
				break;
			default:
				Log.d(TAG, "Action " + (event.getAction() & MotionEvent.ACTION_MASK));
				break;
		}
		
		return true;
	 }

	private boolean validPoint(int xIndex, int yIndex) {
		if (xIndex > 0 && xIndex < (SurfaceNavigator.CELL_COUNT - 1 )&&
			yIndex > 0 && yIndex < (SurfaceNavigator.CELL_COUNT - 1)) {
			return true;
		}
		return false;
	}

	private void checkBoundaries() {
		float checkSpaceLeft;
	    float checkSpaceRight;
	    float checkSpaceUp;
	    float checkSpaceBottom;
	    
	    float[] values = new float[9];
		matrix.getValues(values);
//		if (isWholeXSurfaceDisplayed()) {
//			SPACE_LEFT = -values[2] //w * scaleFactor - SurfaceNavigator.SCREEN_WIDTH;
//			SPACE_RIGHT = 
//		}
		SPACE_LEFT = -values[2];
		SPACE_RIGHT = w*scaleFactor - SurfaceNavigator.SCREEN_WIDTH - SPACE_LEFT;
		SPACE_UP = -values[5];
		SPACE_BOTTOM = h*scaleFactor - SurfaceNavigator.SCREEN_HEIGHT - SPACE_UP;
		
		if (dx < 0) { //going to right
			//check left space
			checkSpaceLeft = SPACE_LEFT - Math.abs(dx);
			if (checkSpaceLeft >= 0) { // we can scroll by dx
				SPACE_LEFT = checkSpaceLeft;
				SPACE_RIGHT = w*scaleFactor - SurfaceNavigator.SCREEN_WIDTH - SPACE_LEFT;
			} else {
				dx = - SPACE_LEFT;
				SPACE_LEFT = 0;
				SPACE_RIGHT = w*scaleFactor - SurfaceNavigator.SCREEN_WIDTH;
			}
			if (D) Log.d(TAG, "SPACE_LEFT = " + SPACE_LEFT + ", SPACE_RIGHT = " + SPACE_RIGHT);
		} else if (dx > 0) {
			//check right space
			checkSpaceRight = SPACE_RIGHT - Math.abs(dx);
			if (checkSpaceRight >= 0) { // we can scroll by dx
				SPACE_RIGHT = checkSpaceRight;
				SPACE_LEFT = w*scaleFactor - SurfaceNavigator.SCREEN_WIDTH - SPACE_RIGHT;
			} else {
				dx = SPACE_RIGHT;
				SPACE_RIGHT = 0;
				SPACE_LEFT = w*scaleFactor - SurfaceNavigator.SCREEN_WIDTH;
			}
			if (D) Log.d(TAG, "SPACE_LEFT = " + SPACE_LEFT + ", SPACE_RIGHT = " + SPACE_RIGHT);
		}
		

		if (dy < 0) { //going down
			//check up space
			checkSpaceUp = SPACE_UP - Math.abs(dy);
			if (checkSpaceUp >= 0) { // we can scroll by dy
				SPACE_UP = checkSpaceUp;
				SPACE_BOTTOM = w*scaleFactor - SurfaceNavigator.SCREEN_HEIGHT - SPACE_UP;
			} else {
				dy = - SPACE_UP;
				SPACE_UP = 0;
				SPACE_BOTTOM = w*scaleFactor - SurfaceNavigator.SCREEN_HEIGHT;
			}
			if (D) Log.d(TAG, "SPACE_UP = " + SPACE_UP + ", SPACE_BOTTOM = " + SPACE_BOTTOM);
		} else if (dy > 0) {
			//check bottom space
			checkSpaceBottom = SPACE_BOTTOM - Math.abs(dy);
			if (checkSpaceBottom >= 0) { // we can scroll by dy
				SPACE_BOTTOM = checkSpaceBottom;
				SPACE_UP = w*scaleFactor - SurfaceNavigator.SCREEN_HEIGHT - SPACE_BOTTOM;
			} else {
				dy = SPACE_BOTTOM;
				SPACE_BOTTOM = 0;
				SPACE_UP = w*scaleFactor - SurfaceNavigator.SCREEN_HEIGHT;
			}
			if (D) Log.d(TAG, "SPACE_UP = " + SPACE_UP + ", SPACE_BOTTOM = " + SPACE_BOTTOM);
		}
		
	}	

	@Override
	protected void onDraw(Canvas canvas) {
		if (D) Log.d(TAG, "canvas : " + canvas.getWidth() + ", " + canvas.getHeight() + ", this.getWidth() = " + this.getWidth() + ", this.getHeight() = " + this.getHeight());
		
		canvas.getMatrix(matrix);
		matrix.preScale(scaleFactor, scaleFactor, mid.x, mid.y);
		canvas.setMatrix(matrix);
			
		if (D) Log.d(TAG, "scaleFactor = " + scaleFactor);
		
		drawSurface(canvas);
		
		//draw existing markers
		float x = cellWidth / 2;
		float y = cellHeight / 2 - (fm.ascent + fm.descent) / 2;
		for (Point point : myMarks) {
			canvas.drawText(Character.toString('X'), startX + point.x * cellWidth + x, startY + point.y * cellHeight + y, foreground);
		}
		
		//draw user selection
		canvas.drawRect(selRect, selected);
		
	}
	
	private float dist(PointF point, MotionEvent event) {
		float x = event.getX() - point.x;
		float y = event.getY() - point.y;
		return FloatMath.sqrt(x * x + y * y);
	}
	
	private void select(MyPoint newPoint) {
		invalidate();
		
		//check if already marked in myMarks
		if (myMarks.indexOf(newPoint) > -1 ||
				myMarks.indexOf(newPoint) > -1) {
			return;
		}
		
		getRect(newPoint.x, newPoint.y, selRect);
		
		//add to arrays
		myMarks.add(newPoint);
		
		invalidate();
		
	}
	
	private void getRect(int x, int y, Rect rect) {
		rect.set((int)(startX + x * cellWidth), (int)(startY + y * cellHeight), (int)(startX + x * cellWidth + cellWidth), (int)(startY + y * cellHeight + cellHeight));
	}
	
	private void drawSurface(Canvas canvas) {
		canvas.drawRect(startX + cellWidth, startY + cellHeight, startX + w - cellWidth, startY + h - cellHeight, background);
			
		int linesCount = (int)(h / cellHeight);
		int colsCount = (int) (w / cellWidth);
			
		//draw horizontal lines
		for (int i = 1; i <= linesCount - 1 ; i++) {
			canvas.drawLine(startX + cellWidth, startY + i*cellHeight, startX + w - cellWidth, startY + i*cellHeight, hilite);
		}
		//draw vertical lines
		for (int j = 1; j <= colsCount - 1; j++) {
			canvas.drawLine(startX + j*cellWidth, startY + cellHeight, startX + j*cellWidth , startY + h - cellHeight, hilite);
		}

		//put some colored margins
		Rect outerRect = new Rect(startX + cellWidth - borderSize, startY + cellHeight - borderSize, startX + w - cellWidth + borderSize, startY + h - cellHeight + borderSize);
		Rect innerRect = new Rect(startX + cellWidth + (int)hilite.getStrokeWidth(),
									startY + cellHeight + (int)hilite.getStrokeWidth(),
									startX + w - cellWidth - (int)hilite.getStrokeWidth(),
									startY + h - cellHeight - (int)hilite.getStrokeWidth());
		Region rgn = new Region();
		rgn.set(outerRect);
		rgn.op(innerRect, Region.Op.DIFFERENCE);
		RegionIterator iter = new RegionIterator(rgn);
		Rect r = new Rect();
		while (iter.next(r)) {
             canvas.drawRect(r, dark);
        }
		
	}

	public ArrayList<MyPoint> getMyMarks() {
		return myMarks;
	}
	
	public float getScaleFactor() {
		return scaleFactor;
	}

	public boolean isWholeSurfaceDisplayed() {
		if (w <= Math.min(SurfaceNavigator.SCREEN_WIDTH, SurfaceNavigator.SCREEN_HEIGHT)) {
			return true;
		}
		return false;
	}
	
	public boolean isWholeXSurfaceDisplayed() {
		if (w <= SurfaceNavigator.SCREEN_WIDTH) {
			return true;
		}
		return false;
	}
	
	public boolean isWholeYSurfaceDisplayed() {
		if (h <= SurfaceNavigator.SCREEN_HEIGHT) {
			return true;
		}
		return false;
	}
	
	public boolean isExtended() {
		return extended;
	}

	public void setExtended(boolean extended) {
		this.extended = extended;
		Log.d(TAG, "setExtended");
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			Log.d(TAG, "onScale " + zoomDetector.getCurrentSpan() + ", " + zoomDetector.getPreviousSpan());
			scaleFactor *= detector.getScaleFactor();
			scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
			if (scaleFactor != 1) invalidate();
			
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			super.onScaleBegin(detector);
			if (!isWholeXSurfaceDisplayed() && !isWholeYSurfaceDisplayed()) {
				MySurface.this.scrollTo(0, 0);
			}
			
			return true;
		}

	}
	
}
