'use strict';

const https = require('https');

const isProduction = process.env.NODE_ENV === 'production';

const config = {
  port: parseInt(process.env.PORT || '8080', 10),
  prefInterval: 60_000,
  camaraInterval: 90_000,
  wsAuthToken: process.env.WS_AUTH_TOKEN || '',
  rateLimitWindowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000', 10),
  rateLimitMax: parseInt(process.env.RATE_LIMIT_MAX || '120', 10),
  isProduction,
};

const httpAgent = new https.Agent({
  rejectUnauthorized: isProduction,
});

module.exports = { config, httpAgent };
