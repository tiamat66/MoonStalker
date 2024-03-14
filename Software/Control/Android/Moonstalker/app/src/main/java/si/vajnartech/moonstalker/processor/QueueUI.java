package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.C.ST_ASTRO_DATA;
import static si.vajnartech.moonstalker.C.ST_BATTERY;
import static si.vajnartech.moonstalker.C.ST_CONNECTION_ERROR;
import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.ST_INFO;
import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_NOT_READY;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_WAITING;
import static si.vajnartech.moonstalker.C.ST_WARNING;
import static si.vajnartech.moonstalker.OpCodes.MSG_BATTERY;
import static si.vajnartech.moonstalker.OpCodes.MSG_BATTERY_RES;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONNECT;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_GET_ASTRO_DATA;
import static si.vajnartech.moonstalker.OpCodes.MSG_INFO;
import static si.vajnartech.moonstalker.OpCodes.MSG_MOVE;
import static si.vajnartech.moonstalker.OpCodes.MSG_MV_ACK;
import static si.vajnartech.moonstalker.OpCodes.MSG_NOT_READY;
import static si.vajnartech.moonstalker.OpCodes.MSG_PING;
import static si.vajnartech.moonstalker.OpCodes.MSG_READY;
import static si.vajnartech.moonstalker.OpCodes.MSG_WARNING;

import android.os.Handler;
import android.os.Message;

import si.vajnartech.moonstalker.telescope.Actions;

public class QueueUI extends Handler
{
    protected Actions actions;

    public QueueUI(Actions act)
    {
        super();
        this.actions = act;
    }

    @Override
    public void handleMessage(Message message)
    {
        if (message.what == MSG_CONNECT) {
            new CmdStatus(this);
            actions.updateStatus(ST_WAITING);
        } else if (message.what == MSG_BATTERY) {
            new CmdBattery(this);
            actions.updateStatus(ST_WAITING);
        } else if (message.what == MSG_PING) {
            new CmdPing(this);
            actions.updateStatus(ST_WAITING);
        } else if (message.what == MSG_MOVE) {
            Command obj = (Command) message.obj;
            new CmdMove(this, obj.ra, obj.dec);
            actions.updateStatus(ST_WAITING);

        } else if (message.what == MSG_GET_ASTRO_DATA) {
            actions.updateStatus(ST_ASTRO_DATA, message.obj);
        } else if (message.what == MSG_NOT_READY) {
            actions.updateStatus(ST_NOT_READY);
        } else if (message.what == MSG_MV_ACK) {
            actions.updateStatus(ST_MOVING);
        } else if (message.what == MSG_CONN_ERROR) {
            actions.updateStatus(ST_CONNECTION_ERROR);
        } else if (message.what == MSG_READY) {
            actions.updateStatus(ST_READY);
        } else if (message.what == MSG_ERROR) {
            String msg = (String) message.obj;
            actions.updateStatus(ST_ERROR, msg);
        } else if (message.what == MSG_WARNING) {
            String msg = (String) message.obj;
            actions.updateStatus(ST_WARNING, msg);
        } else if (message.what == MSG_INFO) {
            String msg = (String) message.obj;
            actions.updateStatus(ST_INFO, msg);
        } else if (message.what == MSG_BATTERY_RES) {
            String msg = (String) message.obj;
            actions.updateStatus(ST_BATTERY, msg);
        }
    }
}
