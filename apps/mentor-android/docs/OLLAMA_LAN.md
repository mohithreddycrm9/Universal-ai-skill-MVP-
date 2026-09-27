# Ollama on your LAN

Universal AI talks to Ollama over **HTTP** on your local network. Android blocks cleartext by default; this app allows it only for:

- `localhost` / `127.0.0.1`
- `10.0.2.2` (emulator → host machine)
- User-entered host in **Models → My Ollama** via a dedicated OkHttp client (`OllamaHttpClient`)

Use your machine’s LAN IP (for example `http://192.168.1.42:11434/`) in the Ollama profile base URL. Ensure Ollama listens on `0.0.0.0` if you need access from a physical phone on Wi‑Fi.

Do not expose Ollama to the public internet without TLS and authentication.
