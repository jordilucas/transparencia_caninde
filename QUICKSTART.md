# 🚀 Quick Start — Transparência Canindé

**Tempo estimado:** 10-15 minutos para rodar tudo localmente

---

## ⚡ 1. Servidor WebSocket (Node.js)

### Pré-requisitos
- Node.js 18+ ([download](https://nodejs.org/))
- Porta 8080 disponível

### Instalação e Execução

```bash
# 1. Entre na pasta do servidor
cd server

# 2. Instale dependências
npm install

# 3. Inicie o servidor
npm start

# ✅ Esperado:
# 🚀 Servidor WebSocket rodando na porta 8080
#    ws://localhost:8080
```

**Pronto!** O servidor está scrapeando dados a cada 60-90 segundos.

---

## 📱 2. App Android (KMP)

### Pré-requisitos
- Android Studio Flamingo+ ([download](https://developer.android.com/studio))
- JDK 17+ (incluído no Android Studio)
- Emulador Android API 24+

### Instalação e Execução

```bash
# 1. Entre na pasta do KMP app
cd kmp-app

# 2. Abra no Android Studio
# File → Open → Escolha pasta 'kmp-app'

# 3. Aguarde Gradle sync (automático)

# 4. Rode no emulador
# Run → Run 'app' (Shift+F10)
```

**ou via terminal:**

```bash
# Build + install
cd kmp-app && ./gradlew :androidApp:installDevDebug

# Abrir app
adb shell am start -n br.gov.caninde.transparencia.dev/.MainActivity
```

### ⚠️ Configuração da URL do Servidor

Se rodar em **dispositivo físico** (não emulador):

```kotlin
// kmp-app/shared/src/commonMain/kotlin/br/gov/caninde/transparencia/data/TransparenciaRepository.kt

companion object {
    // Substitua 10.0.2.2 pelo IP da sua máquina
    const val WS_HOST = "192.168.1.XX"  // ← SEU IP AQUI
    const val WS_PORT = 8080
}
```

Encontre seu IP:
```bash
# Windows
ipconfig

# Mac/Linux
ifconfig | grep "inet "
```

---

## 🐳 3. Servidor via Docker (opcional)

```bash
# Build + start
docker-compose up -d

# Logs
docker-compose logs -f transparencia-ws

# Stop
docker-compose down
```

---

## ✅ Checklist de Funcionamento

Após iniciar servidor e app:

- [ ] Servidor Node.js rodando (console mostra mensagens)
- [ ] App Android inicia sem erros
- [ ] App mostra "Tempo real" em verde (conexão WebSocket)
- [ ] Dados aparecem (contratos, vereadores)
- [ ] Abas funcionam (Contratos, Licitações, etc)
- [ ] Refresh atualiza dados
- [ ] Busca filtra resultados

---

## 🧪 Teste Manual (WebSocket)

```bash
# 1. Instale wscat (uma vez)
npm install -g wscat

# 2. Conecte ao servidor
wscat -c ws://localhost:8080

# 3. Cole um destes comandos:
{"type":"REQUEST_PREFEITURA"}
{"type":"REQUEST_CAMARA"}
{"type":"PING"}
{"type":"REQUEST_REFRESH","source":"prefeitura"}

# 4. Deve receber JSON em resposta
```

---

## 📚 Documentação

| Documento | Quando ler |
|-----------|-----------|
| [README.md](./README.md) | Visão geral e arquitetura |
| [API.md](./API.md) | Detalhe protocolo WebSocket |
| [DEPLOY.md](./DEPLOY.md) | Colocar em produção |
| [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) | Estrutura de arquivos |

---

## 🛠️ Troubleshooting Rápido

### ❌ "Conexão recusada ws://10.0.2.2:8080"

**Solução:**
```bash
# Terminal 1 - Verifique servidor rodando
cd server && npm start

# Terminal 2 - Teste com wscat
wscat -c ws://localhost:8080
# Deve conectar sem erro
```

### ❌ "Port 8080 already in use"

```bash
# Encontre processo
lsof -i :8080

# Mate processo
kill -9 <PID>

# Ou use porta diferente
PORT=8081 npm start
```

### ❌ "App não conecta (conexão recusada)"

1. Verifique se servidor está rodando (`npm start`)
2. Se em dispositivo físico, atualize IP em `TransparenciaRepository.kt`
3. Certifique-se firewall permite porta 8080
4. Verifique logs: `adb logcat | grep -i websocket`

### ❌ "Gradle sync falha"

```bash
# Limpe cache
./gradlew clean

# Sincronize novamente
./gradlew sync
```

---

## 🎨 Customizações Rápidas

### Mudar cores

```kotlin
// kmp-app/shared/src/commonMain/kotlin/br/gov/caninde/transparencia/presentation/Theme.kt

object AppColors {
    val Navy800   = Color(0xFF1B3A5C)  // ← Mude aqui
    val Blue500   = Color(0xFF378ADD)  // ← E aqui
    // ...
}
```

### Mudar cidades/portais

```javascript
// server/server.js (linhas 1-10)

// Substitua URLs:
const PREF_URL = 'https://novo-portal.gov.br/transparencia'
const CAMARA_URL = 'https://novo-camara.gov.br/info'

// Ajuste seletores CSS conforme novo HTML
$('table tbody tr').each((i, row) => { ... })
```

### Mudar intervalo de scraping

```javascript
// server/server.js

const PREF_INTERVAL   = 60_000;   // 60 segundos
const CAMARA_INTERVAL = 90_000;   // 90 segundos
```

---

## 📊 Estrutura Mínima para Começar

```
transparencia-caninde/
├── server/
│   ├── server.js          ← Servidor WebSocket
│   └── package.json
├── kmp-app/
│   ├── shared/            ← Código compartilhado
│   ├── androidApp/        ← App Android
│   └── build.gradle.kts
└── README.md
```

---

## 🎯 Próximos Passos Após Setup

1. **Visualizar dados reais**
   - Adaptar URLs dos portais
   - Ajustar seletores CSS do scraper
   - Testar com dados reais

2. **Customizar UI**
   - Adicionar logo Canindé
   - Ajustar paleta de cores
   - Tradução adicional

3. **Deploy**
   - Servidor: Railway, Heroku, Digital Ocean
   - App: Google Play Store (Android)
   - Domínio: transparencia.caninde.ce.gov.br

4. **Monitoramento**
   - Setup logs (Sentry, Datadog)
   - Alertas (Slack, Email)
   - Dashboard (Grafana)

---

## 💬 Precisando de Ajuda?

```
📧 Email: dev@caninde.ce.gov.br
🐛 Issues: https://github.com/seu-usuario/transparencia-caninde/issues
📱 WhatsApp: (88) 99xxx-xxxx
```

---

## ✨ Agora é sua vez!

1. **Clone/fork este projeto**
2. **Rode o servidor:** `npm start`
3. **Rode o app:** Android Studio → Run
4. **Customize:** Ajuste cores, URLs, dados
5. **Publique:** Deploy em produção

**Parabéns! Você tem um Portal de Transparência completo em tempo real! 🎉**

---

**Versão:** 1.0.0  
**Data:** 2025-05-29  
**Mantido por:** Prefeitura de Canindé, CE
