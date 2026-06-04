'use strict';

function createRateLimiter({ windowMs, max }) {
  const hits = new Map();

  return function allow(ip) {
    const key = ip || 'unknown';
    const now = Date.now();
    let bucket = hits.get(key);

    if (!bucket || now - bucket.start >= windowMs) {
      bucket = { start: now, count: 0 };
      hits.set(key, bucket);
    }

    bucket.count += 1;
    return bucket.count <= max;
  };
}

function extractClientIp(req) {
  const forwarded = req.headers['x-forwarded-for'];
  if (forwarded) {
    return String(forwarded).split(',')[0].trim();
  }
  return req.socket?.remoteAddress || 'unknown';
}

module.exports = { createRateLimiter, extractClientIp };
