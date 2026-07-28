# Deploy do site web — GitHub Pages

O frontend (`kmp-app/webApp/`) é publicado na branch **`gh-pages`**. O backend WebSocket continua no [Render](HOSPEDAGEM-GRATUITA.md).

## URL publicada

**https://transparenciacaninde.com.br/**

Fallback (GitHub Pages): `https://jordilucas.github.io/transparencia_caninde/`

---

## DNS (obrigatório para o domínio próprio)

**Diagnóstico atual:** o domínio usa DNS do Registro.br (`a.auto.dns.br`), mas **não possui registros A** apontando para o GitHub Pages. Por isso o GitHub exibe:

> *Domain does not resolve to the GitHub Pages server (NotServedByPagesError)*

O site na branch `gh-pages` e o arquivo `CNAME` já estão corretos. Falta **só configurar o DNS** no Registro.br.

### Passo a passo — Registro.br

1. Acesse **https://registro.br** → **Meus domínios** → `transparenciacaninde.com.br`
2. Clique em **DNS** (ou **Configurar endereçamento**)
3. Use **Modo avançado** (editar zona) e adicione **todos** estes registros:

```
transparenciacaninde.com.br.     IN  A      185.199.108.153
transparenciacaninde.com.br.     IN  A      185.199.109.153
transparenciacaninde.com.br.     IN  A      185.199.110.153
transparenciacaninde.com.br.     IN  A      185.199.111.153
www.transparenciacaninde.com.br. IN  CNAME  jordilucas.github.io.
```

4. **Remova** registros A/AAAA conflitantes no apex (se existirem apontando para outro lugar)
5. Salve e aguarde propagação (15 min – 24 h)

> O subdomínio **`www`** não é opcional: o GitHub trata `www.transparenciacaninde.com.br` como *alternate name* e exige que ele também aponte para o Pages.

### Formulário simples (se não usar modo avançado)

| Tipo | Nome / Host | Valor / Destino |
|------|-------------|-----------------|
| A | `@` ou vazio | `185.199.108.153` |
| A | `@` ou vazio | `185.199.109.153` |
| A | `@` ou vazio | `185.199.110.153` |
| A | `@` ou vazio | `185.199.111.153` |
| CNAME | `www` | `jordilucas.github.io` |

**Importante:** o CNAME do `www` deve apontar para `jordilucas.github.io` — **sem** `/transparencia_caninde` no final.

### Verificar se funcionou

No terminal (Mac/Linux):

```bash
dig transparenciacaninde.com.br A +short
dig www.transparenciacaninde.com.br CNAME +short
```

Resultado esperado:

```
185.199.108.153
185.199.109.153
185.199.110.153
185.199.111.153
jordilucas.github.io.
```

Depois, no GitHub (**Settings → Pages**):

1. Custom domain: `transparenciacaninde.com.br` → **Save**
2. Aguarde o check verde **DNS check successful**
3. Ative **Enforce HTTPS** (pode demorar até 24 h após o DNS)

O arquivo `kmp-app/webApp/src/wasmJsMain/resources/CNAME` contém o mesmo domínio e é publicado automaticamente pelo workflow.

---

## Erro NotServedByPagesError

| Causa | Solução |
|-------|---------|
| Sem registros A no apex | Adicionar os 4 IPs do GitHub (tabela acima) |
| `www` sem CNAME | CNAME `www` → `jordilucas.github.io` |
| DNS ainda propagando | Aguardar e clicar **Recheck** em Settings → Pages |
| Custom domain em outro repo | Remover o domínio do outro repositório GitHub |
| Branch errada | Source deve ser `gh-pages` / `(root)` |

Enquanto o DNS não propagar, o site continua acessível em:  
**https://jordilucas.github.io/transparencia_caninde/**

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
