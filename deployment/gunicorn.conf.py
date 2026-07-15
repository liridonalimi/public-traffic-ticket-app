"""Conservative Gunicorn defaults for the BusPay WSGI service."""

import os


bind = "0.0.0.0:8080"
worker_class = "gthread"
workers = int(os.environ.get("BUSPAY_WEB_WORKERS", "2"))
threads = int(os.environ.get("BUSPAY_WEB_THREADS", "4"))
timeout = 30
graceful_timeout = 30
keepalive = 5
max_requests = 2_000
max_requests_jitter = 200
limit_request_line = 4_096
limit_request_fields = 50
limit_request_field_size = 8_190
accesslog = "-"
errorlog = "-"
capture_output = True
