# 📡 API WebSocket — Portal da Transparência Canindé

## Visão Geral

O servidor WebSocket fornece acesso em tempo real aos dados dos portais de transparência da Prefeitura e Câmara Municipal de Canindé, Ceará. O servidor faz scraping periódico (HTTP) dos portais e distribui as atualizações via WebSocket a todos os clientes conectados.

### Informações de Conexão

- **Host:** `ws://localhost:8080` (desenvolvimento)
- **Host (Produção):** `ws://seu-dominio.com:8080`
- **Protocolo:** WebSocket (RFC 6455)
- **Formato:** JSON
- **Encoding:** UTF-8

---

## 📨 Mensagens do Cliente

Todos os comandos são enviados como JSON. A estrutura básica é:

```json
{
  "type": "TIPO_COMANDO",
  "source": "prefeitura|camara|all" (opcional),
  "timestamp": "2025-05-29T10:00:00Z" (opcional)
}
```

### 1. REQUEST_PREFEITURA

Solicita dados atuais da Prefeitura.

**Requisição:**
```json
{
  "type": "REQUEST_PREFEITURA"
}
```

**Resposta:**
```json
{
  "type": "PREFEITURA_DATA",
  "payload": {
    "municipio": "Canindé",
    "estado": "CE",
    "fonte": "https://www.caninde.ce.gov.br/acessoainformacao.php",
    "contratos": [
      {
        "numero": "045/2025",
        "objeto": "Pavimentação e drenagem Setor B",
        "valor": "R$ 3.820.000,00",
        "empresa": "Construtora Norte Ltda",
        "data": "02/01/2025"
      },
      ...
    ],
    "licitacoes": [
      {
        "numero": "012/2025",
        "modalidade": "Pregão Eletrônico",
        "objeto": "Gêneros alimentícios merenda escolar",
        "situacao": "Em andamento"
      },
      ...
    ],
    "diariosOficiais": [
      "Diário Oficial nº 1.432 — 27/05/2025 — Portaria 234/2025 — Designação de servidores",
      ...
    ],
    "secretarias": [
      {
        "id": "3",
        "nome": "Secretaria de Educação",
        "secretario": "",
        "url": "https://www.caninde.ce.gov.br/secretaria.php?sec=3",
        "contato": { "email": "", "telefone": "", "whatsapp": "", "endereco": "", "horarioFuncionamento": "" }
      }
    ],
    "graficos": {
      "prefeitura": [
        { "titulo": "Licitações por situação", "labels": ["Aberta", "Homologada"], "valores": [5, 2] }
      ],
      "camara": []
    },
    "resumo": {
      "totalContratos": 127,
      "totalLicitacoes": 34,
      "exercicio": 2025
    },
    "scrapedAt": "2025-05-29T09:41:32Z"
  },
  "timestamp": "2025-05-29T09:41:32Z"
}
```

---

### 2. REQUEST_CAMARA

Solicita dados atuais da Câmara Municipal.

**Requisição:**
```json
{
  "type": "REQUEST_CAMARA"
}
```

**Resposta:**
```json
{
  "type": "CAMARA_DATA",
  "payload": {
    "municipio": "Canindé",
    "estado": "CE",
    "fonte": "https://www.cmcaninde.ce.gov.br/caninde-transparente/",
    "parlamentares": [
      {
        "nome": "Karlinda Coelho",
        "nomeCompleto": "Karlinda Coelho da Silva",
        "partido": "REPUBLICANOS",
        "cargo": "Presidente",
        "foto": "url/foto.jpg",
        "slug": "karlinda-coelho",
        "profileUrl": "https://www.cmcaninde.ce.gov.br/vereadores/karlinda-coelho/",
        "contato": { "email": "", "telefone": "", "whatsapp": "", "endereco": "", "horarioFuncionamento": "" },
        "biografia": ""
      }
    ],
    "sessoes": [
      {
        "titulo": "Sessão Ordinária nº 18/2025",
        "data": "27/05/2025"
      },
      ...
    ],
    "materias": [
      {
        "titulo": "PL 023/2025 — Institui o Programa Municipal de Incentivo ao Esporte",
        "tipo": "Projeto de Lei",
        "slug": "pl-023-2025",
        "url": "https://www.cmcaninde.ce.gov.br/materia/pl-023-2025/",
        "autor": "",
        "dataPublicacao": "",
        "pdfUrl": "",
        "resumo": ""
      }
    ],
    "graficos": {
      "prefeitura": [],
      "camara": [
        { "titulo": "Matérias por tipo", "labels": ["Requerimento", "Projeto de Lei"], "valores": [4, 2] }
      ]
    },
    "mesaDiretora": [
      {
        "nome": "Cícero Rodrigues",
        "cargo": "Presidente"
      },
      ...
    ],
    "resumoCamara": {
      "totalParlamentares": 13,
      "totalSessoes2025": 42,
      "totalMaterias": 89
    },
    "scrapedAt": "2025-05-29T09:43:15Z"
  },
  "timestamp": "2025-05-29T09:43:15Z"
}
```

---

### 3. REQUEST_REFRESH

Força o servidor a fazer scraping imediato de um ou ambas as fontes.

**Requisição:**
```json
{
  "type": "REQUEST_REFRESH",
  "source": "prefeitura"
}
```

**Parâmetros:**
- `source` (string): 
  - `"prefeitura"` — scrape apenas Prefeitura
  - `"camara"` — scrape apenas Câmara
  - `"all"` ou omitido — scrape ambas

**Resposta (broadcast):**
```json
{
  "type": "REFRESHING",
  "payload": {
    "source": "prefeitura"
  },
  "timestamp": "2025-05-29T10:05:00Z"
}
```

Seguido por `PREFEITURA_DATA` ou `CAMARA_DATA` com dados atualizados.

---

### 4. REQUEST_DETAIL

Carrega ficha de detalhe sob demanda (scraping HTTP pontual com cache LRU no servidor).

**Requisição:**
```json
{
  "type": "REQUEST_DETAIL",
  "payload": {
    "entity": "vereador",
    "id": "karlinda-coelho"
  }
}
```

**Entidades suportadas:** `vereador`, `materia`, `secretaria`, `contrato`, `licitacao`, `sessao`, `gestores`, `institucional` (id `camara` ou `prefeitura`).

**Resposta:**
```json
{
  "type": "DETAIL_DATA",
  "payload": {
    "entity": "vereador",
    "entityId": "karlinda-coelho",
    "parlamentar": { "nome": "...", "contato": { "email": "..." } },
    "error": null
  },
  "timestamp": "2025-06-04T12:00:00Z"
}
```

Contrato, licitação e sessão sem página no portal usam os dados já presentes na listagem em cache.

---

### 5. PING

Heartbeat para manter a conexão viva e verificar saúde do servidor.

**Requisição:**
```json
{
  "type": "PING"
}
```

**Resposta:**
```json
{
  "type": "PONG",
  "timestamp": "2025-05-29T10:10:00Z"
}
```

---

## 📨 Mensagens do Servidor

Estas mensagens são enviadas pelo servidor (broadcast ou unicast).

### SERVER_STATUS

Enviada ao conectar. Contém metadados do servidor.

```json
{
  "type": "SERVER_STATUS",
  "payload": {
    "version": "1.0.0",
    "sources": [
      "https://www.caninde.ce.gov.br/acessoainformacao.php",
      "https://www.cmcaninde.ce.gov.br/caninde-transparente/"
    ],
    "intervals": {
      "prefeitura": 60000,
      "camara": 90000
    },
    "lastUpdated": {
      "prefeitura": "2025-05-29T09:41:32Z",
      "camara": "2025-05-29T09:43:15Z"
    }
  },
  "timestamp": "2025-05-29T10:00:00Z"
}
```

**Campos:**
- `version`: Versão da API
- `sources`: URLs dos portais de origem
- `intervals`: Intervalos de scraping em ms
- `lastUpdated`: Último timestamp de sucesso para cada fonte

---

### PREFEITURA_DATA

Enviada quando dados da Prefeitura estão disponíveis (via REQUEST ou broadcast periódico).

Ver seção [REQUEST_PREFEITURA → Resposta](#1-request_prefeitura)

---

### CAMARA_DATA

Enviada quando dados da Câmara estão disponíveis (via REQUEST ou broadcast periódico).

Ver seção [REQUEST_CAMARA → Resposta](#2-request_camara)

---

### ERROR

Enviada em caso de erro no servidor.

```json
{
  "type": "ERROR",
  "payload": {
    "message": "Falha ao fazer scraping da prefeitura: timeout"
  },
  "timestamp": "2025-05-29T10:15:00Z"
}
```

---

## 🔄 Ciclos de Atualização Automática

O servidor faz scraping periódico e envia as atualizações a **todos** os clientes conectados via broadcast:

| Fonte | Intervalo | Comportamento |
|-------|-----------|---------------|
| Prefeitura | 60 segundos | Scrape HTTP, enviado via `PREFEITURA_DATA` |
| Câmara | 90 segundos | Scrape HTTP, enviado via `CAMARA_DATA` |

Se o scraping falhar, o servidor tenta novamente no próximo ciclo (mantém cache anterior).

---

## 🔐 Tratamento de Erros

### Códigos de Status

| Tipo | Descrição | Ação do Cliente |
|------|-----------|-----------------|
| Conectado | 1000 | Enviar REQUEST ou aguardar broadcast |
| Erro no cliente | 1002 (protocol error) | Reconectar após 5-10s |
| Erro no servidor | 1011 | Reconectar com backoff exponencial |
| Timeout | 30s sem resposta | Enviar PING, se sem resposta, reconectar |

### Estratégia de Reconexão Recomendada

```
Tentativa 1: aguardar 5s
Tentativa 2: aguardar 10s
Tentativa 3: aguardar 15s
Tentativa 4+: aguardar 30s
Máximo de tentativas: 10
```

---

## 📊 Estrutura de Dados

### Contrato

```typescript
interface Contrato {
  numero: string;          // ex: "045/2025"
  objeto: string;          // Descrição do objeto
  valor: string;           // Valor em reais (ex: "R$ 3.820.000,00")
  empresa: string;         // Nome da empresa contratada
  data: string;            // Data (ex: "02/01/2025")
}
```

### Licitação

```typescript
interface Licitacao {
  numero: string;          // ex: "012/2025"
  modalidade: string;      // ex: "Pregão Eletrônico"
  objeto: string;          // Descrição do objeto
  situacao: string;        // ex: "Em andamento", "Homologado"
}
```

### Parlamentar

```typescript
interface Parlamentar {
  nome: string;            // Nome completo
  partido: string;         // Sigla do partido (ex: "PT", "MDB")
  cargo: string;           // Cargo (ex: "Presidente", "Vereador")
  foto: string;            // URL da foto
}
```

### Sessão

```typescript
interface Sessao {
  titulo: string;          // Tipo e número (ex: "Sessão Ordinária nº 18/2025")
  data: string;            // Data (ex: "27/05/2025")
}
```

### Matéria

```typescript
interface Materia {
  titulo: string;          // Título completo
  tipo: string;            // Tipo (ex: "Projeto de Lei", "Requerimento")
}
```

---

## 💡 Exemplos de Implementação

### Cliente Kotlin (KMP)

```kotlin
val client = HttpClient {
    install(WebSockets)
}

client.webSocket(host = "localhost", port = 8080, path = "/") {
    // Enviar requisição
    send(Frame.Text("""{"type":"REQUEST_PREFEITURA"}"""))
    
    // Receber resposta
    for (frame in incoming) {
        if (frame is Frame.Text) {
            val json = Json.decodeFromString<WsMessage>(frame.readText())
            when (json.type) {
                "PREFEITURA_DATA" -> updateUI(json.payload)
                "ERROR" -> showError(json.payload.message)
            }
        }
    }
}
```

### Cliente JavaScript

```javascript
const ws = new WebSocket('ws://localhost:8080');

ws.addEventListener('open', () => {
  ws.send(JSON.stringify({ type: 'REQUEST_PREFEITURA' }));
});

ws.addEventListener('message', (event) => {
  const msg = JSON.parse(event.data);
  console.log(`Recebido: ${msg.type}`, msg.payload);
});

ws.addEventListener('error', (error) => {
  console.error('WebSocket error:', error);
});
```

### Cliente Python

```python
import asyncio
import json
import websockets

async def main():
    async with websockets.connect('ws://localhost:8080') as ws:
        # Enviar
        await ws.send(json.dumps({'type': 'REQUEST_PREFEITURA'}))
        
        # Receber
        message = await ws.recv()
        data = json.loads(message)
        print(f"Recebido: {data['type']}")

asyncio.run(main())
```

---

## 🚀 Performance

### Tamanho de Payloads

| Mensagem | Tamanho | Tempo de Transferência (100 Mbps) |
|----------|---------|----------------------------------|
| REQUEST | ~30 bytes | < 1ms |
| PONG | ~50 bytes | < 1ms |
| PREFEITURA_DATA | ~15-20 KB | 1-2ms |
| CAMARA_DATA | ~8-12 KB | < 1ms |

### Throughput

- **Máximo de conexões simultâneas:** ~1000 (com ~2GB RAM Node.js)
- **Latência de broadcast:** < 100ms
- **Taxa de scraping:** HTTP + parse = ~2-5s por fonte

---

## 📝 Changelog

### v1.0.0 (2025-05-29)
- ✅ Suporte inicial para Prefeitura (contratos, licitações, diário oficial)
- ✅ Suporte para Câmara (parlamentares, sessões, matérias, mesa diretora)
- ✅ WebSocket full-duplex com broadcast
- ✅ Reconexão automática recomendada no cliente
- ✅ Health check periódico (PING/PONG)

---

## 📞 Suporte

Para reportar problemas ou sugestões:
- Issues: `https://github.com/seu-usuario/transparencia-caninde/issues`
- Email: `dev@caninde.ce.gov.br`
