# Deploy do site web — GitHub Pages

O frontend (`kmp-app/webApp/`) é publicado automaticamente via GitHub Actions. O backend WebSocket continua no [Render](HOSPEDAGEM-GRATUITA.md).

## URL publicada

| Tipo | URL |
|------|-----|
| **GitHub Pages (padrão)** | `https://jordilucas.github.io/transparencia_caninde/` |
| **Domínio customizado** (futuro) | ex. `https://transparencia.caninde.ce.gov.br` |

O site detecta o host e conecta ao WebSocket:

| Host do site | Backend |
|--------------|---------|
| `localhost` / `127.0.0.1` | `ws://localhost:8080` |
| Qualquer outro (Pages, domínio próprio) | `wss://transparencia-caninde.onrender.com` |

---

## Ativação (uma vez)

1. No GitHub, abra **Settings → Pages**.
2. Em **Build and deployment**, escolha **Source: GitHub Actions**.
3. Faça push para `main` (ou rode manualmente **Actions → Deploy web → Run workflow**).
4. Aguarde o workflow terminar; a URL aparece no job **deploy** e em **Settings → Pages**.

> Se o primeiro deploy falhar com erro de ambiente `github-pages`, confirme o passo 2 (Source = GitHub Actions) e execute o workflow de novo.

---

## Workflow

Arquivo: [`.github/workflows/deploy-web.yml`](../.github/workflows/deploy-web.yml)

| Etapa | O que faz |
|-------|-----------|
| Trigger | Push em `main` alterando `kmp-app/**`, ou manual |
| Build | `./gradlew :webApp:jsBrowserDistribution` |
| SPA | Copia `index.html` → `404.html` (rotas futuras no cliente) |
| Jekyll | Cria `.nojekyll` (evita ignorar assets do Skiko/Compose) |
| Deploy | `actions/deploy-pages` → GitHub Pages |

Build local equivalente:

```bash
cd kmp-app
./gradlew :webApp:jsBrowserDistribution
ls webApp/build/dist/js/productionExecutable/
```

---

## Domínio customizado (Prefeitura / Câmara)

1. Crie `kmp-app/webApp/src/jsMain/resources/CNAME` com uma linha, ex.:
   ```
   transparencia.caninde.ce.gov.br
   ```
2. No DNS do domínio, adicione registro **CNAME** apontando para `jordilucas.github.io`.
3. Em **Settings → Pages → Custom domain**, informe o mesmo host.
4. Faça push; o próximo deploy incluirá o `CNAME` na raiz do site.

---

## Manter o backend responsivo

No plano gratuito do Render, o WebSocket **dorme** após inatividade. O site abre rápido, mas os dados podem demorar ~1 min na primeira carga.

Configure [UptimeRobot](https://uptimerobot.com) em `https://transparencia-caninde.onrender.com/health` (intervalo 5 min) — ver [HOSPEDAGEM-GRATUITA.md](HOSPEDAGEM-GRATUITA.md).

---

## Solução de problemas

| Sintoma | Ação |
|---------|------|
| `github.io/...` redireciona para outro domínio | **Settings → Pages → Custom domain** → apague o domínio (ex. domínio de outro projeto) e salve |
| URL retorna 404 / site antigo | Confirme push do workflow e **Source: GitHub Actions** |
| Workflow falha no Gradle | Veja logs; confirme Java 17 e `./gradlew` em `kmp-app/` |
| Página em branco | Abra DevTools → Console; confirme URL com barra final `/transparencia_caninde/` |
| “Conectando…” eterno | Backend Render dormindo; aguarde ou use UptimeRobot |
| 404 em rota futura | Confirme que `404.html` existe no artifact (gerado pelo workflow) |
| Assets 404 | Confirme `.nojekyll` no deploy |

---

## Alternativas

| Serviço | Quando usar |
|---------|-------------|
| **Cloudflare Pages** | CDN mais agressivo; upload da pasta `productionExecutable/` |
| **Render Static Site** | Mesmo painel do backend WS |
| **Firebase Hosting** | Se integrar outros serviços Google |

Para este projeto, **GitHub Pages + Actions** é a opção mais simples: grátis, no mesmo repositório e com HTTPS automático.
