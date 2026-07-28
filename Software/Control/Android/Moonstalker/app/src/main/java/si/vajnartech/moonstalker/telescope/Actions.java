package si.vajnartech.moonstalker.telescope;

public interface Actions
{
    void updateStatus(int val);

    void updateStatus(int val, String msg);

    void updateStatus(int val, Object msg);

    void updateMode(int val);
}
