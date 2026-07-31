'use strict';

const { URL } = require('url');
const { isAllowedOutboundUrl } = require('./allowed-hosts');

const IMAGE_CACHE_MAX_AGE = 86_400;

function parseTargetUrl(reqUrl) {
  try {
    const parsed = new URL(reqUrl || '/', 'http://localhost');
    return parsed.searchParams.get('url');
  } catch {
    return null;
  }
}

async function handleMediaProxy(req, res, httpClient, extraHeaders = {}) {
  const target = parseTargetUrl(req.url);
  if (!target || !isAllowedOutboundUrl(target)) {
    res.writeHead(400, { 'Content-Type': 'text/plain', ...extraHeaders });
    res.end('URL não permitida');
    return;
  }

  try {
    const response = await httpClient.get(target, {
      responseType: 'arraybuffer',
      timeout: 15_000,
      headers: { Accept: 'image/*,*/*;q=0.8' },
    });
    const contentType = response.headers['content-type'] || 'application/octet-stream';
    res.writeHead(200, {
      'Content-Type': contentType,
      'Cache-Control': `public, max-age=${IMAGE_CACHE_MAX_AGE}`,
      ...extraHeaders,
    });
    res.end(Buffer.from(response.data));
  } catch (err) {
    console.warn('[Media] falha ao buscar imagem:', err?.message || err);
    res.writeHead(502, { 'Content-Type': 'text/plain', ...extraHeaders });
    res.end('Bad Gateway');
  }
}

module.exports = {
  parseTargetUrl,
  handleMediaProxy,
};
