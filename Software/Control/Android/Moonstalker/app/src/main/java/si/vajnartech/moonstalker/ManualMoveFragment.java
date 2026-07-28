package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManualMoveFragment extends MyFragment implements View.OnTouchListener
{
    private final AtomicBoolean fingerDown = new AtomicBoolean(false);

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
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (fingerDown.get()) break;
                fingerDown.set(true);
                act.moveStart("N");
                break;
            case MotionEvent.ACTION_UP:
                if (fingerDown.get()) {
                    fingerDown.set(false);
                    act.moveEnd();
                }
        }
        view.performClick();
        return true;
    }
}
