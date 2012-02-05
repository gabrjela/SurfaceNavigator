package my.pack;


import java.util.ArrayList;
import java.util.Arrays;

import my.pack.utils.MyPoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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
	private float deltaX = 0;
	private float deltaY = 0;
	// coords of total distance dragged since the beginning of the app
	private float deltaXs = 0;
	private float deltaYs = 0;
    
	// Arrays of marked points on screen (drawn as X symbol)
	private ArrayList<MyPoint> myMarks = null;
    
	// Flag indicating wheter a simple click on a cell or a drag operation
    private boolean onDrag = false;
    
    /* Remaining space along the edges of the displayed canvas.
     * Used to stop the dragging when an edge of the canvas reached.
     */
    private static float SPACE_UP;
    private static float SPACE_BOTTOM;
    private static float SPACE_LEFT;
    private static float SPACE_RIGHT;
    
    // Depending on this value we'll have a thiner or thicker line separator 
    public static float BIG_CELL_SIZE = 48;
    
    
	public MySurface(Context context, int width, int height) {
		super(context);
		if (D) Log.d(TAG, "Creating MySurface");
		this.w = width;
		this.h = height;
		
		this.setFocusable(true);
		setFocusableInTouchMode(true);
		
		cellWidth = w / SurfaceNavigator.CELL_COUNT;
		cellHeight = h / SurfaceNavigator.CELL_COUNT;
		if (D) Log.d(TAG, "cellWidth = " + cellWidth);
		
		SPACE_UP = (h - SurfaceNavigator.SCREEN_HEIGHT) /2;
		SPACE_BOTTOM = SPACE_UP;
		SPACE_LEFT = (w - SurfaceNavigator.SCREEN_WIDTH) /2;
		SPACE_RIGHT = SPACE_LEFT;
		if (D) Log.d(TAG, "SPACE_LEFT = " + SPACE_LEFT + ", SPACE_RIGHT = " + SPACE_RIGHT);
		if (D) Log.d(TAG, "SPACE_UP = " + SPACE_UP + ", SPACE_BOTTOM = " + SPACE_BOTTOM);
		
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
		dark.setStrokeWidth(9);dark.setStyle(Style.FILL_AND_STROKE);
		
		hilite = new Paint();
		hilite.setColor(getResources().getColor(R.color.grid_hilite));
		
		light = new Paint();
		light.setColor(getResources().getColor(R.color.grid_light));
		
		// Define color for the selected cell
		selected = new Paint();
		selected.setColor(getResources().getColor(R.color.grid_selected));
		
	}
	
	/** Called when surface info retrieved from Activity bundle */
	public MySurface(Context context, int width, int height, MyPoint[] myMarks) {
		this(context, width, height);
		this.myMarks = new ArrayList<MyPoint>(Arrays.asList(myMarks));
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: //finger down
			   	start.set(event.getX(), event.getY());
			   	_mode = DRAG;
			   	//reset onMove
			   	onDrag = false;
			   			
			   	oldX = start.x;
			   	oldY = start.y;
				break;
			case MotionEvent.ACTION_MOVE: //finger dragging
				if (_mode == DRAG ) {
					dx = - event.getX() + oldX;
					dy = - event.getY() + oldY; 
					checkBoundaries();
							
					//if dragging done on a distance more than a cell size, then definitely a drag operation
					if (dist(start, event) >= cellHeight) onDrag = true;
							
					this.scrollBy((int)dx, (int)dy);
							
					oldX = event.getX();
				   	oldY = event.getY();
				}
						
				break;		
			case MotionEvent.ACTION_POINTER_UP: //finger lifted
			case MotionEvent.ACTION_UP: //finger lifted
				end.set(event.getX(), event.getY());
				deltaXs += deltaX;
				deltaYs += deltaY;
				if (!onDrag) { //simple click on a cell
					int xIndex = (int) ((end.x + deltaXs) / cellWidth); 
					int yIndex = (int) ((end.y + deltaYs) / cellHeight);
					if (D) Log.d(TAG, "User clicked on a cell: (" + xIndex + ", " + yIndex + ")");
					if (notOnMargin(xIndex, yIndex)) { // let the margin cells as a border
						select(new MyPoint(xIndex, yIndex)); 
					}
				}
				_mode = NONE;
				deltaX = 0;
				deltaY = 0;
				break;
		}
		return true;
	 }

	private boolean notOnMargin(int xIndex, int yIndex) {
		if (xIndex == 0 ||
			yIndex == 0 ||
			xIndex == SurfaceNavigator.CELL_COUNT ||
			yIndex == SurfaceNavigator.CELL_COUNT) {
			return false;
		}
		return true;
	}

	private void checkBoundaries() {
		float checkSpaceLeft;
	    float checkSpaceRight;
	    float checkSpaceUp;
	    float checkSpaceBottom;
		if (dx < 0) { //going to right
			//check left space
			checkSpaceLeft = SPACE_LEFT - Math.abs(dx);
			if (checkSpaceLeft >= 0) { // we can scroll by dx
				SPACE_LEFT = checkSpaceLeft;
				SPACE_RIGHT = w - SurfaceNavigator.SCREEN_WIDTH - SPACE_LEFT;
			} else {
				dx = - SPACE_LEFT;
				SPACE_LEFT = 0;
				SPACE_RIGHT = w - SurfaceNavigator.SCREEN_WIDTH;
			}
			deltaX += dx;
			Log.d(TAG, "SPACE_LEFT = " + SPACE_LEFT + ", SPACE_RIGHT = " + SPACE_RIGHT + ", deltaX = " + deltaX);
		} else if (dx > 0) {
			//check right space
			checkSpaceRight = SPACE_RIGHT - Math.abs(dx);
			if (checkSpaceRight >= 0) { // we can scroll by dx
				SPACE_RIGHT = checkSpaceRight;
				SPACE_LEFT = w - SurfaceNavigator.SCREEN_WIDTH - SPACE_RIGHT;
			} else {
				dx = SPACE_RIGHT;
				SPACE_RIGHT = 0;
				SPACE_LEFT = w - SurfaceNavigator.SCREEN_WIDTH;
			}
			deltaX += dx;
		}
		

		if (dy < 0) { //going down
			//check up space
			checkSpaceUp = SPACE_UP - Math.abs(dy);
			if (checkSpaceUp >= 0) { // we can scroll by dy
				SPACE_UP = checkSpaceUp;
				SPACE_BOTTOM = w - SurfaceNavigator.SCREEN_HEIGHT - SPACE_UP;
			} else {
				dy = - SPACE_UP;
				SPACE_UP = 0;
				SPACE_BOTTOM = w - SurfaceNavigator.SCREEN_HEIGHT;
			}
			deltaY += dy;
			Log.d(TAG, "SPACE_UP = " + SPACE_UP + ", SPACE_BOTTOM = " + SPACE_BOTTOM + ", deltaY = " + deltaY);
		} else if (dy > 0) {
			//check bottom space
			checkSpaceBottom = SPACE_BOTTOM - Math.abs(dy);
			if (checkSpaceBottom >= 0) { // we can scroll by dy
				SPACE_BOTTOM = checkSpaceBottom;
				SPACE_UP = w - SurfaceNavigator.SCREEN_HEIGHT - SPACE_BOTTOM;
			} else {
				dy = SPACE_BOTTOM;
				SPACE_BOTTOM = 0;
				SPACE_UP = w - SurfaceNavigator.SCREEN_HEIGHT;
			}
			deltaY += dy;
		}
		
	}	

	@Override
	protected void onDraw(Canvas canvas) {
		drawSurface(canvas);
		
		//draw existing markers
		float x = cellWidth / 2;
		float y = cellHeight / 2 - (fm.ascent + fm.descent) / 2;
		for (Point point : myMarks) {
			canvas.drawText(Character.toString('X'), point.x * cellWidth + x, point.y * cellHeight + y, foreground);
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
		invalidate(selRect);
		
		//check if already marked in myMarks
		if (myMarks.indexOf(newPoint) > -1 ||
				myMarks.indexOf(newPoint) > -1) {
			return;
		}
		
		getRect(newPoint.x, newPoint.y, selRect);
		
		//add to arrays
		myMarks.add(newPoint);
		
		invalidate(selRect);
		
	}
	
	private void getRect(int x, int y, Rect rect) {
		rect.set((int)(x*cellWidth), (int)(y*cellHeight), (int)(x*cellWidth + cellWidth), (int)(y*cellHeight + cellHeight));
	}
	
	private void drawSurface(Canvas canvas) {
		canvas.drawRect(cellWidth, cellHeight, w - cellWidth, h - cellHeight, background);
		
		int linesCount = (int)(h / cellHeight);
		int colsCount = (int) (w / cellWidth);
		
		//draw horizontal lines
		for (int i = 1; i <= linesCount - 1 ; i++) {
			canvas.drawLine(cellWidth, i*cellHeight, w - cellWidth, i*cellHeight, light);
			canvas.drawLine(cellWidth, i*cellHeight + 1, w - cellWidth, i*cellHeight + 1, hilite);
			if (cellWidth > BIG_CELL_SIZE) canvas.drawLine(cellWidth, i*cellHeight + 2, w - cellWidth, i*cellHeight + 2, hilite);
		}
		//draw vertical lines
		for (int j = 1; j <= colsCount - 1; j++) {
			canvas.drawLine(j*cellWidth, cellHeight, j*cellWidth , h - cellHeight, light);
			canvas.drawLine(j*cellWidth + 1, cellHeight, j*cellWidth + 1 , h - cellHeight, hilite);
			if (cellWidth > BIG_CELL_SIZE) canvas.drawLine(j*cellWidth + 2, cellHeight, j*cellWidth + 2 , h - cellHeight, hilite);
		}
		
		//put some colored margins
		canvas.drawLine(cellWidth, cellHeight, w - cellWidth, cellHeight, dark);
		canvas.drawLine(cellWidth - 2, cellHeight - 2, w - cellWidth + 2, cellHeight - 2, dark);
		canvas.drawLine(cellWidth, (linesCount - 1) * cellHeight, w - cellWidth, (linesCount - 1) * cellHeight, dark);
		canvas.drawLine(cellWidth + 2, (linesCount - 1) * cellHeight + 2, w - cellWidth + 2, (linesCount - 1) * cellHeight + 2, dark);
		
		canvas.drawLine(cellWidth, cellHeight, cellWidth , h - cellHeight, dark);
		canvas.drawLine(cellWidth - 2, cellHeight - 2, cellWidth - 2 , h - cellHeight + 2, dark);
		canvas.drawLine((colsCount - 1) * cellWidth, cellHeight, (colsCount - 1) * cellWidth , h - cellHeight, dark);
		canvas.drawLine((colsCount - 1) * cellWidth + 2, cellHeight - 2, (colsCount - 1) * cellWidth + 2 , h - cellHeight + 2, dark);
		
	}

	public ArrayList<MyPoint> getMyMarks() {
		return myMarks;
	}
	
}
