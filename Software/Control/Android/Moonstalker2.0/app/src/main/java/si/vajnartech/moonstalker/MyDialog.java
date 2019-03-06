package si.vajnartech.moonstalker;

import android.app.AlertDialog;
import android.os.Handler;

class MyDialog extends AlertDialog.Builder
{
  MyDialog(final MainActivity ctx, String msg, int to, Runnable run)
  {
    super(ctx);
    setTitle(msg);
    create();
    show();
    if (run != null)
      new Handler().postDelayed(run, to);
  }
}