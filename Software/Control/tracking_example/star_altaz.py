#!/usr/bin/env python3
"""Calculate current alt/az coordinates of a star from Jesenice, Slovenia."""

import sys
from datetime import datetime, timezone
import time

from skyfield.api import Star, wgs84, load

GEAR_RATIO = 120.0
MOTOR_STEPS_PER_REV = 200.0
MICROSTEPS = 16.0


MICROSTEP_ANGLE_DEGREES = 360.0 / (GEAR_RATIO * MOTOR_STEPS_PER_REV * MICROSTEPS)

Jesenice = wgs84.latlon(46.4367, 14.0519, elevation_m=585.0)

KNOWN_STARS = {
    "sirius": Star(ra_hours=(6, 45, 8.917), dec_degrees=(-16, 42, 58.017), names=["Sirius"]),
    "canopus": Star(ra_hours=(6, 23, 57.109), dec_degrees=(-52, 41, 44.62), names=["Canopus"]),
    "arcturus": Star(ra_hours=(14, 15, 39.672), dec_degrees=(19, 10, 56.67), names=["Arcturus"]),
    "vega": Star(ra_hours=(18, 36, 56.336), dec_degrees=(38, 47, 1.251), names=["Vega"]),
    "capella": Star(ra_hours=(5, 16, 41.359), dec_degrees=(45, 59, 52.77), names=["Capella"]),
    "rigel": Star(ra_hours=(5, 14, 32.277), dec_degrees=(-8, 12, 5.90), names=["Rigel"]),
    "procyon": Star(ra_hours=(7, 39, 18.107), dec_degrees=(5, 13, 29.96), names=["Procyon"]),
    "betelgeuse": Star(ra_hours=(5, 55, 10.305), dec_degrees=(7, 24, 25.43), names=["Betelgeuse"]),
    "altair": Star(ra_hours=(19, 50, 46.999), dec_degrees=(8, 52, 5.954), names=["Altair"]),
    "aldebaran": Star(ra_hours=(4, 35, 55.239), dec_degrees=(16, 30, 33.49), names=["Aldebaran"]),
    "spica": Star(ra_hours=(13, 25, 11.579), dec_degrees=(-11, 9, 40.75), names=["Spica"]),
    "antares": Star(ra_hours=(16, 29, 24.460), dec_degrees=(-26, 25, 55.21), names=["Antares"]),
    "pollux": Star(ra_hours=(7, 45, 18.920), dec_degrees=(28, 1, 34.32), names=["Pollux"]),
    "fomalhaut": Star(ra_hours=(22, 57, 39.046), dec_degrees=(-29, 37, 20.05), names=["Fomalhaut"]),
    "deneb": Star(ra_hours=(20, 41, 25.915), dec_degrees=(45, 16, 49.22), names=["Deneb"]),
    "regulus": Star(ra_hours=(10, 8, 22.311), dec_degrees=(11, 58, 1.95), names=["Regulus"]),
    "polaris": Star(ra_hours=(2, 31, 49.095), dec_degrees=(89, 15, 50.80), names=["Polaris"]),
    "achernar": Star(ra_hours=(3, 4, 19.952), dec_degrees=(-53, 43, 40.83), names=["Achernar"]),
    "castor": Star(ra_hours=(7, 34, 35.872), dec_degrees=(31, 53, 18.58), names=["Castor"]),
    "bellatrix": Star(ra_hours=(5, 25, 7.863), dec_degrees=(6, 20, 58.93), names=["Bellatrix"]),
    "mirfak": Star(ra_hours=(3, 24, 19.374), dec_degrees=(49, 51, 40.25), names=["Mirfak"]),
    "wezen": Star(ra_hours=(7, 8, 23.491), dec_degrees=(-26, 23, 35.55), names=["Wezen"]),
    "adhara": Star(ra_hours=(6, 58, 37.549), dec_degrees=(-28, 58, 19.43), names=["Adhara"]),
    "alnitak": Star(ra_hours=(5, 40, 45.527), dec_degrees=(-1, 56, 34.27), names=["Alnitak"]),
    "alnilam": Star(ra_hours=(5, 36, 12.814), dec_degrees=(-1, 12, 7.00), names=["Alnilam"]),
    "mintaka": Star(ra_hours=(5, 32, 0.401), dec_degrees=(-0, 17, 56.74), names=["Mintaka"]),
}

def calculate_alt_az(star, ts, eph) -> tuple:
    """Calculate the current altitude and azimuth of a star from Jesenice, Slovenia.
    Returns:
        tuple: Current altitude and azimuth of the star.
    """
    now_utc = datetime.now(timezone.utc)
    t = ts.utc(now_utc.year, now_utc.month, now_utc.day,
               now_utc.hour, now_utc.minute, now_utc.second)

    earth = eph['earth'] + Jesenice
    alt, az, _ = earth.at(t).observe(star).apparent().altaz()

    print("-" * 80)
    print(f"Star:              {star.names[0]}")
    print(f"Location:          Jesenice, Slovenia (46.4367 N, 14.0519 E, 585m)")
    print(f"Time (UTC):        {now_utc.strftime('%Y-%m-%d %H:%M:%S UTC')}")
    print(f"Altitude:          {alt.degrees} deg")
    print(f"Azimuth:           {az.degrees} deg (0=N, 90=E, 180=S, 270=W)")
    print("-" * 80)

    return alt.degrees, az.degrees

def generate_move_command(current_alt, current_az, target_alt, target_az):
    """Generate a command to move the telescope from current alt/az to target alt/az.
    Args:
        current_alt (float): Current altitude in degrees.
        current_az (float): Current azimuth in degrees.
        target_alt (float): Target altitude in degrees.
        target_az (float): Target azimuth in degrees.
    Returns:
        str: Command string to move the telescope.
    """
    delta_alt = target_alt - current_alt
    delta_az = target_az - current_az

    steps_alt = int(delta_alt / MICROSTEP_ANGLE_DEGREES)
    steps_az = int(delta_az / MICROSTEP_ANGLE_DEGREES)

    command = f"<MV {steps_az} {steps_alt} SPEED>"
    return command

def main():
    if len(sys.argv) < 2:
        print("Usage: python star_altaz.py <star_name>")
        print(f"\nAvailable stars: {', '.join(sorted(KNOWN_STARS.keys()))}")
        sys.exit(1)

    star_input = sys.argv[1].lower()
    if star_input not in KNOWN_STARS:
        print(f"Error: Unknown star '{sys.argv[1]}'")
        print(f"Available stars: {', '.join(sorted(KNOWN_STARS.keys()))}")
        sys.exit(1)

    ts = load.timescale()
    eph = load('de421.bsp')

    star = KNOWN_STARS[star_input]

    print(f"Microstep angle: {MICROSTEP_ANGLE_DEGREES} degrees per microstep")

    alt, az = calculate_alt_az(star, ts, eph)
    print(f"Initial Altitude: {alt} deg, Azimuth: {az} deg")
    time.sleep(1)

    while True:
        next_alt, next_az = calculate_alt_az(star, ts, eph)
        cmd = generate_move_command(alt, az, next_alt, next_az)
        print(f"Move Command: {cmd}")
        alt, az = next_alt, next_az
        time.sleep(1)  # Update every second



if __name__ == "__main__":
    main()
