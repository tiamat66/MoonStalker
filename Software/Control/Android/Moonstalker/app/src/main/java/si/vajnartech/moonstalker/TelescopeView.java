package si.vajnartech.moonstalker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class TelescopeView extends View {
    private float azimuth = 0;   // 0 to 360
    private float elevation = 0; // 0 to 90
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TelescopeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void update(float azimuth, float elevation) {
        this.azimuth = azimuth;
        this.elevation = elevation;
        postInvalidate(); // Redraw on UI thread
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        int size = Math.min(width / 2, height) - 60;
        int centerY = height / 2;

        // --- Draw Azimuth (Top View - Left Side) ---
        int centerXAz = width / 4;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(centerXAz, centerY, size / 2f, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        canvas.drawText("N", centerXAz, centerY - size / 2f - 10, paint);
        canvas.drawText("Az: " + String.format("%.1f°", azimuth), centerXAz, centerY + size / 2f + 40, paint);
        
        // Telescope pointer for Azimuth
        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(8);
        float azRad = (float) Math.toRadians(azimuth - 90);
        float azStopX = centerXAz + (float) Math.cos(azRad) * (size / 2f);
        float azStopY = centerY + (float) Math.sin(azRad) * (size / 2f);
        canvas.drawLine(centerXAz, centerY, azStopX, azStopY, paint);

        // --- Draw Elevation (Side View - Right Side) ---
        int centerXEl = (width * 3) / 4;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.DKGRAY);
        paint.setStrokeWidth(4);
        
        // Ground line and Zenith line
        float groundY = centerY + size / 4f;
        canvas.drawLine(centerXEl - size / 2f, groundY, centerXEl + size / 2f, groundY, paint);
        
        // Elevation arc (0 to 90)
        canvas.drawArc(centerXEl - size / 2f, centerY - size / 4f, centerXEl + size / 2f, groundY + size / 4f, 180, 180, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawText("Zenith", centerXEl, centerY - size / 4f - 20, paint);
        canvas.drawText("El: " + String.format("%.1f°", elevation), centerXEl, groundY + 40, paint);

        // Telescope pointer for Elevation
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(8);
        float elRad = (float) Math.toRadians(-elevation); // Negative because Canvas Y is down
        float elStopX = centerXEl + (float) Math.cos(elRad) * (size / 2f);
        float elStopY = groundY + (float) Math.sin(elRad) * (size / 2f);
        canvas.drawLine(centerXEl, groundY, elStopX, elStopY, paint);
    }
}
