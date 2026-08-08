package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

public class ManualMoveFragment extends MyFragment implements View.OnTouchListener
{
    private String currentDirection = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        View keypad = Objects.requireNonNull(view).findViewById(R.id.keypad);
        keypad.setOnTouchListener(this);
        keypad.setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        float centerX = view.getWidth() / 2f;
        float centerY = view.getHeight() / 2f;

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                String newDirection = getDirection(x - centerX, y - centerY, view.getWidth());
                if (newDirection != null) {
                    if (!newDirection.equals(currentDirection)) {
                        if (currentDirection != null) {
                            act.moveEnd();
                        }
                        currentDirection = newDirection;
                        act.moveStart(currentDirection);
                    }
                } else if (currentDirection != null) {
                    act.moveEnd();
                    currentDirection = null;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentDirection != null) {
                    act.moveEnd();
                    currentDirection = null;
                }
                break;
        }
        view.performClick();
        return true;
    }

    private String getDirection(float dx, float dy, int viewWidth)
    {
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < viewWidth * 0.15f) return null; // Dead zone (15% of width)

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;

        // Sectors: 45 degrees each
        if (angle >= 337.5 || angle < 22.5) return "E";
        if (angle < 67.5) return "SE";
        if (angle < 112.5) return "S";
        if (angle < 157.5) return "SW";
        if (angle < 202.5) return "W";
        if (angle < 247.5) return "NW";
        if (angle < 292.5) return "N";
        return "NE";
    }
}
