'use strict';

const DEFAULT_ALLOWED_ORIGINS = [
  'https://transparenciacaninde.com.br',
  'https://www.transparenciacaninde.com.br',
  'https://jordilucas.github.io',
];

function loadAllowedOrigins() {
  const raw = process.env.CORS_ORIGINS;
  if (!raw) return DEFAULT_ALLOWED_ORIGINS;
  return raw.split(',').map((item) => item.trim()).filter(Boolean);
}

function isLocalDevOrigin(origin) {
  if (!origin || typeof origin !== 'string') return false;
  return (
    origin.startsWith('http://localhost:') ||
    origin.startsWith('http://127.0.0.1:')
  );
}

function isAllowedOrigin(origin, allowedOrigins = loadAllowedOrigins()) {
  if (!origin) return false;
  if (allowedOrigins.includes(origin)) return true;
  return isLocalDevOrigin(origin);
}

function corsHeaders(origin, allowedOrigins = loadAllowedOrigins()) {
  if (!isAllowedOrigin(origin, allowedOrigins)) return {};
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    Vary: 'Origin',
  };
}

function handleOptionsPreflight(req, res, allowedOrigins = loadAllowedOrigins()) {
  const origin = req.headers.origin;
  const headers = corsHeaders(origin, allowedOrigins);
  if (Object.keys(headers).length === 0) {
    res.writeHead(403);
    res.end();
    return true;
  }
  res.writeHead(204, {
    ...headers,
    'Access-Control-Max-Age': '86400',
  });
  res.end();
  return true;
}

module.exports = {
  DEFAULT_ALLOWED_ORIGINS,
  loadAllowedOrigins,
  isAllowedOrigin,
  corsHeaders,
  handleOptionsPreflight,
};
