# Ollama on your LAN

Universal AI talks to Ollama over **HTTP** on your local network. Android blocks cleartext by default; this app allows it only for:

- `localhost` / `127.0.0.1`
- `10.0.2.2` (Android emulator → your PC; **not** available on a physical phone)
- User-entered host in **Models → My Ollama** via a dedicated OkHttp client (`OllamaHttpClient`)

On a **real phone**, open **Models → My Ollama** and set the base URL to your computer’s Wi‑Fi IP, for example `http://192.168.1.42:11434/` (find it with `ip addr` / `ipconfig`). Ensure Ollama listens on `0.0.0.0` if you need access from a physical phone on Wi‑Fi.

Do not expose Ollama to the public internet without TLS and authentication.
