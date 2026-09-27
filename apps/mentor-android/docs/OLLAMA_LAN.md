# Ollama on your LAN

Lumina talks to Ollama over **HTTP** on your local network. Android blocks cleartext by default; this app allows it only for:

- `localhost` / `127.0.0.1`
- `10.0.2.2` (Android emulator → your PC; **not** available on a physical phone)
- A private IPv4 address (10.x, 172.16–31.x, 192.168.x) that **you entered** in **Models → My Ollama**

How the last one works without turning cleartext on app-wide: the Network Security Config keeps
`cleartextTrafficPermitted="false"` and only permits the reserved placeholder zone `ollama-lan.invalid`.
The Ollama OkHttp client (`OllamaHttpClient` + `LanCleartextBridge`) rewrites
`http://192.168.1.42:11434` to `http://192-168-1-42.ollama-lan.invalid:11434`, sends
`Host: 192.168.1.42:11434`, and resolves the placeholder back to the IP only if that IP came from your
Ollama settings. Other HTTP clients in the app use system DNS, where `.invalid` never resolves, and
public `http://` URLs are refused everywhere (also rejected when saving settings).

On a **real phone**, open **Models → My Ollama** and set the base URL to your computer’s Wi‑Fi IP, for example `http://192.168.1.42:11434/` (find it with `ip addr` / `ipconfig`). Ensure Ollama listens on `0.0.0.0` if you need access from a physical phone on Wi‑Fi.

Do not expose Ollama to the public internet without TLS and authentication.
