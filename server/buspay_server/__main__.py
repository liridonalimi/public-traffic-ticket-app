"""Development entry point for the BusPay synchronization service."""

import os
from pathlib import Path
import ssl
from wsgiref.simple_server import make_server

from .runtime import create_application


def main() -> None:
    host = os.environ.get("BUSPAY_HOST", "127.0.0.1")
    port = int(os.environ.get("BUSPAY_PORT", "8080"))
    os.environ.setdefault(
        "BUSPAY_DB_PATH",
        str(Path(__file__).resolve().parents[1] / "data" / "buspay.db"),
    )
    certificate_path = os.environ.get("BUSPAY_TLS_CERT")
    private_key_path = os.environ.get("BUSPAY_TLS_KEY")
    if bool(certificate_path) != bool(private_key_path):
        raise SystemExit("BUSPAY_TLS_CERT and BUSPAY_TLS_KEY must be supplied together")

    application = create_application()
    server = make_server(host, port, application)
    scheme = "http"
    if certificate_path and private_key_path:
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(certificate_path, private_key_path)
        server.socket = context.wrap_socket(server.socket, server_side=True)
        scheme = "https"

    print(f"BusPay development server listening on {scheme}://{host}:{port}")
    print("Use a production WSGI server and managed TLS before external deployment.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBusPay development server stopped.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
