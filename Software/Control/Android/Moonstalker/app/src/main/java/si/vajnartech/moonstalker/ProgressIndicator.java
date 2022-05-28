package si.vajnartech.moonstalker;

import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public final class ProgressIndicator
{
  private LinearLayout llProgress = null;

  private final MainActivity act;

  public enum ProgressType
  {
    CONNECTING,
    INITIALIZING,
    MOVING
  }

  ProgressIndicator(MainActivity act)
  {
    this.act = act;
  }

  public void progressOn(ProgressType type)
  {
    ProgressBar progress = new ProgressBar(act);
    LinearLayout.LayoutParams lp_progress = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp_progress.gravity = Gravity.CENTER;
    lp_progress.weight = 0;

    progress.setLayoutParams(lp_progress);

    int color = act.getResources().getColor(R.color.colorAccent, null);
    if (progress.getIndeterminateDrawable() != null)
      progress.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

    TextView loadingText = new TextView(act);
    LinearLayout.LayoutParams lp_loading = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp_loading.weight = 0;
    lp_loading.gravity = Gravity.CENTER;
    if (type == ProgressType.CONNECTING)
      loadingText.setText(act.tx(R.string.connecting));
    else if (type == ProgressType.INITIALIZING)
      loadingText.setText(act.tx(R.string.initializing));
    else if (type == ProgressType.MOVING)
      loadingText.setText(act.tx(R.string.moving));

    loadingText.setTextColor(act.getResources().getColor(R.color.colorAccent, null));
    loadingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
    loadingText.setLayoutParams(lp_loading);

    llProgress = new LinearLayout(act);
    llProgress.setOrientation(LinearLayout.VERTICAL);
    llProgress.setBackgroundColor(act.getResources().getColor(R.color.colorPrimary, null));
    llProgress.addView(progress, lp_progress);
    llProgress.addView(loadingText, lp_loading);
    final FrameLayout.LayoutParams lp_ll = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    );
    lp_ll.gravity = Gravity.CENTER;
    llProgress.setLayoutParams(lp_ll);
    act.runOnUiThread(() -> ((ConstraintLayout) act.findViewById(R.id.content)).addView(llProgress, lp_ll));
  }

  public void progressStop()
  {
    if (llProgress != null) {
      act.runOnUiThread(() -> {
        ((ConstraintLayout) act.findViewById(R.id.content)).removeView(llProgress);
        llProgress = null;
      });
    }
  }
}
