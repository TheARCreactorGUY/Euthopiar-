#!/usr/bin/env python3
"""
Lightweight local network HTTP server for distributing Euthopiar stable release APK.
Serves release_dist on 0.0.0.0:8888 with Range request support for resumable APK downloads.
"""

import os
import sys
import mimetypes
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

DIST_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "release_dist"))

class ResumableReleaseHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIST_DIR, **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache, must-revalidate")
        super().end_headers()

    def guess_type(self, path):
        if path.endswith(".apk"):
            return "application/vnd.android.package-archive"
        if path.endswith(".png"):
            return "image/png"
        if path.endswith(".html"):
            return "text/html; charset=utf-8"
        return super().guess_type(path)

    def log_message(self, format, *args):
        sys.stdout.write(f"[{self.log_date_time_string()}] {self.client_address[0]} - {format % args}\n")
        sys.stdout.flush()

def run_server(port=8888):
    os.chdir(DIST_DIR)
    server_address = ("0.0.0.0", port)
    httpd = ThreadingHTTPServer(server_address, ResumableReleaseHandler)
    print(f"[*] Serving Euthopiar Release from {DIST_DIR}")
    print(f"[*] Local access: http://localhost:{port}/")
    print(f"[*] LAN access  : http://192.168.1.100:{port}/")
    print(f"[*] Direct APK  : http://192.168.1.100:{port}/euthopiar-v1.0.0-stable.apk")
    sys.stdout.flush()
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n[*] Server stopped.")
        httpd.server_close()

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8888
    run_server(port)
