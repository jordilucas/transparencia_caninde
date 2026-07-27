# Deploy do site web — GitHub Pages

O frontend (`kmp-app/webApp/`) é publicado na branch **`gh-pages`**. O backend WebSocket continua no [Render](HOSPEDAGEM-GRATUITA.md).

## URL publicada

`https://jordilucas.github.io/transparencia_caninde/`

---

## Redireciona para `mercadinhosantos.me`? (**causa raiz**)

O site **Transparência Canindé já está publicado** (branch `gh-pages` com `index.html`, JS, etc.).

O Safari não abre porque **todo** `jordilucas.github.io` redireciona para `mercadinhosantos.me` — domínio que **não existe mais** / não responde.

Isso **não vem deste repositório**. Está configurado no repositório de **user pages**:

**`jordilucas/jordilucas.github.io`**

### Correção (2 minutos)

1. Abra: **https://github.com/jordilucas/jordilucas.github.io/settings/pages**
2. Em **Custom domain**, apague `mercadinhosantos.me` (deixe **vazio**).
3. Clique **Save**.
4. Aguarde 2–5 minutos.
5. Teste: **https://jordilucas.github.io/transparencia_caninde/**

### Confirmar branch deste repo

Em **https://github.com/jordilucas/transparencia_caninde/settings/pages** :

| Campo | Valor |
|-------|--------|
| Custom domain | *(vazio)* |
| Source | Deploy from branch |
| Branch | `gh-pages` / `(root)` |

### Workflow auxiliar (opcional)

[Actions → Fix GitHub Pages config](https://github.com/jordilucas/transparencia_caninde/actions/workflows/fix-pages.yml) — ajusta **este** repo via API; **não** altera `jordilucas.github.io`.

---

## WebSocket

| Host do site | Backend |
|--------------|---------|
| `localhost` | `ws://localhost:8080` |
| Produção | `wss://transparencia-caninde.onrender.com` |

---

## Deploy automático

Push em `main` (`kmp-app/**`) ou [Deploy web](https://github.com/jordilucas/transparencia_caninde/actions/workflows/deploy-web.yml) → Run workflow.

Build local:

```bash
cd kmp-app && ./gradlew :webApp:jsBrowserDistribution
```

---

## Domínio próprio (Canindé)

1. Arquivo `kmp-app/webApp/src/jsMain/resources/CNAME` → `transparencia.caninde.ce.gov.br`
2. DNS CNAME → `jordilucas.github.io`
3. Custom domain em **Settings → Pages** deste repo

---

## Alternativa se quiser manter `mercadinhosantos.me` no user site

Use **Cloudflare Pages** ou **Netlify** com URL própria (ex. `transparencia-caninde.pages.dev`) — upload de `webApp/build/dist/js/productionExecutable/`.
