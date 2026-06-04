# 📱 Transparência Canindé — Portal da Transparência em Tempo Real

App **Kotlin Multiplatform (KMP)** com Compose Multiplatform para visualizar dados em tempo real do Portal da Transparência da Prefeitura e Câmara Municipal de Canindé, CE.

---

## 🏗️ Arquitetura

```
transparencia-caninde/
├── server/                    # WebSocket Server (Node.js)
│   ├── server.js             # Scraper + WebSocket broadcaster
│   └── package.json
├── kmp-app/                   # Kotlin Multiplatform Project
│   ├── shared/               # Shared logic (iOS + Android)
│   │   ├── src/commonMain/   # Código compartilhado
│   │   │   ├── domain/       # Models, use cases
│   │   │   ├── data/         # Repository, WebSocket client
│   │   │   └── presentation/ # UI Compose
│   │   ├── src/androidMain/  # Android-specific
│   │   └── src/iosMain/      # iOS-specific
│   ├── androidApp/           # Android app entry point
│   └── build.gradle.kts      # Root build config
```

### Fluxo de Dados

```
Portais Web (HTML)
    ↓
[Node.js Server - Scraping periódico]
    ↓
WebSocket (ws://host:8080)
    ↓
[KMP App - Ktor WebSocket Client]
    ↓
StateFlow (Kotlin Coroutines)
    ↓
[Compose UI - Recomposição automática]
```

---

## 🚀 Configuração Rápida

### 1️⃣ **Servidor WebSocket (Node.js)**

#### Pré-requisitos
- Node.js 18+ 
- npm ou yarn

#### Instalação

```bash
cd server
npm install
```

#### Executar

```bash
npm start
# Ou em desenvolvimento com hot-reload:
npm run dev
```

**Saída esperada:**
```
🚀 Servidor WebSocket rodando na porta 8080
   ws://localhost:8080
```

#### Testes via WebSocket

```bash
# Use websocat ou wscat para testar
wscat -c ws://localhost:8080

# Comandos disponíveis (JSON):
{"type":"REQUEST_PREFEITURA"}
{"type":"REQUEST_CAMARA"}
{"type":"REQUEST_REFRESH","source":"prefeitura"}
{"type":"PING"}
```

---

### 2️⃣ **App Kotlin Multiplatform**

#### Pré-requisitos

- **Android**
  - Android Studio Flamingo+ (ou IntelliJ IDEA)
  - JDK 17+
  - Android SDK 24+

- **iOS**
  - Xcode 15+
  - CocoaPods

#### Configuração da Conexão WebSocket

**Arquivo:** `kmp-app/shared/src/commonMain/kotlin/br/gov/caninde/transparencia/data/TransparenciaRepository.kt`

```kotlin
companion object {
    // Para emulador Android (VM Linux na máquina)
    const val WS_HOST = "10.0.2.2"
    
    // Para dispositivo físico ou iOS (IP da máquina na rede local)
    // const val WS_HOST = "192.168.1.XX"  // Substitua pelo seu IP
    
    const val WS_PORT = 8080
}
```

> **Dica:** Execute `ipconfig` (Windows) ou `ifconfig` (Mac/Linux) para encontrar seu IP local.

#### Build Android

```bash
cd kmp-app
./gradlew build
# ou via Android Studio: Build → Build Bundle(s) / APK(s)
```

#### Executar no Emulador Android

```bash
./gradlew installDebug
# ou via Android Studio: Run → Run 'app'
```

#### Build iOS

```bash
cd kmp-app/iosApp
pod install
open iosApp.xcworkspace
# No Xcode: Product → Run (⌘R)
```

---

## 📡 Protocolo WebSocket

### Mensagens Servidor → Cliente

#### `PREFEITURA_DATA`
```json
{
  "type": "PREFEITURA_DATA",
  "payload": {
    "municipio": "Canindé",
    "estado": "CE",
    "contratos": [
      {
        "numero": "045/2025",
        "objeto": "Pavimentação Setor B",
        "valor": "R$ 3.820.000,00",
        "empresa": "Construtora Norte Ltda",
        "data": "02/01/2025"
      }
    ],
    "licitacoes": [...],
    "diariosOficiais": [...],
    "secretarias": [...],
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

#### `CAMARA_DATA`
```json
{
  "type": "CAMARA_DATA",
  "payload": {
    "municipio": "Canindé",
    "parlamentares": [
      {
        "nome": "Cícero Rodrigues",
        "partido": "PT",
        "cargo": "Presidente",
        "foto": ""
      }
    ],
    "sessoes": [...],
    "materias": [...],
    "mesaDiretora": [...],
    "resumo": {
      "totalParlamentares": 13,
      "totalSessoes2025": 42,
      "totalMaterias": 89
    },
    "scrapedAt": "2025-05-29T09:43:15Z"
  },
  "timestamp": "2025-05-29T09:43:15Z"
}
```

#### `SERVER_STATUS`
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
  }
}
```

### Mensagens Cliente → Servidor

```json
// Solicitar dados prefeitura
{"type":"REQUEST_PREFEITURA"}

// Solicitar dados câmara
{"type":"REQUEST_CAMARA"}

// Atualizar scraping
{"type":"REQUEST_REFRESH","source":"prefeitura"}
// Opções source: "prefeitura", "camara", "all"

// Heartbeat
{"type":"PING"}
```

---

## 🎨 Interface Visual

### Telas Implementadas

1. **Dashboard Prefeitura**
   - Métricas: contratos, licitações, exercício
   - Abas: Contratos, Licitações, Diário Oficial, Secretarias
   - Gráfico de execução mensal
   - Live badge com badge de atualização em tempo real

2. **Câmara Municipal**
   - Cards de resumo: vereadores, sessões, matérias, verbas
   - Abas: Parlamentares (com presença), Sessões, Matérias, Mesa Diretora
   - Avatares com iniciais dos nomes
   - Status visual de presença (verde/âmbar/vermelho)

3. **Gráficos**
   - Execução orçamentária mensal (barras)
   - Distribuição por secretaria (progresso)

4. **Busca**
   - Campo de busca multi-fonte
   - Resultados filtrados por contratos e vereadores
   - Preview em tempo real

### Paleta de Cores

| Token | Cor | Uso |
|-------|-----|-----|
| Navy800 | `#1B3A5C` | Headers, elementos primários |
| Blue500 | `#378ADD` | Botões, links, destaques |
| Green500 | `#3B6D11` | Status positivo, "Pago" |
| Amber700 | `#854F0B` | Status parcial, "Em andamento" |
| Red700 | `#A32D2D` | Status negativo, "Pendente" |

---

## 🔧 Customização

### Mudar URL do Servidor

**Android:** `res/values/strings.xml`
```xml
<string name="ws_host">10.0.2.2</string>
```

**Código:** `TransparenciaRepository.kt`
```kotlin
const val WS_HOST = "seu-ip-aqui"
const val WS_PORT = 8080
```

### Adicionar Novos Dados

1. **Server (Node.js):** Adicione novo scraper em `server.js`
   ```javascript
   async function scrapeNovosDados() {
     // fetch + parse HTML
     return { ... }
   }
   ```

2. **Models KMP:** Estenda `WsPayload` em `Models.kt`

3. **UI:** Crie nova `@Composable` screen

---

## 🐛 Troubleshooting

### ❌ "Erro: Conexão recusada ws://10.0.2.2:8080"

**Solução:** 
- Certifique-se que o servidor Node está rodando: `npm start`
- Verifique se a porta 8080 está aberta: `netstat -an | grep 8080`
- Se usar dispositivo físico, substitua `10.0.2.2` pelo IP local da máquina

### ❌ "HTML parsing erro no servidor"

**Solução:**
- Os portais podem ter mudado de estrutura
- Verifique as URLs em `server.js`
- Ajuste os seletores CSS em `$('...')`

### ❌ "App congela ao conectar"

**Solução:**
- Aumente o timeout: `const RECONNECT_DELAY_MS = 15_000`
- Verifique logs: aDB `adb logcat | grep Transparencia`

### ❌ "Dados não atualizam após 1 minuto"

**Solução esperada:** O servidor faz scraping a cada 60s (prefeitura) e 90s (câmara). Se não há atualização:
- Verifique logs do servidor: `npm start` (saída console)
- Os portais podem estar indisponíveis
- Tente força manualmente: envie `REQUEST_REFRESH` via WebSocket

---

## 📊 Monitoramento

### Logs do Servidor

```bash
# Terminal rodando Node.js
[Prefeitura] iniciando scraping...
[Prefeitura] OK — 127 contratos, 34 licitações
[WS] cliente conectado: 127.0.0.1  total: 1
[WS] mensagem recebida: REQUEST_PREFEITURA
```

### Logs do App (Android)

```bash
adb logcat | grep -i "transparencia\|ws\|ktor"
```

---

## 🚢 Deploy

### Servidor Node.js → Servidor Linux/Cloud

```bash
# Na máquina remota
git clone <repo>
cd server
npm install --production
PORT=8080 nohup node server.js > app.log 2>&1 &

# Ou com PM2
npm install -g pm2
pm2 start server.js --name transparencia
pm2 save
```

### App Android → Google Play Store

```bash
./gradlew bundleRelease
# Assine e envie em: Google Play Console
```

---

## 📚 Referências

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Ktor Client WebSockets](https://ktor.io/docs/websocket-client.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-mpp/)
- [Koin Dependency Injection](https://insert-koin.io/)
- [Portal da Transparência Canindé](https://www.caninde.ce.gov.br/acessoainformacao.php)

---

## 📄 Licença

MIT © 2025 Prefeitura de Canindé

---

## 👥 Contribuindo

Pull requests são bem-vindos! Por favor abra uma issue para discussões maiores.

---

**Desenvolvido com ❤️ para Canindé, Ceará**
