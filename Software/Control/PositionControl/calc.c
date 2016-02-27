#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"

t_telescope_coordinates cur_tel_pos;
t_astronomical_coordinates cur_ast_coordinates;
double cur_time;

int vtsk_astronomical_to_sferical(
        t_astronomical_coordinates *in,
        t_sferical_coordinates     *out)
{
    double rec = in->rec;
    double dec = in->dec;
    
    dec = 90 - dec;
    
    DEG_TO_RAD(out->d, dec);
    DEG_TO_RAD(out->fi, (360 - (rec * 15.0)));
    DEG_TO_RAD(out->t, in->latitude); 

    return(0);
}

int vtsk_astronomical_to_telescope(
        t_astronomical_coordinates *in,
        t_telescope_coordinates    *out)
{
    double tmp1, tmp2, tmp3;
    double a, b, c;
    double azimuth;
    t_sferical_coordinates sfr;

    vtsk_astronomical_to_sferical(in, &sfr);

    /* alpha-> AZIMUTH */
    tmp1 = a = cos(sfr.d)*cos(sfr.fi);
    tmp2 = cos(sfr.t)*sin(sfr.d);
    tmp3 = sin(sfr.t)*cos(sfr.d)*sin(sfr.fi);
    tmp2 = b = tmp2 - tmp3;
    printf("B=%lf\n", b);
#if 0
    if((0.0 == tmp2) && (tmp1 >= 0.0))
        out->azimuth = PI/2.0;
    else if((0.0 == tmp2) && (tmp1 < 0.0))
        out->azimuth = -PI/2.0;
    else if(0.0 != tmp2)
#endif
        out->azimuth = atan(tmp1/tmp2);

    /* omega -> VISINA */
    tmp1 = sin(sfr.t)*sin(sfr.d);
    tmp2 = cos(sfr.t)*cos(sfr.d)*sin(sfr.fi);
    out->height = asin(tmp1 + tmp2); 
#if 0    
    a = sin(sfr.d)*cos(sfr.fi);
    b = cos(sfr.t)*cos(sfr.d) - sin(sfr.t)*sin(sfr.d)*sin(sfr.fi);
    c = sin(sfr.t)*cos(sfr.d) + cos(sfr.t)*sin(sfr.d)*sin(sfr.fi); 
#else
    //a = sin(sfr.d)*cos(sfr.fi);
    //b = sin(sfr.t)*cos(sfr.d) - cos(sfr.t)*sin(sfr.d)*sin(sfr.fi);
    c = cos(sfr.t)*cos(sfr.d) + sin(sfr.t)*sin(sfr.d)*sin(sfr.fi); 
#endif

    /* Robni Pogoji */
    RAD_TO_DEG(out->azimuth, out->azimuth);
    RAD_TO_DEG(out->height,  out->height);

    azimuth = out->azimuth;
    //printf("A=%lf B=%lf\n C=%lf\n", a, b, c);
    printf("C=%lf\n", c);
    printf("AZIMUTH=%lf\n", out->azimuth);

    if((c < 0.0) && (b > 0.0))
        azimuth = 180.0 - out->azimuth;
    else if((c > 0.0) && (b < 0.0))
        azimuth = 0.0 -out->azimuth;
#if 0
    else
        azimuth = 360.0 - out->azimuth;

    if((c == 0.0) && (out->azimuth == -90.0))
    {
        printf("Klobasa\n");
        azimuth = 270.0;
    }

    //else
    //else if((in->rec > 12) && (in->rec <= 24))
    //    out->azimuth = 360 - out->azimuth;
    
    //if(out->azimuth >= 360.0) out->azimuth -= 360;
#endif
    out->azimuth = azimuth;

    return(0);
}

double vtsk_get_time()
{
    time_t timer;
    struct tm y2k;
    double seconds;

    y2k.tm_hour = 0;   y2k.tm_min = 0; y2k.tm_sec = 0;
    y2k.tm_year = 100; y2k.tm_mon = 0; y2k.tm_mday = 1;
    time(&timer);  /* get current time; same as: timer = time(NULL)  */
    seconds = difftime(timer,mktime(&y2k));

    return seconds;
}

int vtsk_calibration()
{
    printf(".....Calibration on 0, 0, 90\n");

    /* Za primer Severnica!!!! TODO */
    cur_ast_coordinates.rec =       0.0;
    cur_ast_coordinates.dec =       0.0;
    cur_ast_coordinates.latitude =  LAT;
    vtsk_astronomical_to_telescope(&cur_ast_coordinates, &cur_tel_pos);

    return(0);
}

int vtsk_move(t_astronomical_coordinates *new_pos)
{
    t_telescope_coordinates    tel;
    double dif_az;
    double dif_hi;

    vtsk_astronomical_to_telescope(new_pos, &tel);
    printf("azimuth=%lf visina=%lf ", 
            tel.azimuth, tel.height);

    dif_az    = tel.azimuth  - cur_tel_pos.azimuth;
    dif_hi    = tel.height   - cur_tel_pos.height;
    printf("dif_az=%lf dif_heigh=%lf\n", dif_az, dif_hi);
    cur_ast_coordinates.rec = new_pos->rec;
    cur_ast_coordinates.dec = new_pos->dec;
    cur_ast_coordinates.latitude = new_pos->latitude;
    cur_tel_pos.azimuth = tel.azimuth;
    cur_tel_pos.height = tel.height;
    
    return(0); 
}
#define VTSK_FOLLOW_TIME 1
void vtsk_follow()
{
    double seconds = 0;
    t_astronomical_coordinates ast;

    ast.rec = cur_ast_coordinates.rec;
    ast.dec = cur_ast_coordinates.dec;
    ast.latitude = cur_ast_coordinates.latitude;
    do
    {
        ast.rec = ast.rec -= SEC_TO_HOUR;
        if(ast.rec < 0.0)   ast.rec = 24.0 + ast.rec;
        if(ast.rec >= 24.0) ast.rec = ast.rec - 24.0;
        printf("\t%d:: rec=%lf\n", (int)seconds, ast.rec);
        vtsk_move(&ast);
        sleep(VTSK_FOLLOW_TIME);
        seconds++;
    } 
    while (1);
}


/* TODO: This is a reference function of low level priority */
void vtsk_draw()
{
    /* NOTE: priblizno to deluje le za cirkumpolarno polje */
    initscr();          /* Start curses mode          */
    printw("Hello World !!!");  /* Print Hello World          */
    refresh();          /* Print it on to the real screen */
    getch();            /* Wait for user input */
    endwin();           /* End curses mode        */
}

int main(int argc, char **argv)
{
    t_astronomical_coordinates ast;

    vtsk_calibration();

    sleep(3);

    ast.rec =       REC;
    ast.dec =       DEC;
    ast.latitude =  LAT;
    vtsk_move(&ast);
    sleep(1);

    printf("Ajmo follow......\n\n");
    vtsk_follow();

    return(0);
}

