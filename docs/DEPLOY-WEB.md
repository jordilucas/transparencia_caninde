# Deploy do site web — GitHub Pages

O frontend (`kmp-app/webApp/`) é publicado automaticamente via GitHub Actions na branch **`gh-pages`**. O backend WebSocket continua no [Render](HOSPEDAGEM-GRATUITA.md).

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

## Ativação (uma vez) — **obrigatório**

### 1. Remover redirecionamento para `mercadinhosantos.me` (**urgente**)

O site **já está publicado** na branch `gh-pages`, mas o GitHub redireciona para um domínio antigo que não existe.

**Opção A — workflow automático (recomendado)**

1. [Actions → Fix GitHub Pages config](https://github.com/jordilucas/transparencia_caninde/actions/workflows/fix-pages.yml) → **Run workflow**.
2. Aguarde ficar verde (~30 s).
3. Teste: `https://jordilucas.github.io/transparencia_caninde/` (pode levar 1–2 min).

**Opção B — manual no GitHub**

1. [Settings → Pages](https://github.com/jordilucas/transparencia_caninde/settings/pages).
2. **Custom domain:** apague `mercadinhosantos.me` (deixe **vazio**) → **Save**.
3. **Source:** Deploy from branch → **`gh-pages`** / **`/ (root)`** → **Save**.

### 2. Confirmar branch `gh-pages` (se ainda não fez)

1. Na mesma página **Settings → Pages**.
2. **Build and deployment → Source:** escolha **Deploy from a branch**.
3. **Branch:** `gh-pages` · pasta **`/ (root)`** → **Save**.

> Não use “GitHub Actions” como source — o workflow publica na branch `gh-pages` com `peaceiris/actions-gh-pages`.

### 3. Rodar o deploy

1. [Actions → Deploy web](https://github.com/jordilucas/transparencia_caninde/actions/workflows/deploy-web.yml) → **Run workflow**.
2. Aguarde o job **deploy** ficar verde (~4 min).
3. Confirme que a branch **`gh-pages`** foi criada no repositório.

URL: `https://jordilucas.github.io/transparencia_caninde/`

---

## Workflow

Arquivo: [`.github/workflows/deploy-web.yml`](../.github/workflows/deploy-web.yml)

| Etapa | O que faz |
|-------|-----------|
| Trigger | Push em `main` (`kmp-app/**`) ou manual |
| Build | `./gradlew :webApp:jsBrowserDistribution` |
| SPA | `index.html` → `404.html` |
| Jekyll | `.nojekyll` |
| Deploy | Push para branch **`gh-pages`** |

---

## Erros comuns

| Erro / sintoma | Solução |
|----------------|---------|
| *Ensure GitHub Pages has been enabled* (workflow antigo) | Atualize o workflow; use **Deploy from branch → gh-pages** |
| *Creating Pages deployment failed / 404* | Source deve ser **branch gh-pages**, não “GitHub Actions” |
| Redireciona para `mercadinhosantos.me` | Apague **Custom domain** em Settings → Pages |
| Aviso *Node 20 is deprecated* | Aviso das actions; não impede o deploy |
| Site 404 após deploy verde | Confirme branch `gh-pages` + source Pages apontando para ela; aguarde ~2 min |

---

## Domínio customizado (Prefeitura / Câmara)

1. Crie `kmp-app/webApp/src/jsMain/resources/CNAME` com uma linha, ex. `transparencia.caninde.ce.gov.br`.
2. DNS: **CNAME** → `jordilucas.github.io`.
3. **Settings → Pages → Custom domain** → mesmo host.
4. Push → novo deploy inclui o `CNAME`.

---

## Manter o backend responsivo

Render free **dorme** após inatividade. [UptimeRobot](https://uptimerobot.com) em `/health` a cada 5 min — ver [HOSPEDAGEM-GRATUITA.md](HOSPEDAGEM-GRATUITA.md).

---

## Build local

```bash
cd kmp-app
./gradlew :webApp:jsBrowserDistribution
ls webApp/build/dist/js/productionExecutable/
```
