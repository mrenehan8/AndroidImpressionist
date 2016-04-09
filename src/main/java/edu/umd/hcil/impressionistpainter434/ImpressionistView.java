package edu.umd.hcil.impressionistpainter434;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Random;
import java.util.UUID;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
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
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    public void savePainting() {
        File file = new File(Environment.getExternalStorageDirectory().toString() + "/Pictures/" + UUID.randomUUID() + ".jpg");


        if ( !file.exists() )
        {
            try {
                boolean success = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

       // System.out.println(success+"file");



        FileOutputStream ostream = null;
        try {
            ostream = new FileOutputStream(file);
            _offScreenBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, file.getPath());

        getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Toast.makeText(getContext(), "Image Saved!", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();
        //Paint _paint = new Paint();
        //_paint.setColor(Color.RED);
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:

            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();
                Bitmap temp = _imageView.getDrawingCache();
                for (int i = 0; i < historySize; i++) {

                    float touchHX = motionEvent.getHistoricalX(i);
                    float touchHY = motionEvent.getHistoricalY(i);
                    float pressure = motionEvent.getHistoricalPressure(i);
                    float width = 0;


                    if(_brushType == BrushType.Square) {
                        if (touchHX - _defaultRadius > getBitmapPositionInsideImageView(_imageView).left && touchHX < getBitmapPositionInsideImageView(_imageView).right
                                && touchHY - _defaultRadius > getBitmapPositionInsideImageView(_imageView).top && touchHY < getBitmapPositionInsideImageView(_imageView).bottom) {
                            int pix = temp.getPixel((int) touchHX, (int) touchHY);
                            _paint.setColor(pix);
                            _offScreenCanvas.drawRect(touchHX - _defaultRadius, touchY - _defaultRadius, touchHX, touchHY, _paint);
                        }
                    } else if (_brushType == BrushType.Circle){
                        if (touchHX - _defaultRadius/2 > getBitmapPositionInsideImageView(_imageView).left && touchHX < getBitmapPositionInsideImageView(_imageView).right
                                && touchHY - _defaultRadius/2 > getBitmapPositionInsideImageView(_imageView).top && touchHY < getBitmapPositionInsideImageView(_imageView).bottom) {
                            int pix = temp.getPixel((int) touchHX, (int) touchHY);
                            _paint.setColor(pix);
                            _offScreenCanvas.drawCircle(touchHX, touchHY, _defaultRadius / 2, _paint);

                        }
                    } else if (_brushType == BrushType.SquarePressure){
                        if (motionEvent.getPressure()>=0.00 && motionEvent.getPressure()<0.05)
                        {
                            width = 5;
                        }
                        else if (motionEvent.getPressure()>=0.05 && motionEvent.getPressure()<0.10)
                        {
                            width = 15;
                        }
                        else if (motionEvent.getPressure()>=0.10 && motionEvent.getPressure()<0.15)
                        {
                            width = 30;
                        }
                        else if (motionEvent.getPressure()>=0.15 && motionEvent.getPressure() <0.20)
                        {
                            width = 40;
                        }
                        else if (motionEvent.getPressure()>=0.20 && motionEvent.getPressure()<0.25)
                        {
                            width=50;
                        }
                        else if (motionEvent.getPressure() >= 0.25 && motionEvent.getPressure() <0.30)
                        {
                            width = 60;
                        }
                        else if (motionEvent.getPressure() >= 0.30 && motionEvent.getPressure()<0.35)
                        {
                            width = 70;
                        }
                        else if (motionEvent.getPressure() >= 0.35 && motionEvent.getPressure()<0.40)
                        {
                            width = 80;
                        }
                        else if (motionEvent.getPressure() >= 0.40 && motionEvent.getPressure()<0.45)
                        {
                            width = 90;
                        }
                        else if (motionEvent.getPressure()>= 0.45 && motionEvent.getPressure()<0.60)
                        {
                            width = 100;
                        } else {
                            width =450;
                        }
                        if (touchHX - width > getBitmapPositionInsideImageView(_imageView).left && touchHX < getBitmapPositionInsideImageView(_imageView).right
                                && touchHY - width > getBitmapPositionInsideImageView(_imageView).top && touchHY < getBitmapPositionInsideImageView(_imageView).bottom) {
                            int pix = temp.getPixel((int) touchHX, (int) touchHY);
                            _paint.setColor(pix);
                            _offScreenCanvas.drawRect(touchHX - width, touchY - width, touchHX, touchHY, _paint);
                        }
                    }
                    //_offScreenCanvas.drawPoint(touchHX, touchHY, _paint);
                }
                if(_brushType == BrushType.Square) {
                    if (touchX - _defaultRadius > getBitmapPositionInsideImageView(_imageView).left && touchX < getBitmapPositionInsideImageView(_imageView).right
                            && touchY - _defaultRadius > getBitmapPositionInsideImageView(_imageView).top && touchY < getBitmapPositionInsideImageView(_imageView).bottom) {
                        int pix = temp.getPixel((int) touchX, (int) touchY);
                        _paint.setColor(pix);
                        _offScreenCanvas.drawRect(touchX - _defaultRadius, touchY - _defaultRadius, touchX, touchY, _paint);
                    }
                } else if (_brushType == BrushType.Circle){
                    if (touchX - _defaultRadius/2 > getBitmapPositionInsideImageView(_imageView).left && touchX < getBitmapPositionInsideImageView(_imageView).right
                            && touchY - _defaultRadius/2 > getBitmapPositionInsideImageView(_imageView).top && touchY < getBitmapPositionInsideImageView(_imageView).bottom) {
                        int pix = temp.getPixel((int) touchX, (int) touchY);
                        _paint.setColor(pix);
                        _offScreenCanvas.drawCircle(touchX, touchY, _defaultRadius / 2, _paint);

                    }
                } else if (_brushType == BrushType.SquarePressure){
                    float width = 0;
                    if (motionEvent.getPressure()>=0.00 && motionEvent.getPressure()<0.05)
                    {
                        width = 5;
                    }
                    else if (motionEvent.getPressure()>=0.05 && motionEvent.getPressure()<0.10)
                    {
                        width = 15;
                    }
                    else if (motionEvent.getPressure()>=0.10 && motionEvent.getPressure()<0.15)
                    {
                        width = 30;
                    }
                    else if (motionEvent.getPressure()>=0.15 && motionEvent.getPressure() <0.20)
                    {
                        width = 40;
                    }
                    else if (motionEvent.getPressure()>=0.20 && motionEvent.getPressure()<0.25)
                    {
                        width=50;
                    }
                    else if (motionEvent.getPressure() >= 0.25 && motionEvent.getPressure() <0.30)
                    {
                        width = 60;
                    }
                    else if (motionEvent.getPressure() >= 0.30 && motionEvent.getPressure()<0.35)
                    {
                        width = 70;
                    }
                    else if (motionEvent.getPressure() >= 0.35 && motionEvent.getPressure()<0.40)
                    {
                        width = 80;
                    }
                    else if (motionEvent.getPressure() >= 0.40 && motionEvent.getPressure()<0.45)
                    {
                        width = 90;
                    }
                    else if (motionEvent.getPressure()>= 0.45 && motionEvent.getPressure()<0.60)
                    {
                        width = 100;
                    } else {
                        width =450;
                    }
                    if (touchX - width > getBitmapPositionInsideImageView(_imageView).left && touchX < getBitmapPositionInsideImageView(_imageView).right
                            && touchY - width > getBitmapPositionInsideImageView(_imageView).top && touchY < getBitmapPositionInsideImageView(_imageView).bottom) {
                        int pix = temp.getPixel((int) touchX, (int) touchY);
                        _paint.setColor(pix);
                        _offScreenCanvas.drawRect(touchX - width, touchY - width, touchX, touchY, _paint);
                    }
                }
                //_offScreenCanvas.drawPoint(touchX, touchY, _paint);



                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    public void setBlackAndWhite(){
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        _paint.setColorFilter(f);
    }

    public void setColorBack(){
        _paint.setColorFilter(null);

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

