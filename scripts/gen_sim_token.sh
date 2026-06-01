#!/usr/bin/env python3
"""
Generates a Firebase custom auth token for the simulator test parent UID.
Run this when the token in AppState.swift expires (every ~1h during active testing).
Usage: python3 scripts/gen_sim_token.sh
"""
import json, time, base64, subprocess, tempfile, os, sys

SA_PATH = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")
APPSTATE = os.path.join(os.path.dirname(__file__), "../iosApp/iosApp/State/AppState.swift")
UID = "MEUUziPfpLRG0BSIA29Z4o0TUbK2"

with open(SA_PATH) as f:
    sa = json.load(f)

now = int(time.time())
header  = base64.urlsafe_b64encode(json.dumps({"alg":"RS256","typ":"JWT"}).encode()).rstrip(b'=').decode()
payload = base64.urlsafe_b64encode(json.dumps({
    "iss": sa["client_email"], "sub": sa["client_email"],
    "aud": "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit",
    "iat": now, "exp": now + 3600, "uid": UID
}).encode()).rstrip(b'=').decode()

with tempfile.NamedTemporaryFile(mode='w', suffix='.pem', delete=False) as f:
    f.write(sa["private_key"]); key_path = f.name

msg = f"{header}.{payload}"
result = subprocess.run(['openssl','dgst','-sha256','-sign',key_path], input=msg.encode(), capture_output=True)
os.unlink(key_path)
sig = base64.urlsafe_b64encode(result.stdout).rstrip(b'=').decode()
token = f"{msg}.{sig}"

# Patch AppState.swift in-place
with open(APPSTATE) as f:
    src = f.read()

import re
new_src = re.sub(
    r'(let customToken = ")([^"]+)(")',
    f'\\g<1>{token}\\g<3>',
    src
)
with open(APPSTATE, 'w') as f:
    f.write(new_src)

print(f"Token generated for UID={UID} (valid 1h)")
print("AppState.swift updated — rebuild the app.")
