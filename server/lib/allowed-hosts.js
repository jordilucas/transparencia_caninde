'use strict';

/**
 * Hosts permitidos para requisições HTTP de saída (scraping).
 * Apenas portais oficiais / governamentais de Canindé e Governo Transparente.
 */
const ALLOWED_HOSTS = new Set([
  'www.caninde.ce.gov.br',
  'caninde.ce.gov.br',
  'www.cmcaninde.ce.gov.br',
  'cmcaninde.ce.gov.br',
  'www.governotransparente.com.br',
  'governotransparente.com.br',
]);

function isAllowedOutboundUrl(url) {
  if (!url || typeof url !== 'string') return false;
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== 'https:') return false;
    const host = parsed.hostname.toLowerCase();
    return ALLOWED_HOSTS.has(host);
  } catch {
    return false;
  }
}

function assertAllowedOutboundUrl(url) {
  if (!isAllowedOutboundUrl(url)) {
    throw new Error(`URL não permitida (apenas portais oficiais): ${String(url).substring(0, 120)}`);
  }
}

module.exports = {
  ALLOWED_HOSTS,
  isAllowedOutboundUrl,
  assertAllowedOutboundUrl,
};
