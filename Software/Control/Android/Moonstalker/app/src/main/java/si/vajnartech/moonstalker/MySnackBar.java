package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.SnackbarContentLayout;

public final class MySnackBar
{
  public static Snackbar makeSnackbar(MainActivity act ,String text, int duration)
  {
    Snackbar snack = Snackbar.make(act.findViewById(R.id.drawer_layout), text, duration);
    View     view  = snack.getView();
    view.setBackgroundColor(act.getResources().getColor(R.color.colorAccent, null));
    TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
    tv.setTextColor(act.getResources().getColor(R.color.textInvertedNormal, null));
    ((SnackbarContentLayout) tv.getParent()).setBackgroundColor(act.getResources().getColor(R.color.colorAccent, null));
    Button btn = view.findViewById(com.google.android.material.R.id.snackbar_action);
    if (btn != null) {
      btn.setTextColor(act.getResources().getColor(R.color.textInvertedNormal, null));
    }
    return snack;
  }
}
