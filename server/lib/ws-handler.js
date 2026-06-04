'use strict';

function now() {
  return new Date().toISOString();
}

function parseWsMessage(raw) {
  return JSON.parse(raw.toString());
}

/**
 * Retorna descritores de resposta WS (sem I/O). O servidor resolve payload via cache/scrape.
 */
function handleWsMessage(msg) {
  const timestamp = now();

  switch (msg.type) {
    case 'REQUEST_PREFEITURA':
      return [{ type: 'PREFEITURA_DATA', timestamp, source: 'prefeitura' }];

    case 'REQUEST_CAMARA':
      return [{ type: 'CAMARA_DATA', timestamp, source: 'camara' }];

    case 'REQUEST_REFRESH': {
      const refreshSource = msg.source || 'all';
      const out = [{ type: 'REFRESHING', payload: { source: refreshSource }, timestamp }];
      const refreshPref = !refreshSource || refreshSource === 'all' || refreshSource === 'prefeitura';
      const refreshCam = !refreshSource || refreshSource === 'all' || refreshSource === 'camara';
      if (refreshPref) {
        out.push({ type: 'PREFEITURA_DATA', timestamp, source: 'prefeitura', broadcast: true, forceScrape: true });
      }
      if (refreshCam) {
        out.push({ type: 'CAMARA_DATA', timestamp, source: 'camara', broadcast: true, forceScrape: true });
      }
      return out;
    }

    case 'PING':
      return [{ type: 'PONG', timestamp }];

    case 'REQUEST_DETAIL': {
      const entity = msg.payload?.entity || msg.entity;
      const id = msg.payload?.id || msg.id || '';
      return [{ type: 'DETAIL_DATA', timestamp, entity, entityId: id, detailRequest: true }];
    }

    default:
      return [];
  }
}

function buildServerStatusPayload(cache, intervals) {
  return {
    version: '1.0.0',
    sources: [
      'https://www.caninde.ce.gov.br/acessoainformacao.php',
      'https://www.cmcaninde.ce.gov.br/caninde-transparente/',
    ],
    intervals,
    lastUpdated: cache.lastUpdated,
  };
}

function checkWsAuth(req, authToken) {
  if (!authToken) return true;
  try {
    const url = new URL(req.url || '/', 'http://localhost');
    return url.searchParams.get('token') === authToken;
  } catch {
    return false;
  }
}

module.exports = {
  now,
  parseWsMessage,
  handleWsMessage,
  buildServerStatusPayload,
  checkWsAuth,
};
