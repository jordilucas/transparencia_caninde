# Hospedagem gratuita do servidor WebSocket

Guia para publicar o `server/` (Node.js + WebSocket) **sem custo**, usando o repositório GitHub já existente.

## Recomendação: Render (mais simples)

| Item | Detalhe |
|------|---------|
| Plano | **Free** Web Service |
| URL | `https://transparencia-caninde.onrender.com` (se o nome do serviço for `transparencia-caninde`) |
| WebSocket | `wss://transparencia-caninde.onrender.com` (porta 443, sem `:8080` na URL) |
| Limite | Serviço **dorme** após ~15 min sem tráfego; primeiro acesso pode levar ~1 min (cold start) |

### Passo a passo (Render)

1. Crie conta em [render.com](https://render.com) e conecte o GitHub.
2. **New → Blueprint** (ou **New → Web Service**).
3. Selecione o repositório `jordilucas/transparencia_caninde`.
4. O arquivo [`render.yaml`](../render.yaml) na raiz já define o serviço:
   - `rootDir: server`
   - `healthCheckPath: /health`
   - `plan: free`
5. Clique em **Apply** / **Create Web Service** e aguarde o deploy (build + start).
6. Quando ficar **Live**, teste no navegador:
   - `https://SEU-SERVICO.onrender.com/health` → JSON `{ "ok": true, ... }`
7. **WebSocket** (terminal com [wscat](https://www.npmjs.com/package/wscat) ou similar):
   ```bash
   wscat -c "wss://transparencia-caninde.onrender.com"
   ```
   Envie: `{"type":"REQUEST_PREFEITURA"}`

### Manter o serviço acordado (opcional)

No plano gratuito o processo para quando não há requisições. Para reduzir cold start:

1. Crie conta gratuita em [UptimeRobot](https://uptimerobot.com).
2. Monitor HTTP a cada **5 min**: `https://transparencia-caninde.onrender.com/health`.

O scraping periódico (60 s / 90 s) só roda enquanto o processo está ativo.

### Segurança (recomendado)

No painel Render → **Environment**:

| Variável | Valor |
|----------|--------|
| `WS_AUTH_TOKEN` | Token longo aleatório (ex. `openssl rand -hex 32`) |

Conexão do app:

```
wss://transparencia-caninde.onrender.com?token=SEU_TOKEN
```

Configure o mesmo token no Android (`WS_AUTH_TOKEN` no flavor `staging`/`prod` em `androidApp/build.gradle.kts`) e ajuste `WebSocketEndpoint` se ainda não anexar query string — hoje o token deve ser passado na URL no `MainActivity` / endpoint.

---

## Alternativa: Fly.io (mais estável 24/7)

Fly costuma manter a máquina ligada (pode exigir cartão para verificação, sem cobrança no tier pequeno).

```bash
# Instale: https://fly.io/docs/hands-on/install-flyctl/
cd server
fly launch --no-deploy   # use o fly.toml existente; confirme app name
fly secrets set NODE_ENV=production
# fly secrets set WS_AUTH_TOKEN=...
fly deploy
fly status
```

URL: `wss://transparencia-caninde.fly.dev` (após deploy).

Arquivo: [`server/fly.toml`](../server/fly.toml), região **gru** (São Paulo).

---

## App Android apontando para a nuvem

O flavor **staging** já usa:

| Campo | Valor |
|-------|--------|
| `WS_HOST` | `transparencia-caninde.onrender.com` |
| `WS_PORT` | `443` |
| `WS_SCHEME` | `wss` |

Build:

```bash
cd kmp-app
./gradlew :androidApp:installStagingDebug
```

Se você escolher **outro nome** de serviço no Render, altere `WS_HOST` em `androidApp/build.gradle.kts` (flavor `staging`).

**dev** continua com `ws://10.0.2.2:8080` (servidor local).

---

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `PORT` | Não (Render injeta) | Porta HTTP/WS (padrão `8080` local) |
| `NODE_ENV` | Sim em produção | `production` |
| `WS_AUTH_TOKEN` | Recomendada | Token na query `?token=` |
| `RATE_LIMIT_MAX` | Não | Padrão `120` |
| `RATE_LIMIT_WINDOW_MS` | Não | Padrão `60000` |

---

## Docker local / VPS

```bash
docker compose up --build
```

[`server/Dockerfile`](../server/Dockerfile) copia `lib/` e expõe `/health`.

---

## Solução de problemas

| Sintoma | Ação |
|---------|------|
| Deploy falha no build | Confirme `rootDir: server` e Node ≥ 18 |
| Health check falha | Aguarde o scrape inicial (~30–60 s); veja logs no Render |
| App não conecta | Use `wss` + host `.onrender.com`, porta **443**, não `8080` |
| Dados vazios após cold start | Abra o app e puxe para atualizar; ou use UptimeRobot |
| `EADDRINUSE` local | `lsof -i :8080` e `kill <PID>` |

---

## O que não usar no tier gratuito

- **Vercel / Netlify Functions** — não mantêm WebSocket persistente + scraping em background.
- **GitHub Actions** como servidor — apenas CI, não hospedagem do WS.

Para evolução (domínio `transparencia.caninde.ce.gov.br`, TLS próprio), use o flavor **prod** com DNS apontando para um VPS ou serviço pago.
