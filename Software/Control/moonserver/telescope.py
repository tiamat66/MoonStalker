import math
from datetime import datetime, timezone

CALIBRATOR = ("Polaris", 37.95456067, 89.26410897)  # RA, Dec in degrees

# Mechanical characteristics
MOTOR_STEPS_NUM = 200.0
REDUCTOR_TRANSLATION = 30.0
BELT_TRANSLATION = 48.0 / 14.0
K = MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION

class Telescope:
    def __init__(self):
        self.h_steps = 0
        self.v_steps = 0


def hms_to_degrees(hours: int, minutes: int, seconds: float) -> float:
    """Convert hour-minute-second RA to decimal degrees."""
    if not 0 <= hours < 24:
        raise ValueError("RA hours must be between 0 and 23")
    if not 0 <= minutes < 60:
        raise ValueError("RA minutes must be between 0 and 59")
    if not 0 <= seconds < 60:
        raise ValueError("RA seconds must be between 0 and 59")
    return hours * 15.0 + minutes * 0.25 + seconds / 240.0


def degrees_to_hms(degrees: float) -> tuple[int, int, float]:
    """Convert decimal degrees to hour-minute-second RA."""
    normalized = ((degrees % 360.0) + 360.0) % 360.0
    hours = int(normalized / 15.0)
    remaining = (normalized / 15.0 - hours) * 60.0
    minutes = int(remaining)
    seconds = (remaining - minutes) * 60.0
    return hours, minutes, seconds


def dms_to_degrees(degrees: int, minutes: int, seconds: float) -> float:
    """Convert degree-minute-second Dec to decimal degrees."""
    if not -90 <= degrees <= 90:
        raise ValueError("Declination degrees must be between -90 and 90")
    if not 0 <= minutes < 60:
        raise ValueError("Declination minutes must be between 0 and 59")
    if not 0 <= seconds < 60:
        raise ValueError("Declination seconds must be between 0 and 59")
    sign = -1 if degrees < 0 else 1
    abs_degrees = abs(degrees)
    return sign * (abs_degrees + minutes / 60.0 + seconds / 3600.0)


def degrees_to_dms(degrees: float) -> tuple[int, int, float]:
    """Convert decimal degrees to degree-minute-second Dec."""
    sign = -1 if degrees < 0 else 1
    abs_value = abs(degrees)
    whole_degrees = int(abs_value)
    remaining_minutes = (abs_value - whole_degrees) * 60.0
    minutes = int(remaining_minutes)
    seconds = (remaining_minutes - minutes) * 60.0
    return sign * whole_degrees, minutes, seconds


def convert_radec_to_az_el(
    ra_deg: float,
    dec_deg: float,
    latitude_deg: float,
    longitude_deg: float,
    utc_time: datetime | None = None,
) -> tuple[float, float]:
    """Convert right ascension/declination to azimuth and elevation.

    Parameters
    ----------
    ra_deg, dec_deg:
        Right ascension and declination of the target in degrees.
    latitude_deg, longitude_deg:
        Observer location in degrees.
    utc_time:
        UTC datetime for the observation. Defaults to the current UTC time.
    """
    if utc_time is None:
        utc_time = datetime.now(timezone.utc)

    if utc_time.tzinfo is None:
        utc_time = utc_time.replace(tzinfo=timezone.utc)

    # Convert to Julian date.
    year = utc_time.year
    month = utc_time.month
    day = utc_time.day
    hour = utc_time.hour + utc_time.minute / 60.0 + utc_time.second / 3600.0 + utc_time.microsecond / 3600000000.0

    if month <= 2:
        year -= 1

    a = year // 100
    b = 2 - a + a // 4

    jd = (
        math.floor(365.25 * (year + 4716))
        + math.floor(30.6001 * (month + 1))
        + day
        + b
        - 1524.5
        + hour / 24.0
    )

    # Greenwich mean sidereal time in degrees.
    t = (jd - 2451545.0) / 36525.0
    gmst_deg = (
        280.46061837
        + 360.98564736629 * (jd - 2451545.0)
        + 0.000387933 * t * t
        - (t**3) / 38710000.0
    ) % 360.0

    # Local sidereal time.
    lst_deg = (gmst_deg + longitude_deg) % 360.0

    # Hour angle.
    hour_angle_deg = lst_deg - ra_deg
    hour_angle_rad = math.radians(hour_angle_deg)

    lat_rad = math.radians(latitude_deg)
    dec_rad = math.radians(dec_deg)

    sin_alt = (
        math.sin(dec_rad) * math.sin(lat_rad)
        + math.cos(dec_rad) * math.cos(lat_rad) * math.cos(hour_angle_rad)
    )
    altitude_rad = math.asin(max(-1.0, min(1.0, sin_alt)))
    altitude_deg = math.degrees(altitude_rad)

    cos_az = (
        (math.sin(dec_rad) - math.sin(altitude_rad) * math.sin(lat_rad))
        / (math.cos(altitude_rad) * math.cos(lat_rad))
    )
    sin_az = -math.cos(dec_rad) * math.sin(hour_angle_rad) / math.cos(altitude_rad)

    azimuth_rad = math.atan2(sin_az, cos_az)
    azimuth_deg = (math.degrees(azimuth_rad) + 360.0) % 360.0

    return azimuth_deg, altitude_deg