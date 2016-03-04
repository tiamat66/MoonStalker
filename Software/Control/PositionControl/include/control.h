#ifndef CONTROL_H
#define CONTROL_H


#define LATITUDE  40.0 
#define LONGITUDE 44.0 

int vtsk_get_geo_coordinates(
      double *latitude, 
      double *longitude);


void vtsk_bt_send(char *message);
#endif


