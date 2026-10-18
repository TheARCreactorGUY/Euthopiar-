import socket
import ssl

s = socket.socket()
s.settimeout(5)
try:
    s.connect(('94.75.220.6', 443))
    print("TCP Connect to Eporner IP: SUCCESS")
    ctx = ssl.create_default_context()
    ss = ctx.wrap_socket(s, server_hostname='www.eporner.com')
    print("TLS Handshake to Eporner: SUCCESS")
except Exception as e:
    print("Eporner error:", e)

s2 = socket.socket()
s2.settimeout(5)
try:
    s2.connect(('89.222.127.6', 443))
    print("TCP Connect to Xvideos IP: SUCCESS")
    ctx = ssl.create_default_context()
    ss2 = ctx.wrap_socket(s2, server_hostname='www.xvideos.com')
    print("TLS Handshake to Xvideos: SUCCESS")
except Exception as e:
    print("Xvideos error:", e)
