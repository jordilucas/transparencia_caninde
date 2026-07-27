# Deploy do site web — GitHub Pages

O frontend (`kmp-app/webApp/`) é publicado na branch **`gh-pages`**. O backend WebSocket continua no [Render](HOSPEDAGEM-GRATUITA.md).

## URL publicada

**https://transparenciacaninde.com.br/**

Fallback (GitHub Pages): `https://jordilucas.github.io/transparencia_caninde/`

---

## DNS (obrigatório para o domínio próprio)

Configure no registrador de **`transparenciacaninde.com.br`**:

### Domínio raiz (`transparenciacaninde.com.br`)

Registros **A** apontando para os IPs do GitHub Pages:

| Tipo | Nome | Valor |
|------|------|--------|
| A | `@` | `185.199.108.153` |
| A | `@` | `185.199.109.153` |
| A | `@` | `185.199.110.153` |
| A | `@` | `185.199.111.153` |

### Subdomínio `www` (opcional)

| Tipo | Nome | Valor |
|------|------|--------|
| CNAME | `www` | `jordilucas.github.io` |

No GitHub (**Settings → Pages → Custom domain**), informe `transparenciacaninde.com.br` e marque **Enforce HTTPS** após o certificado ser emitido (pode levar até 24 h após o DNS propagar).

O arquivo `kmp-app/webApp/src/wasmJsMain/resources/CNAME` contém o mesmo domínio e é publicado automaticamente pelo workflow.

---

## GitHub Pages (Settings deste repo)

Em **https://github.com/jordilucas/transparencia_caninde/settings/pages** :

| Campo | Valor |
|-------|--------|
| Custom domain | `transparenciacaninde.com.br` |
| Source | Deploy from branch |
| Branch | `gh-pages` / `(root)` |
| Enforce HTTPS | ✅ (após DNS válido) |

### Workflow auxiliar

[Actions → Fix GitHub Pages config](https://github.com/jordilucas/transparencia_caninde/actions/workflows/fix-pages.yml) — reaplica branch `gh-pages` + domínio via API.

---

## WebSocket

| Host do site | Backend |
|--------------|---------|
| `localhost` | `ws://localhost:8080` |
| Produção (`transparenciacaninde.com.br`, github.io, etc.) | `wss://transparencia-caninde.onrender.com` |

O frontend web usa o backend Render; o domínio `.com.br` serve apenas o site estático.

---

## Deploy automático

Push em `main` (`kmp-app/**`) ou [Deploy web](https://github.com/jordilucas/transparencia_caninde/actions/workflows/deploy-web.yml) → Run workflow.

Build local:

```bash
cd kmp-app && ./gradlew :webApp:wasmJsBrowserDistribution
# Saída: webApp/build/dist/wasmJs/productionExecutable/
```

---

## Redireciona para `mercadinhosantos.me`?

Se **`jordilucas.github.io`** ainda redireciona para outro domínio, o problema está no repositório de user pages **`jordilucas/jordilucas.github.io`** (Custom domain em Settings → Pages). Remova o domínio antigo lá.

Com **`transparenciacaninde.com.br`** configurado, use o domínio próprio — não depende do redirect do github.io.
