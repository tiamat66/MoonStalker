package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SVGDrawable extends Drawable
{
  protected SVG image;
  int alpha;
  ColorFilter colorFilter;

  protected PointF size;

  boolean softwareRendering = true;
  Bitmap cachedRender = null; // Stored previous rendering so that we don't render needlessly again

  public SVGDrawable(Resources res, int id, int height, int width)
  {
    try {
      SVG image = SVG.getFromResource(res, id);
      image.setRenderDPI(res.getDisplayMetrics().xdpi);
      image.setDocumentHeight(height);
      image.setDocumentWidth(width);
      setImage(image);
    } catch (SVGParseException e) {
      Log.i("SVG", e.toString());
    }
  }

  public SVGDrawable(Resources res, int id)
  {
    try {
      SVG image = SVG.getFromResource(res, id);
      image.setRenderDPI(res.getDisplayMetrics().xdpi);
      setImage(image);
    } catch (SVGParseException e) {
      Log.i("SVG", e.toString());
    }
  }

  public SVGDrawable(SVG image)
  {
    setImage(image);
  }

  public void setImage(SVG image)
  {
    this.image = image;
    size = new PointF(image.getDocumentWidth(), image.getDocumentHeight());
    RectF vb = image.getDocumentViewBox();
    if (size.x == -1) size.x = vb != null ? vb.width() : image.getRenderDPI() * 0.3f;
    if (size.y == -1) size.y = vb != null ? vb.height() : image.getRenderDPI() * 0.3f;
    if (vb == null) image.setDocumentViewBox(0, 0, size.x, size.y);
    try {
      image.setDocumentWidth("100%");
      image.setDocumentHeight("100%");
    } catch (SVGParseException ignored) { }
  }

  @SuppressLint("NewApi")
  @Override
  public void draw(Canvas canvas)
  {
    // TODO: actual rendering to Picture in SVGParser, there's no need to keep SVG in RAM after it can be reliably rendered. Picture is much more efficient

    // getBounds lies!!! actual rendering view port may be 48x48 or even more yet here you will get 24x24 (getIntrinsicBounds). Result: blurry rendering
    // and there is just no way to get current transformation matrix (canvas.getMatrix() is faulty - http://code.google.com/p/android/issues/detail?id=24517)
    Rect bounds = getBounds();
    int saveCount = canvas.save();

    canvas.clipRect(bounds); // Make sure our SVG won't exceed its bounds
    canvas.translate(-bounds.left,
                     -bounds.top); // offset the viewport so that it starts at 0,0 and extends to width, height
    // TODO: image.root.setFilters(alpha, colorFilter);
    int width = Math.max(1, bounds.width());
    int height = Math.max(1, bounds.height());
    /*
      if (width == 0 || height == 0) {
        canvas.restoreToCount(saveCount);
        return;
      }
     */

    Canvas target = null;
    Bitmap b = null;
    // Hardware rendering never works, so here we do a workaround to always draw in software mode
    if (softwareRendering && canvas.isHardwareAccelerated()) {
      while (width < 64 * image.getRenderDPI() / 160f && height < 64 * image.getRenderDPI() / 160f) { // I want at least 64 dp large bitmap to draw into (see "getBounds lies" above)
        width *= 2;
        height *= 2;
      }
      if (cachedRender != null && cachedRender.getWidth() == width) b = cachedRender;
      else {
        b = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        target = new Canvas(b);
      }
    } else target = canvas;

    try {
      if (target != null) image.renderToCanvas(target, new RectF(0, 0, width, height));
    } catch (Exception ignored) {
    }

    if (softwareRendering && canvas.isHardwareAccelerated()) {
      assert b != null;
      canvas.drawBitmap(b, null, new Rect(0, 0, bounds.width(), bounds.height()), null);
      cachedRender = b;
    }
    canvas.restoreToCount(saveCount);
  }

  @Override
  public int getIntrinsicWidth()
  {
    return Math.round(size.x);
  }

  @Override
  public int getIntrinsicHeight()
  {
    return Math.round(size.y);
  }

  @Override
  public int getOpacity()
  {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public void setAlpha(int alpha)
  {
    this.alpha = alpha;
  }

  @Override
  public void setColorFilter(ColorFilter cf)
  {
    this.colorFilter = cf;
  }

  public Picture getPicture()
  {
    Picture pic = new Picture();
    Canvas c = pic.beginRecording(getIntrinsicWidth(), getIntrinsicHeight());
    setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
    draw(c);
    pic.endRecording();
    return pic;
  }

  public PictureDrawable getPictureDrawable()
  {
    return new PictureDrawable(getPicture());
  }

  public Bitmap getBitmap(Rect bounds)
  {
    //Picture pic = getPicture();
    if (bounds == null) bounds = new Rect(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
    Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    image.renderToCanvas(canvas);
    return bitmap;
  }

  public BitmapDrawable getBitmapDrawable(Resources res, Rect bounds)
  {
    BitmapDrawable bd = new BitmapDrawable(res, getBitmap(bounds));
    if (bounds != null) bd.setBounds(bounds);
    return bd;
  }

  public SVG getImage(){
    return image;
  }
}