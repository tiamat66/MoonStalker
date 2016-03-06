#ifndef MESSAGES_H
#define MESSAGES_H

#define VTSK_MAX_MSG_LEN 100
#define VTSK_POLL_TIME 1000000

#define VTSK_MSG_MV "<MV %d,%d>" 
#define VTSK_MSG_ST "<ST?>" 
#define VTSK_MSG_RDY "<RDY>"
#define VTSK_MSG_NOT_RDY "<NOT_RDY>"

void wait_until_msg_received(char *msg);
void vtsk_mv(int horiz, int vert);
void vtsk_st();

/* DEBUG */
typedef enum e_debug_msgs
{
   DBG_MSG_RDY = 1,
   DBG_MSG_NOT_RDY,
}E_DEBUG_MSGS;
#endif
