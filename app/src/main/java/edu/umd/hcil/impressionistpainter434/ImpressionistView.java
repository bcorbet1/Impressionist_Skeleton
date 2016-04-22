package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Bitmap _imageMap;
    private Paint _paint = new Paint();
    private VelocityTracker trackSpeed = null;
    private boolean complimentaryColor = false;

    private int _alpha = 150;
    private int _defaultRadius = 15;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){

        _imageView = imageView;
        _imageMap = _imageView.getDrawingCache();
    }

    public void ComplimentaryColor() {
        if (complimentaryColor == true)
            complimentaryColor = false;
        else
            complimentaryColor = true;
    }

    public boolean getComplimentaryColor() {

        return complimentaryColor;
    }


    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){

        _brushType = brushType;
    }

    public Bitmap get_offScreenBitmap() {

        return _offScreenBitmap;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        if(_offScreenCanvas != null){
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
        _imageMap = _imageView.getDrawingCache();

    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float currentTouchX = motionEvent.getX();
        float currentTouchY = motionEvent.getY();
        int curTouchX = (int) currentTouchX;
        int curTouchY = (int) currentTouchY;
        float brushRadius = _defaultRadius;
        int index = motionEvent.getActionIndex();
        int a = motionEvent.getActionMasked();
        int pid = motionEvent.getPointerId(index);

        if(curTouchX < 0){
            curTouchX = 0;
        }

        if(curTouchY < 0){
            curTouchY = 0;
        }

        if(curTouchX >= _imageMap.getWidth()){
            curTouchX = _imageMap.getWidth() - 1;
        }

        if(curTouchY >= _imageMap.getWidth()){
            curTouchY = _imageMap.getWidth() - 1;
        }

        int pixel = _imageMap.getPixel(curTouchX, curTouchY);

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (trackSpeed == null) {
                    trackSpeed = VelocityTracker.obtain();
                } else {
                    trackSpeed.clear();
                }
                trackSpeed.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();
                _paint.setColor(pixel);
                _paint.setStrokeWidth(brushRadius);

                if (complimentaryColor) {
                    int alpha = Color.alpha(pixel);
                    int red = Color.red(pixel);
                    int blue = Color.blue(pixel);
                    int green = Color.green(pixel);

                    red = (~red) & 0xff;
                    blue = (~blue) & 0xff;
                    green = (~green) & 0xff;

                    int complimentaryColor = Color.argb(alpha, red, blue, green);
                    _paint.setColor(complimentaryColor);
                }

                for (int i = 0; i < historySize; i++) {

                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if (_brushType == _brushType.Square) {
                        _offScreenCanvas.drawRect(touchX, touchY, touchX + brushRadius, touchY + brushRadius, _paint);
                    }
                    if (_brushType == _brushType.Circle) {
                        _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, _paint);
                    }
                    if (_brushType == _brushType.Line) {
                        _offScreenCanvas.drawLine(touchX, touchY, touchX + brushRadius, touchY + brushRadius, _paint);
                    }
                    if (_brushType == _brushType.SpeedBrush) {
                        trackSpeed.addMovement(motionEvent);
                        trackSpeed.computeCurrentVelocity(1000);

                        int xVelocity = Math.abs((int) trackSpeed.getXVelocity(pid));
                        int yVelocity = Math.abs((int) trackSpeed.getYVelocity(pid));
                        int velocity = Math.max(xVelocity, yVelocity);
                        brushRadius = getMotionBrushRadius(velocity);

                        _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, _paint);
                    }

                }

                break;
            case MotionEvent.ACTION_UP:
                trackSpeed.recycle();
                trackSpeed = null;
                break;
        }

        invalidate();
        return true;
    }

    private int getMotionBrushRadius(int velocity) {
        int brushRadius = _defaultRadius;

        if (velocity >= 0 && velocity < 100)
            brushRadius = 5;
        if (velocity >= 100 && velocity < 120)
            brushRadius = 6;
        if (velocity >= 120 && velocity < 150)
            brushRadius = 10;
        if (velocity >= 150 && velocity < 180)
            brushRadius = 12;
        if (velocity >= 180 && velocity < 220)
            brushRadius = 15;
        if (velocity >= 220 && velocity < 250)
            brushRadius = 20;
        if (velocity >= 250 && velocity < 300)
            brushRadius = 25;
        if (velocity >= 300 && velocity < 330)
            brushRadius = 30;
        if (velocity >= 330)
            brushRadius = 40;

        return brushRadius;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

