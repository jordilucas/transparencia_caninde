#!/bin/bash
# ─── Test Script para WebSocket Server ─────────────────────────────────────

# Certifique-se de que o servidor está rodando:
# npm start

# ─── Opção 1: usando wscat (recomendado) ──────────────────────────────────
# npm install -g wscat
# wscat -c ws://localhost:8080

# ─── Opção 2: usando curl (sem suporte bidirecional) ───────────────────────
# curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
#   http://localhost:8080

# ─── Opção 3: usando websocat ────────────────────────────────────────────
# cargo install websocat
# websocat ws://localhost:8080

# ────────────────────────────────────────────────────────────────────────────

# Exemplos de requisições (copie e cole no wscat)

# 1. Solicitar dados da Prefeitura
echo "Comando: Solicitar Prefeitura"
echo '{"type":"REQUEST_PREFEITURA"}'
echo ""

# 2. Solicitar dados da Câmara
echo "Comando: Solicitar Câmara"
echo '{"type":"REQUEST_CAMARA"}'
echo ""

# 3. Atualizar Prefeitura
echo "Comando: Atualizar Prefeitura"
echo '{"type":"REQUEST_REFRESH","source":"prefeitura"}'
echo ""

# 4. Atualizar Câmara
echo "Comando: Atualizar Câmara"
echo '{"type":"REQUEST_REFRESH","source":"camara"}'
echo ""

# 5. Atualizar tudo
echo "Comando: Atualizar Tudo"
echo '{"type":"REQUEST_REFRESH","source":"all"}'
echo ""

# 6. Ping (heartbeat)
echo "Comando: Ping"
echo '{"type":"PING"}'
echo ""

# ────────────────────────────────────────────────────────────────────────────

# Script de teste automático em Node.js
cat > test-client.js << 'EOF'
const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:8080');

ws.on('open', () => {
  console.log('✅ Conectado ao servidor WebSocket\n');

  // Teste 1: Solicitar Prefeitura
  console.log('📤 Enviando: REQUEST_PREFEITURA');
  ws.send(JSON.stringify({ type: 'REQUEST_PREFEITURA' }));

  setTimeout(() => {
    // Teste 2: Solicitar Câmara
    console.log('\n📤 Enviando: REQUEST_CAMARA');
    ws.send(JSON.stringify({ type: 'REQUEST_CAMARA' }));
  }, 2000);

  setTimeout(() => {
    // Teste 3: Ping
    console.log('\n📤 Enviando: PING');
    ws.send(JSON.stringify({ type: 'PING' }));
  }, 4000);

  setTimeout(() => {
    console.log('\n⏹️  Encerrando conexão...\n');
    ws.close();
  }, 6000);
});

ws.on('message', (data) => {
  try {
    const msg = JSON.parse(data);
    console.log(`\n📥 Recebido: ${msg.type}`);
    console.log(`   Payload keys: ${Object.keys(msg.payload || {}).join(', ')}`);
    console.log(`   Timestamp: ${msg.timestamp}`);
  } catch (e) {
    console.log(`\n❌ Erro ao parsear: ${e.message}`);
  }
});

ws.on('error', (err) => {
  console.error('❌ Erro WebSocket:', err.message);
});

ws.on('close', () => {
  console.log('✅ Desconectado do servidor\n');
});
EOF

echo "💾 Script de teste criado: test-client.js"
echo ""
echo "Execute com: node test-client.js"
