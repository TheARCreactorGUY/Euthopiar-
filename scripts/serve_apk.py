import os
import shutil
import hashlib
import socket
import io
import base64
import sys
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
import qrcode

PORT = 8080
DIST_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dist_apk")
RELEASE_APK = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "build", "outputs", "apk", "release", "app-release.apk"
)
DEBUG_APK = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "build", "outputs", "apk", "debug", "app-debug.apk"
)
ARTIFACT_DIR = r"C:\Users\sehga\.gemini\antigravity\brain\da5dfbf9-9a57-497c-bc04-1849244511a1"
RELEASE_DIST_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "release_dist")

def get_lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "192.168.1.100"

def calculate_sha256(filepath):
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()

def setup_distribution():
    os.makedirs(DIST_DIR, exist_ok=True)
    os.makedirs(RELEASE_DIST_DIR, exist_ok=True)
    
    # Prioritize Release APK if it exists
    if os.path.exists(RELEASE_APK):
        source_apk = RELEASE_APK
        is_release = True
        version_label = "v1.0.0 Stable (Release)"
    elif os.path.exists(DEBUG_APK):
        source_apk = DEBUG_APK
        is_release = False
        version_label = "v1.0.0 Debug"
    else:
        raise FileNotFoundError(f"Neither Release APK ({RELEASE_APK}) nor Debug APK ({DEBUG_APK}) was found!")

    target_stable = os.path.join(DIST_DIR, "euthopiar-stable.apk")
    target_main = os.path.join(DIST_DIR, "euthopiar.apk")
    target_v1 = os.path.join(DIST_DIR, "euthopiar-v1.0.0-stable.apk")
    target_rel_v1 = os.path.join(RELEASE_DIST_DIR, "euthopiar-v1.0.0-stable.apk")

    shutil.copy2(source_apk, target_stable)
    shutil.copy2(source_apk, target_main)
    shutil.copy2(source_apk, target_v1)
    shutil.copy2(source_apk, target_rel_v1)

    if is_release:
        shutil.copy2(source_apk, os.path.join(DIST_DIR, "app-release.apk"))
        for dbg_name in ["euthopiar-debug.apk", "app-debug.apk"]:
            dbg_path = os.path.join(DIST_DIR, dbg_name)
            if os.path.exists(dbg_path):
                try:
                    os.remove(dbg_path)
                except Exception:
                    pass
    elif os.path.exists(DEBUG_APK):
        shutil.copy2(DEBUG_APK, os.path.join(DIST_DIR, "euthopiar-debug.apk"))
        shutil.copy2(DEBUG_APK, os.path.join(DIST_DIR, "app-debug.apk"))

    apk_size_bytes = os.path.getsize(target_stable)
    apk_size_mb = apk_size_bytes / (1024 * 1024)
    sha256 = calculate_sha256(target_stable)
    lan_ip = get_lan_ip()
    primary_apk_filename = "euthopiar-stable.apk" if is_release else "euthopiar-debug.apk"
    download_url = f"http://{lan_ip}:{PORT}/{primary_apk_filename}"
    landing_url = f"http://{lan_ip}:{PORT}/"

    # Generate QR Code pointing directly to download
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=8,
        border=2,
    )
    qr.add_data(download_url)
    qr.make(fit=True)
    qr_img = qr.make_image(fill_color="#0F1016", back_color="#FFFFFF")
    
    # Save physical PNG file
    qr_img_path = os.path.join(DIST_DIR, "qr-download.png")
    qr_img.save(qr_img_path)

    # Save to artifact directory as well
    if os.path.exists(ARTIFACT_DIR):
        try:
            shutil.copy2(qr_img_path, os.path.join(ARTIFACT_DIR, "qr-download.png"))
        except Exception as e:
            print(f"Notice: Failed to copy QR to artifact directory: {e}")

    # Encode as Base64 Data URI so it renders instantly with zero HTTP roundtrips
    buf = io.BytesIO()
    qr_img.save(buf, format="PNG")
    qr_b64 = base64.b64encode(buf.getvalue()).decode("ascii")
    qr_data_uri = f"data:image/png;base64,{qr_b64}"

    # Self-contained, robust HTML page
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0">
    <meta name="theme-color" content="#0d0f17">
    <title>Euthopiar {version_label} Download</title>
    <style>
        body {{
            background-color: #0d0f17;
            color: #f8fafc;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            margin: 0;
            padding: 16px;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            box-sizing: border-box;
        }}
        .card {{
            background-color: #161a29;
            border: 1px solid #28304a;
            border-radius: 16px;
            max-width: 440px;
            width: 100%;
            padding: 24px;
            box-shadow: 0 12px 36px rgba(0, 0, 0, 0.5);
            text-align: center;
            box-sizing: border-box;
        }}
        .pill {{
            display: inline-block;
            background: #1e293b;
            color: #38bdf8;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.8px;
            text-transform: uppercase;
            padding: 4px 12px;
            border-radius: 999px;
            margin-bottom: 12px;
            border: 1px solid #334155;
        }}
        h1 {{
            font-size: 24px;
            margin: 0 0 6px 0;
            font-weight: 700;
            color: #ffffff;
        }}
        p.subtitle {{
            font-size: 13px;
            color: #94a3b8;
            margin: 0 0 20px 0;
            line-height: 1.4;
        }}
        .qr-wrapper {{
            background: #ffffff;
            padding: 10px;
            border-radius: 12px;
            display: inline-block;
            margin-bottom: 16px;
        }}
        .qr-wrapper img {{
            width: 170px;
            height: 170px;
            display: block;
        }}
        .btn {{
            display: block;
            background: #4f46e5;
            color: #ffffff !important;
            font-size: 16px;
            font-weight: 600;
            text-decoration: none;
            padding: 14px 20px;
            border-radius: 10px;
            margin: 16px 0 12px 0;
            box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
            transition: background 0.2s;
        }}
        .btn:hover, .btn:active {{
            background: #4338ca;
        }}
        .meta {{
            background: #0f1320;
            border-radius: 10px;
            padding: 14px;
            margin-top: 16px;
            text-align: left;
            font-size: 12px;
            border: 1px solid #1f253a;
        }}
        .meta-row {{
            display: flex;
            justify-content: space-between;
            padding: 4px 0;
        }}
        .meta-k {{
            color: #94a3b8;
        }}
        .meta-v {{
            color: #e2e8f0;
            font-weight: 500;
            font-family: monospace;
        }}
        .meta-v a {{
            color: #818cf8;
            text-decoration: none;
        }}
        .sha {{
            font-size: 10px;
            color: #64748b;
            word-break: break-all;
            margin-top: 8px;
            font-family: monospace;
        }}
        .steps {{
            margin-top: 14px;
            text-align: left;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.5;
            padding: 0 4px;
        }}
        .steps strong {{
            color: #cbd5e1;
        }}
    </style>
</head>
<body>
    <div class="card">
        <div class="pill">{version_label}</div>
        <h1>Euthopiar</h1>
        <p class="subtitle">Universal Streaming, Multi-Audio Engine, Movy Stream Cipher & Native Decryptors</p>

        <div class="qr-wrapper">
            <img src="{qr_data_uri}" alt="Scan to download" width="170" height="170">
        </div>

        <a href="/{primary_apk_filename}" download="{primary_apk_filename}" class="btn">
            ⬇️ Download Stable APK ({apk_size_mb:.1f} MB)
        </a>

        <div class="meta">
            <div class="meta-row">
                <span class="meta-k">Package:</span>
                <span class="meta-v">com.euthopiar</span>
            </div>
            <div class="meta-row">
                <span class="meta-k">Version:</span>
                <span class="meta-v">1.0.0-stable</span>
            </div>
            <div class="meta-row">
                <span class="meta-k">Size:</span>
                <span class="meta-v">{apk_size_mb:.2f} MB</span>
            </div>
            <div class="meta-row">
                <span class="meta-k">Server:</span>
                <span class="meta-v">{lan_ip}:{PORT}</span>
            </div>
            <div class="meta-row">
                <span class="meta-k">Direct Link:</span>
                <span class="meta-v"><a href="/{primary_apk_filename}">{primary_apk_filename}</a></span>
            </div>
            <div class="sha">SHA256: {sha256[:32]}...</div>
        </div>

        <div class="steps">
            <strong>Install Guide:</strong><br>
            1. Tap <strong>Download Stable APK</strong> above.<br>
            2. If Android warns <em>"File might be harmful"</em>, tap <strong>Download anyway</strong>.<br>
            3. Tap the downloaded notification to install.
        </div>
    </div>
</body>
</html>
"""
    with open(os.path.join(DIST_DIR, "index.html"), "w", encoding="utf-8") as f:
        f.write(html_content)

    return lan_ip, download_url, landing_url, apk_size_mb, qr, primary_apk_filename

class ApkRequestHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIST_DIR, **kwargs)

    def guess_type(self, path):
        if str(path).lower().endswith(".apk"):
            return "application/vnd.android.package-archive"
        return super().guess_type(path)

    def end_headers(self):
        # Add CORS and attachment headers
        self.send_header("Access-Control-Allow-Origin", "*")
        if str(self.path).lower().endswith(".apk"):
            fname = os.path.basename(self.path.split('?')[0])
            if not fname:
                fname = "euthopiar-stable.apk"
            self.send_header("Content-Disposition", f'attachment; filename="{fname}"')
        super().end_headers()

    def copyfile(self, source, outputfile):
        try:
            shutil.copyfileobj(source, outputfile)
        except (ConnectionResetError, ConnectionAbortedError, BrokenPipeError):
            pass

    def log_message(self, format, *args):
        # Format clean access logs to stdout with boundary protection
        try:
            if len(args) >= 3:
                sys.stdout.write(f"[{self.log_date_time_string()}] {args[0]} - {args[1]} ({args[2]})\n")
            else:
                sys.stdout.write(f"[{self.log_date_time_string()}] {format % args}\n")
        except Exception:
            sys.stdout.write(f"[{self.log_date_time_string()}] HTTP Request\n")
        sys.stdout.flush()

class ReusableThreadingHTTPServer(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True

    def handle_error(self, request, client_address):
        exc = sys.exc_info()[1]
        if isinstance(exc, (ConnectionResetError, ConnectionAbortedError, BrokenPipeError)):
            return
        super().handle_error(request, client_address)

def run_server():
    lan_ip, download_url, landing_url, apk_size_mb, qr, primary_apk_filename = setup_distribution()
    server_address = ("0.0.0.0", PORT)
    httpd = ReusableThreadingHTTPServer(server_address, ApkRequestHandler)

    print("=" * 60)
    print("  EUTHOPIAR LOCAL NETWORK APK SERVER RUNNING (STABLE RELEASE)")
    print("=" * 60)
    print(f"  Landing Page URL : {landing_url}")
    print(f"  Direct APK Link  : {download_url}")
    print(f"  APK File Size    : {apk_size_mb:.2f} MB")
    print(f"  Serving Directory: {DIST_DIR}")
    print("=" * 60)
    print("  SCAN THIS QR CODE FROM YOUR PHONE:")
    try:
        if hasattr(sys.stdout, 'reconfigure'):
            sys.stdout.reconfigure(encoding='utf-8')
        qr.print_ascii(invert=True)
    except Exception:
        print("  (Open the Landing Page URL in your browser to view the high-res QR code)")
    print("=" * 60)
    print(f"Listening on 0.0.0.0:{PORT}...")
    sys.stdout.flush()

    while True:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server.")
            httpd.server_close()
            break
        except Exception as e:
            print(f"Server caught transient exception: {e}, continuing...")

if __name__ == "__main__":
    run_server()
