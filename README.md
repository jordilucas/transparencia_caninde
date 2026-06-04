# Transparência Canindé

Aplicativo **Kotlin Multiplatform** (Compose) + **servidor Node.js** para consultar dados públicos da **Prefeitura** e da **Câmara Municipal de Canindé**, CE, em tempo quase real via WebSocket.

O app **não inventa dados**: exibe apenas o que foi obtido dos portais oficiais. Se o scraping falhar, mostra mensagem de erro ou um resumo mínimo (contagens e listas vazias).

**Recursos:** listas clicáveis com telas de detalhe (vereador, matéria, secretaria, contrato, licitação, sessão, gestores), gráficos agregados no servidor (`REQUEST_DETAIL` / `DETAIL_DATA` — ver [`API.md`](API.md)).

## Arquitetura

```
caninde.ce.gov.br  +  cmcaninde.ce.gov.br
            ↓ (scraping HTTP)
      server/ (Node.js + WebSocket :8080)
            ↓ (JSON)
      kmp-app/shared (Ktor + Compose)
            ↓
      androidApp (Android)
```

| Pasta | Descrição |
|-------|-----------|
| [`server/`](server/) | Scraping Cheerio/Axios + broadcast WebSocket |
| [`kmp-app/`](kmp-app/) | UI e lógica KMP (`:shared`, `:androidApp`) |
| [`API.md`](API.md) | Contrato das mensagens WebSocket |
| [`docs/ROADMAP-DADOS.md`](docs/ROADMAP-DADOS.md) | Roadmap de fontes de dados e fases de implementação (continuidade) |
| [`docker-compose.yml`](docker-compose.yml) | Sobe o servidor na porta 8080 |

## Pré-requisitos

- **Servidor:** Node.js 18+
- **Android:** JDK 17, Android SDK 24+ (Android Studio recomendado)

## Servidor WebSocket

```bash
cd server
npm install
npm start
# desenvolvimento com reload:
npm run dev
# testes:
npm test
```

Servidor em `ws://localhost:8080` (emulador Android: `ws://10.0.2.2:8080`).

Variáveis opcionais:

| Variável | Descrição |
|----------|-----------|
| `PORT` | Porta (padrão `8080`) |
| `NODE_ENV` | `production` valida certificados TLS no scraping |
| `WS_AUTH_TOKEN` | Exige `?token=` na conexão WebSocket (ver [`server/.env.example`](server/.env.example)) |
| `RATE_LIMIT_MAX` | Limite de mensagens por IP |

## App Android

```bash
cd kmp-app
./gradlew :androidApp:installDevDebug
```

Variantes de ambiente (`dev`, `staging`, `prod`) configuram host/porta/esquema do WebSocket via `BuildConfig`. Detalhes em [`kmp-app/BUILD_VARIANTS.md`](kmp-app/BUILD_VARIANTS.md).

**Ordem de execução:** subir o servidor antes de abrir o app.

## Fontes de dados

- Prefeitura: [caninde.ce.gov.br](https://www.caninde.ce.gov.br)
- Câmara: [cmcaninde.ce.gov.br](https://www.cmcaninde.ce.gov.br) (vereadores, sessões, matérias)

Não confundir com **Canindé de São Francisco (SE)** (`camaradecaninde.se.gov.br`).

## Repositório

```bash
git clone git@github.com:jordilucas/transparencia_caninde.git
cd transparencia_caninde
```

## Licença

Projeto de transparência pública municipal. Uso conforme legislação de acesso à informação e termos dos portais de origem.
