# Dados públicos e segurança de coleta

Este documento descreve **de onde** o projeto obtém dados, **como** evita abuso dos portais governamentais e quais salvaguardas estão implementadas.

---

## Princípios

1. **Somente fontes oficiais** — dados capturados exclusivamente de portais públicos e governamentais de Canindé/CE e do Governo Transparente (links curados).
2. **Preferir APIs e dados abertos** — JSON oficial e REST antes de parse HTML.
3. **Não inventar dados** — se a fonte falhar, exibir cache, lista vazia ou erro explícito.
4. **Scraping responsável** — intervalos fixos, fila serial, cooldown de refresh e allowlist de hosts para não sobrecarregar os portais (evitar comportamento tipo DDoS).

---

## Fontes permitidas

| Host | Tipo | Uso |
|------|------|-----|
| `www.caninde.ce.gov.br` | **Dados abertos JSON** + HTML | Prefeitura: contratos, licitações, secretarias, publicações, obras, LRF |
| `www.cmcaninde.ce.gov.br` | **WordPress REST** + HTML | Câmara: vereadores, sessões, matérias |
| `www.governotransparente.com.br` | Links + páginas pontuais | Transparência financeira (IDs oficiais 11979490 / 11979588) |

Implementação da allowlist: `server/lib/allowed-hosts.js`.

Qualquer URL fora desses hosts é **rejeitada** antes do fetch HTTP (inclui `pagina_portal` sob demanda).

---

## Estratégia de coleta (prioridade)

### 1. Dados abertos — Prefeitura (preferencial)

```
GET https://www.caninde.ce.gov.br/dadosabertosexportar.php?d={dataset}&a={ano}&f=json
```

Datasets: `licitacoes`, `contratos`, `secretarias`, `publicacoes`, `obras`, `LRF`.

Arquivo: `server/lib/scraper-prefeitura-dadosabertos.js`

### 2. WordPress REST — Câmara (preferencial)

```
GET https://www.cmcaninde.ce.gov.br/wp-json/wp/v2/{vereadores|sessao|materia|…}
```

Arquivo: `server/lib/scraper-camara-wp.js`

### 3. HTML (complemento / fallback)

Usado quando JSON/REST não cobre o campo ou para detalhes sob demanda (biografias, páginas longas).

Arquivos: `scraper-prefeitura.js`, `scraper-camara.js`, `detail-handler.js`

### 4. Governo Transparente

- **Links curados** no app (não simula dados financeiros).
- Fetch HTML apenas para páginas oficiais já vinculadas, via allowlist.

---

## Salvaguardas anti-sobrecarga

| Mecanismo | Config | Efeito |
|-----------|--------|--------|
| Intervalo automático | 60s (Prefeitura), 90s (Câmara) | Scraping periódico previsível |
| **Cooldown de refresh** | `REFRESH_COOLDOWN_MS` (padrão 120s) | `REQUEST_REFRESH` não dispara scrape completo em loop |
| **Fila HTTP serial** | `FETCH_MIN_DELAY_MS` (padrão 200ms) | Espaça requests de saída |
| **Delay WP paginado** | `WP_PAGE_DELAY_MS` (padrão 150ms) | Pausa entre páginas da REST API |
| **Lock de scrape** | — | Evita dois scrapes simultâneos da mesma fonte |
| **Cache de listagens** | memória | Clientes leem cache; scrape só no timer ou refresh válido |
| **Cache de detalhes** | LRU ~200, TTL 10 min | Evita refetch repetido do mesmo item |
| **Rate limit WS (entrada)** | `RATE_LIMIT_MAX` / janela | Protege o servidor (não substitui proteção outbound) |
| **Allowlist de hosts** | `allowed-hosts.js` | Bloqueia SSRF / fetch para domínios arbitrários |
| **Timeout HTTP** | 15s | Evita conexões penduradas |

Arquivos: `server/lib/scrape-guard.js`, `server/lib/config.js`, `server/server.js`

---

## Riscos remanescentes e mitigação

| Risco | Mitigação atual | Melhoria futura |
|-------|-----------------|-----------------|
| Carga contínua nos portais (timer 60s/90s) | Intervalos conservadores + cache | Negociar API oficial / aumentar intervalo em produção |
| Muitos clientes pedindo refresh | Cooldown 2 min por fonte | Aumentar cooldown se necessário |
| Paginação WP grande | Delay entre páginas | Limitar páginas ou cache mais longo |
| HTML frágil | Merge JSON + HTML | Migrar mais campos para JSON/REST |
| Dados desatualizados | Refresh manual + timer | Indicador de “última atualização” mais visível |

---

## Variáveis de ambiente (servidor)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `REFRESH_COOLDOWN_MS` | `120000` | Intervalo mínimo entre refreshes manuais |
| `FETCH_MIN_DELAY_MS` | `200` | Atraso mínimo entre requests HTTP de saída |
| `WP_PAGE_DELAY_MS` | `150` | Pausa entre páginas da API WordPress |
| `RATE_LIMIT_MAX` | `120` | Máx. mensagens WS por IP por janela |
| `RATE_LIMIT_WINDOW_MS` | `60000` | Janela do rate limit WS |
| `WS_AUTH_TOKEN` | (vazio) | Token opcional na conexão WebSocket |

---

## O que o app **não** faz

- Não faz scraping no cliente (Android/iOS/Web) — só consome o servidor.
- Não acessa sites de notícias, redes sociais ou bases privadas.
- Não publica dados sem fonte rastreável no payload (`fonte`, `fontesUtilizadas`).
- Não abre fetch para URLs arbitrárias enviadas pelo usuário.

---

## Referências no código

- Allowlist: `server/lib/allowed-hosts.js`
- Throttle / cooldown / lock: `server/lib/scrape-guard.js`
- Orquestração: `server/server.js`
- Dados abertos: `server/lib/scraper-prefeitura-dadosabertos.js`
- WP REST: `server/lib/scraper-camara-wp.js`
- Documentação de fontes (app): `kmp-app/shared/.../domain/DataSources.kt`
