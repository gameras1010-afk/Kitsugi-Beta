import subprocess
import json

payload = {"query": "{ Page { media(type: ANIME) { id } } }"}
payload_str = json.dumps(payload)

# Run curl
cmd = [
    "curl", "-i", "-X", "POST",
    "-H", "Content-Type: application/json",
    "-H", "Accept: application/json",
    "-d", payload_str,
    "https://graphql.anilist.co"
]

res = subprocess.run(cmd, capture_output=True, text=True)
print("STDOUT:")
print(res.stdout)
print("STDERR:")
print(res.stderr)
