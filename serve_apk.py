import os
import re
import sys
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler

DIRECTORY = r"C:\Euthopiar\app\build\outputs\apk\release"

class MultiThreadedApkHandler(SimpleHTTPRequestHandler):
    extensions_map = SimpleHTTPRequestHandler.extensions_map.copy()
    extensions_map['.apk'] = 'application/vnd.android.package-archive'

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def do_GET(self):
        if self.path == '/favicon.ico':
            self.send_response(204)
            self.end_headers()
            return
        try:
            super().do_GET()
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
            pass

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Accept-Ranges", "bytes")
        super().end_headers()

    def send_head(self):
        path = self.translate_path(self.path)
        if os.path.isdir(path):
            for index in ("index.html", "index.htm"):
                index_path = os.path.join(path, index)
                if os.path.exists(index_path):
                    path = index_path
                    break
            else:
                return super().send_head()

        ctype = self.guess_type(path)
        if path.endswith('.apk') and not os.path.exists(path):
            release_apk = os.path.join(DIRECTORY, 'app-release.apk')
            if os.path.exists(release_apk):
                path = release_apk

        try:
            f = open(path, 'rb')
        except OSError:
            self.send_error(404, "File not found")
            return None

        fs = os.fstat(f.fileno())
        total_size = fs.st_size

        is_apk = path.endswith('.apk')

        range_header = self.headers.get('Range')
        if range_header:
            m = re.match(r'bytes=(\d+)-(\d*)', range_header)
            if m:
                start = int(m.group(1))
                end = int(m.group(2)) if m.group(2) else total_size - 1
                if start < total_size:
                    end = min(end, total_size - 1)
                    content_length = end - start + 1
                    self.send_response(206, "Partial Content")
                    self.send_header("Content-Type", ctype)
                    if is_apk:
                        self.send_header("Content-Disposition", 'attachment; filename="Euthopiar-release.apk"')
                    self.send_header("Content-Range", f"bytes {start}-{end}/{total_size}")
                    self.send_header("Content-Length", str(content_length))
                    self.send_header("Last-Modified", self.date_time_string(int(fs.st_mtime)))
                    self.end_headers()
                    f.seek(start)
                    self.range_end = end
                    return f

        self.send_response(200)
        self.send_header("Content-Type", ctype)
        if is_apk:
            self.send_header("Content-Disposition", 'attachment; filename="Euthopiar-release.apk"')
        self.send_header("Content-Length", str(total_size))
        self.send_header("Last-Modified", self.date_time_string(int(fs.st_mtime)))
        self.end_headers()
        self.range_end = total_size - 1
        return f

    def copyfile(self, source, outputfile):
        buffer_size = 256 * 1024
        try:
            if hasattr(self, 'range_end') and self.range_end is not None:
                remaining = self.range_end - source.tell() + 1
                while remaining > 0:
                    chunk_size = min(buffer_size, remaining)
                    data = source.read(chunk_size)
                    if not data:
                        break
                    outputfile.write(data)
                    remaining -= len(data)
            else:
                while True:
                    data = source.read(buffer_size)
                    if not data:
                        break
                    outputfile.write(data)
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
            pass

    def log_message(self, format, *args):
        sys.stderr.write(f"[{self.log_date_time_string()}] {self.address_string()} - {format % args}\n")
        sys.stderr.flush()

if __name__ == '__main__':
    port = 8080
    server_address = ('0.0.0.0', port)
    httpd = ThreadingHTTPServer(server_address, MultiThreadedApkHandler)
    httpd.daemon_threads = True
    print(f"Serving APK on http://0.0.0.0:{port} (Multi-Threaded) from {DIRECTORY}")
    sys.stdout.flush()
    while True:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            httpd.server_close()
            break
        except Exception as e:
            sys.stderr.write(f"Server error: {e}\n")
            sys.stderr.flush()
