/**
 * Servidor WebSocket — Portal da Transparência de Canindé
 * Estratégia: HTTP scraping periódico dos portais públicos,
 * dados distribuídos em tempo real via WebSocket para os clientes KMP.
 *
 * Fontes:
 *  - Prefeitura : https://www.caninde.ce.gov.br  (+ governotransparente.com.br id=11979490)
 *  - Câmara     : https://www.cmcaninde.ce.gov.br (+ governotransparente.com.br id=11979588)
 */

const WebSocket = require('ws');
const axios = require('axios');
const cheerio = require('cheerio');

const { config, httpAgent } = require('./lib/config');
const scrapeResult = require('./lib/scrape-result');
const { createRateLimiter, extractClientIp } = require('./lib/rate-limit');
const wsHandler = require('./lib/ws-handler');
const scraperCamara = require('./lib/scraper-camara');

const PORT = config.port;
const PREF_INTERVAL = config.prefInterval;
const CAMARA_INTERVAL = config.camaraInterval;

const rateLimit = createRateLimiter({
  windowMs: config.rateLimitWindowMs,
  max: config.rateLimitMax,
});

const http = axios.create({
  timeout: 15_000,
  httpsAgent: httpAgent,
  headers: {
    'User-Agent': 'Mozilla/5.0 (compatible; TransparenciaBot/1.0)',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'pt-BR,pt;q=0.9',
  }
});

// ─── estado em memória ───────────────────────────────────────────────────────
let cache = {
  prefeitura: null,
  camara: null,
  lastUpdated: { prefeitura: null, camara: null }
};

// ─── helpers ─────────────────────────────────────────────────────────────────
function parseBRL(str) {
  if (!str) return 0;
  return parseFloat(str.replace(/[^\d,]/g, '').replace(',', '.')) || 0;
}

function now() { return wsHandler.now(); }

function broadcast(wss, type, payload) {
  const msg = JSON.stringify({ type, payload, timestamp: now() });
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) client.send(msg);
  });
}

// ─── scraper: Prefeitura ─────────────────────────────────────────────────────
async function scrapePrefeitura() {
  console.log('[Prefeitura] iniciando scraping...');
  try {
    // 1. Página principal de transparência
    const { data: htmlMain } = await http.get('https://www.caninde.ce.gov.br/acessoainformacao.php');
    const $m = cheerio.load(htmlMain);

    // 2. Contratos
    const { data: htmlContratos } = await http.get('https://www.caninde.ce.gov.br/contratos.php');
    const $c = cheerio.load(htmlContratos);

    const contratos = [];
    $c('table tbody tr').each((i, row) => {
      if (i >= 10) return false;
      const cols = $c(row).find('td');
      if (cols.length >= 3) {
        contratos.push({
          numero:    $c(cols[0]).text().trim(),
          objeto:    $c(cols[1]).text().trim().substring(0, 80),
          valor:     $c(cols[2]).text().trim(),
          empresa:   $c(cols[3])?.text().trim() || '',
          data:      $c(cols[4])?.text().trim() || '',
        });
      }
    });

    // 3. Licitações
    const { data: htmlLicit } = await http.get('https://www.caninde.ce.gov.br/licitacao.php');
    const $l = cheerio.load(htmlLicit);
    const licitacoes = [];
    $l('table tbody tr').each((i, row) => {
      if (i >= 8) return false;
      const cols = $l(row).find('td');
      if (cols.length >= 2) {
        licitacoes.push({
          numero:    $l(cols[0]).text().trim(),
          modalidade:$l(cols[1]).text().trim(),
          objeto:    $l(cols[2])?.text().trim().substring(0, 80) || '',
          situacao:  $l(cols[3])?.text().trim() || '',
        });
      }
    });

    // 4. Diário Oficial (últimas publicações)
    const { data: htmlDiario } = await http.get('https://www.caninde.ce.gov.br/diariolista.php');
    const $d = cheerio.load(htmlDiario);
    const diarios = [];
    $d('table tbody tr, .lista-publicacoes li, ul.publicacoes li').each((i, el) => {
      if (i >= 5) return false;
      const txt = $d(el).text().trim();
      if (txt.length > 5) diarios.push(txt.substring(0, 120));
    });

    // 5. Secretarias (estrutura organizacional)
    const secretarias = [];
    $m('a[href*="secretaria.php?sec="]').each((i, el) => {
      secretarias.push($m(el).text().trim());
    });

    const result = {
      ...scrapeResult.buildPrefeituraPayload({
        contratos,
        licitacoes,
        diariosOficiais: diarios,
        secretarias: secretarias.slice(0, 15),
        fonte: 'https://www.caninde.ce.gov.br/acessoainformacao.php',
      }),
      scrapedAt: now(),
    };

    cache.prefeitura = result;
    cache.lastUpdated.prefeitura = now();
    console.log(`[Prefeitura] OK — ${contratos.length} contratos, ${licitacoes.length} licitações`);
    if (result.error) console.warn(`[Prefeitura] aviso: ${result.error}`);
    return result;
  } catch (err) {
    console.error('[Prefeitura] erro no scraping:', err.message);
    if (cache.prefeitura) return cache.prefeitura;
    return { ...scrapeResult.basicPrefeituraShell(err.message), scrapedAt: now() };
  }
}

// ─── scraper: Câmara Municipal de Canindé/CE (cmcaninde.ce.gov.br) ───────────
async function scrapeCamara() {
  console.log('[Câmara] iniciando scraping (Canindé/CE)...');
  try {
    const { data: htmlParl } = await http.get(`${scraperCamara.BASE}/parlamentares/`);
    const parlamentares = scraperCamara.scrapeParlamentaresFromHtml(htmlParl, cheerio);
    const mesaDiretora = scraperCamara.scrapeMesaDiretoraFromHtml(htmlParl, cheerio);

    const { data: htmlSessoes } = await http.get(`${scraperCamara.BASE}/sessoes/`);
    const sessoes = scraperCamara.scrapeSessoesFromHtml(htmlSessoes, cheerio);

    const { data: htmlMat } = await http.get(`${scraperCamara.BASE}/materias/`);
    const materias = scraperCamara.scrapeMateriasFromHtml(htmlMat, cheerio);

    const result = {
      ...scrapeResult.buildCamaraPayload({
        parlamentares,
        sessoes,
        materias,
        mesaDiretora,
        fonte: `${scraperCamara.BASE}/parlamentares/`,
      }),
      scrapedAt: now(),
    };

    cache.camara = result;
    cache.lastUpdated.camara = now();
    console.log(`[Câmara] OK — ${parlamentares.length} parlamentares, ${sessoes.length} sessões`);
    if (result.error) console.warn(`[Câmara] aviso: ${result.error}`);
    else if (parlamentares.length > 0) {
      console.log(`[Câmara] vereadores: ${parlamentares.map((p) => p.nome).join(', ')}`);
    }
    return result;
  } catch (err) {
    console.error('[Câmara] erro no scraping:', err.message);
    if (cache.camara) return cache.camara;
    return { ...scrapeResult.basicCamaraShell(err.message), scrapedAt: now() };
  }
}

async function payloadForSource(source, forceScrape) {
  if (source === 'prefeitura') {
    if (forceScrape || !cache.prefeitura) return scrapePrefeitura();
    return cache.prefeitura;
  }
  if (source === 'camara') {
    if (forceScrape || !cache.camara) return scrapeCamara();
    return cache.camara;
  }
  return null;
}

// ─── WebSocket server ─────────────────────────────────────────────────────────
const wss = new WebSocket.Server({ port: PORT });

wss.on('connection', (ws, req) => {
  const ip = extractClientIp(req);

  if (!wsHandler.checkWsAuth(req, config.wsAuthToken)) {
    ws.close(4001, 'Unauthorized');
    console.warn(`[WS] conexão recusada (auth): ${ip}`);
    return;
  }

  if (!rateLimit(ip)) {
    ws.close(4029, 'Too Many Requests');
    console.warn(`[WS] rate limit: ${ip}`);
    return;
  }

  console.log(`[WS] cliente conectado: ${ip}  total: ${wss.clients.size}`);

  if (cache.prefeitura) {
    ws.send(JSON.stringify({ type: 'PREFEITURA_DATA', payload: cache.prefeitura, timestamp: now() }));
  }
  if (cache.camara) {
    ws.send(JSON.stringify({ type: 'CAMARA_DATA', payload: cache.camara, timestamp: now() }));
  }

  ws.send(JSON.stringify({
    type: 'SERVER_STATUS',
    payload: wsHandler.buildServerStatusPayload(cache, {
      prefeitura: PREF_INTERVAL,
      camara: CAMARA_INTERVAL,
    }),
    timestamp: now(),
  }));

  ws.on('message', async (raw) => {
    if (!rateLimit(ip)) {
      ws.send(JSON.stringify({
        type: 'ERROR',
        payload: { message: 'Rate limit excedido' },
        timestamp: now(),
      }));
      return;
    }

    try {
      const msg = wsHandler.parseWsMessage(raw);
      console.log(`[WS] mensagem recebida: ${msg.type}`);

      const responses = wsHandler.handleWsMessage(msg);
      for (const r of responses) {
        let payload = r.payload;
        if (r.source) {
          payload = await payloadForSource(r.source, r.forceScrape);
        }
        const envelope = { type: r.type, timestamp: r.timestamp || now() };
        if (payload !== undefined) envelope.payload = payload;

        if (r.broadcast) {
          broadcast(wss, r.type, payload);
        } else {
          ws.send(JSON.stringify(envelope));
        }
      }
    } catch (e) {
      ws.send(JSON.stringify({ type: 'ERROR', payload: { message: e.message }, timestamp: now() }));
    }
  });

  ws.on('close', () => console.log(`[WS] cliente desconectado. total: ${wss.clients.size}`));
  ws.on('error', (e) => console.error('[WS] erro no cliente:', e.message));
});

// ─── ciclos periódicos de scraping ───────────────────────────────────────────
async function initAndSchedule() {
  console.log('[Init] fazendo scraping inicial...');
  const [p, c] = await Promise.allSettled([scrapePrefeitura(), scrapeCamara()]);
  if (p.status === 'fulfilled') cache.prefeitura = p.value;
  if (c.status === 'fulfilled') cache.camara = c.value;
  console.log('[Init] scraping inicial concluído.');

  setInterval(async () => {
    const data = await scrapePrefeitura();
    if (data) broadcast(wss, 'PREFEITURA_DATA', data);
  }, PREF_INTERVAL);

  setInterval(async () => {
    const data = await scrapeCamara();
    if (data) broadcast(wss, 'CAMARA_DATA', data);
  }, CAMARA_INTERVAL);
}

wss.on('listening', () => {
  console.log(`\n🚀 Servidor WebSocket rodando na porta ${PORT}`);
  console.log(`   ws://localhost:${PORT}`);
  console.log(`   NODE_ENV=${process.env.NODE_ENV || 'development'} TLS verify=${config.isProduction}`);
  if (config.wsAuthToken) console.log('   Autenticação WS: token obrigatório (?token=...)');
  console.log('');
  initAndSchedule();
});

wss.on('error', (err) => console.error('[WS Server] erro:', err));
