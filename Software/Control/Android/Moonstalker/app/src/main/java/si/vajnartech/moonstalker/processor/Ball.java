package si.vajnartech.moonstalker.processor;

public final class Ball
{
    Runnable ioAction;
    Runnable uxAction;

    public Ball(Runnable ioAction, Runnable uxAction)
    {
        this.ioAction = ioAction;
        this.uxAction = uxAction;
    }
}
