# Variantes de build (Android)

Configuração ativa em [`androidApp/build.gradle.kts`](androidApp/build.gradle.kts).

## Product flavors (`environment`)

| Flavor | `WS_HOST` | `WS_PORT` | `WS_SCHEME` | `applicationId` |
|--------|-----------|-----------|-------------|-----------------|
| **dev** | `10.0.2.2` | `8080` | `ws` | `…transparencia.dev` |
| **staging** | `staging-api.caninde.ce.gov.br` | `443` | `wss` | `…transparencia.staging` |
| **prod** | `transparencia.caninde.ce.gov.br` | `443` | `wss` | `…transparencia` |

`WS_AUTH_TOKEN` fica vazio em dev; em staging/prod defina no Gradle se o servidor exigir `?token=` (ver `server/.env.example`).

## Comandos

```bash
cd kmp-app

# Desenvolvimento (emulador → host local)
./gradlew :androidApp:installDevDebug

# Staging / produção
./gradlew :androidApp:assembleStagingDebug
./gradlew :androidApp:assembleProdRelease
```

## Rede

- **dev:** cleartext permitido para `10.0.2.2` em [`network_security_config.xml`](androidApp/src/main/res/xml/network_security_config.xml).
- **staging/prod:** apenas `wss` (TLS).

O endpoint é injetado no Koin em `MainActivity` via `WebSocketEndpoint` (sem reflexão em `BuildConfig` no módulo shared).
