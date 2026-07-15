"""Gunicorn import target."""

from .runtime import create_application


application = create_application()
