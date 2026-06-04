# 🚀 Guia de Deploy para Produção

## Servidor WebSocket (Node.js)

### Opção 1: Docker (Recomendado)

#### Pré-requisitos
- Docker 20+
- Docker Compose 2+

#### Deploy

```bash
# Build da imagem
docker build -t transparencia-caninde-ws:1.0.0 ./server

# Rodar container
docker run -d \
  --name transparencia-ws \
  -p 8080:8080 \
  -e NODE_ENV=production \
  -v /path/to/logs:/app/logs \
  --restart unless-stopped \
  transparencia-caninde-ws:1.0.0

# Ou com Docker Compose
docker-compose up -d
```

#### Verificar Status

```bash
docker ps | grep transparencia
docker logs transparencia-ws
docker exec transparencia-ws ps aux
```

---

### Opção 2: PM2 (Node Process Manager)

#### Instalação

```bash
npm install -g pm2
pm2 install pm2-logrotate  # Rotação de logs
```

#### Start

```bash
cd server
npm ci --only=production
pm2 start server.js \
  --name "transparencia-ws" \
  --instances 2 \
  --exec-mode cluster \
  --env "NODE_ENV=production"

pm2 save
pm2 startup
```

#### Monitoramento

```bash
pm2 monit
pm2 logs transparencia-ws
pm2 status
```

---

### Opção 3: Systemd (Linux)

#### Criar arquivo de serviço

```bash
sudo nano /etc/systemd/system/transparencia-ws.service
```

Conteúdo:

```ini
[Unit]
Description=Portal da Transparência - WebSocket Server
After=network.target

[Service]
Type=simple
User=nodejs
WorkingDirectory=/home/nodejs/transparencia-caninde/server
ExecStart=/usr/bin/node server.js
Restart=always
RestartSec=10
StandardOutput=append:/var/log/transparencia-ws.log
StandardError=append:/var/log/transparencia-ws-error.log
Environment="NODE_ENV=production"
Environment="PORT=8080"

[Install]
WantedBy=multi-user.target
```

#### Carregar e iniciar

```bash
sudo systemctl daemon-reload
sudo systemctl enable transparencia-ws
sudo systemctl start transparencia-ws
sudo systemctl status transparencia-ws
```

---

## Configuração de Proxy (Nginx/Apache)

### Nginx (Recomendado)

```nginx
upstream transparencia_ws {
    server localhost:8080;
}

server {
    listen 80;
    server_name transparencia.caninde.ce.gov.br;
    client_max_body_size 10M;

    # Redirect para HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name transparencia.caninde.ce.gov.br;

    ssl_certificate /etc/ssl/certs/caninde.crt;
    ssl_certificate_key /etc/ssl/private/caninde.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # WebSocket
    location / {
        proxy_pass http://transparencia_ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_read_timeout 86400;
        proxy_send_timeout 86400;
        proxy_connect_timeout 7d;
    }

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
    limit_req zone=api_limit burst=20 nodelay;
}
```

### Apache

```apache
<VirtualHost *:443>
    ServerName transparencia.caninde.ce.gov.br
    
    SSLEngine on
    SSLCertificateFile /etc/ssl/certs/caninde.crt
    SSLCertificateKeyFile /etc/ssl/private/caninde.key

    # WebSocket proxy
    ProxyPreserveHost On
    ProxyPass / ws://localhost:8080/ upgrade=websocket connectiontimeout=7200 timeout=7200
    ProxyPassReverse / ws://localhost:8080/

    RequestHeader set X-Forwarded-Proto "https"
</VirtualHost>

<VirtualHost *:80>
    ServerName transparencia.caninde.ce.gov.br
    Redirect permanent / https://transparencia.caninde.ce.gov.br/
</VirtualHost>
```

---

## Certificados SSL

### Let's Encrypt (Certbot)

```bash
# Instalação
sudo apt-get install certbot python3-certbot-nginx

# Gerar certificado
sudo certbot certonly --standalone \
  -d transparencia.caninde.ce.gov.br \
  -d www.transparencia.caninde.ce.gov.br \
  --email admin@caninde.ce.gov.br

# Auto-renovação
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

---

## Monitoramento e Logs

### Setup ELK Stack (Elasticsearch + Logstash + Kibana)

```yaml
# docker-compose.yml (adições)
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
  environment:
    discovery.type: single-node
  ports:
    - "9200:9200"

kibana:
  image: docker.elastic.co/kibana/kibana:8.0.0
  ports:
    - "5601:5601"

# Node.js logs → Elasticsearch (integração)
# npm install @elastic/elasticsearch
```

### Prometheus + Grafana (Métricas)

```bash
# Instalar Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.30.0/prometheus-2.30.0.linux-amd64.tar.gz
tar xvfz prometheus-2.30.0.linux-amd64.tar.gz

# prometheus.yml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: 'transparencia-ws'
    static_configs:
      - targets: ['localhost:9090']
```

---

## Backup e Recuperação

### Backup do Servidor

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backups/transparencia"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup da aplicação
tar -czf $BACKUP_DIR/server_$DATE.tar.gz \
  /home/nodejs/transparencia-caninde/server

# Backup de logs
tar -czf $BACKUP_DIR/logs_$DATE.tar.gz \
  /var/log/transparencia-ws*

# Remover backups antigos (> 30 dias)
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete

# Salvar em storage externo (AWS S3, Google Cloud, etc)
# aws s3 cp $BACKUP_DIR s3://seu-bucket/backups/
```

Adicione ao cron:

```bash
0 2 * * * /path/to/backup.sh  # Executar diariamente às 2AM
```

---

## Variáveis de Ambiente

### .env para Produção

```bash
# Server
NODE_ENV=production
PORT=8080
LOG_LEVEL=info

# Scraping
PREF_INTERVAL=60000    # 60s
CAMARA_INTERVAL=90000  # 90s
SCRAPE_TIMEOUT=15000   # 15s

# URLs de origem (pode customizar)
PREF_URL=https://www.caninde.ce.gov.br/acessoainformacao.php
CAMARA_URL=https://www.cmcaninde.ce.gov.br/caninde-transparente/

# Rate limiting
MAX_CLIENTS=1000
PING_INTERVAL=20000

# Notificações (opcional)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
ALERT_ON_ERROR=true
```

---

## Checklist de Deploy

- [ ] Servidor Node.js rodando (verificar `npm start`)
- [ ] Porta 8080 liberada no firewall
- [ ] Proxy reverso configurado (Nginx/Apache)
- [ ] Certificado SSL válido
- [ ] HTTPS redirecionando corretamente
- [ ] WebSocket testado com wscat/client
- [ ] Logs sendo gerados `/var/log/transparencia-ws*`
- [ ] PM2/Docker em modo restart automático
- [ ] Backup configurado (cron job)
- [ ] Monitoramento ativo (PM2, Prometheus, etc)
- [ ] Alertas configurados
- [ ] DNS apontando para o servidor
- [ ] Documentação de runbook criada

---

## Troubleshooting Produção

### Erro: "EADDRINUSE: address already in use :::8080"

```bash
# Encontrar processo na porta 8080
sudo lsof -i :8080

# Matar processo
sudo kill -9 <PID>

# Ou usar porta diferente
PORT=8081 node server.js
```

### Erro: "Cannot find module 'ws'"

```bash
cd /home/nodejs/transparencia-caninde/server
npm ci --only=production
```

### Erro: "OutOfMemoryError" no Node

```bash
# Aumentar heap
node --max-old-space-size=4096 server.js

# Ou no PM2
pm2 start server.js --max-memory-restart 1G
```

### WebSocket desconecta frequentemente

```javascript
// Client side: aumentar timeout
proxy_read_timeout 86400;
proxy_send_timeout 86400;

// Server side: manter vivo com PING mais frequente
pingInterval = 10_000  // 10s em vez de 20s
```

---

## Performance Tuning

### Node.js Cluster Mode (PM2)

```bash
pm2 start server.js -i max  # Usar todos os CPU cores
```

### Nginx Tuning

```nginx
worker_processes auto;
worker_connections 4096;

upstream transparencia_ws {
    least_conn;  # Load balancing
    server localhost:8080;
    server localhost:8081;
    server localhost:8082;
}
```

### Limpar logs antigos

```bash
# Logrotate
cat > /etc/logrotate.d/transparencia << EOF
/var/log/transparencia-ws*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
}
EOF
```

---

## Roadmap de Melhorias Futuras

- [ ] **Cache distribuído:** Redis para cache de dados entre instâncias
- [ ] **Banco de dados:** PostgreSQL para histórico de dados
- [ ] **GraphQL API:** Alternativa ao REST para queries complexas
- [ ] **Mobile push notifications:** Alertas de atualizações importantes
- [ ] **Analytics:** Dashboard de uso e estatísticas
- [ ] **Multi-idioma:** Suporte a EN/ES além de PT

---

**Documento atualizado:** 2025-05-29
