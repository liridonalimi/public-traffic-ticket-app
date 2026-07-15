ARG PYTHON_IMAGE=python:3.12.13-slim-trixie
FROM ${PYTHON_IMAGE}

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/opt/buspay/server \
    BUSPAY_DB_PATH=/data/buspay.db \
    BUSPAY_SYNC_TOKEN_FILE=/run/secrets/buspay_sync_token

RUN groupadd --gid 10001 buspay \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin buspay \
    && mkdir -p /opt/buspay/deployment /data /backups \
    && chown -R 10001:10001 /opt/buspay /data /backups

WORKDIR /opt/buspay

COPY deployment/requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir --disable-pip-version-check -r /tmp/requirements.txt \
    && rm /tmp/requirements.txt

COPY --chown=10001:10001 server /opt/buspay/server
COPY --chown=10001:10001 deployment/gunicorn.conf.py /opt/buspay/deployment/gunicorn.conf.py
COPY --chown=10001:10001 deployment/backup_sqlite.py /opt/buspay/deployment/backup_sqlite.py
COPY --chown=10001:10001 deployment/restore_sqlite.py /opt/buspay/deployment/restore_sqlite.py

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD ["python", "-c", "import json,urllib.request; assert json.load(urllib.request.urlopen('http://127.0.0.1:8080/health', timeout=3))['status']=='ok'"]

CMD ["gunicorn", "--config", "/opt/buspay/deployment/gunicorn.conf.py", "buspay_server.wsgi:application"]
