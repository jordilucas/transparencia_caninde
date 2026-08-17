'use strict';

/** Converte ValorGlobal do portal (número JSON, "2.004.257,55" ou "2004257.55") para float. */
function parseBRLNumber(value) {
  if (value == null || value === '') return 0;
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  const s = String(value).trim().replace(/[^\d,.-]/g, '');
  if (!s) return 0;
  if (s.includes(',')) {
    const n = parseFloat(s.replace(/\./g, '').replace(',', '.'));
    return Number.isNaN(n) ? 0 : n;
  }
  const dotCount = (s.match(/\./g) || []).length;
  if (dotCount > 1) {
    const n = parseFloat(s.replace(/\./g, ''));
    return Number.isNaN(n) ? 0 : n;
  }
  const n = parseFloat(s);
  return Number.isNaN(n) ? 0 : n;
}

function formatBRL(value) {
  const n = parseBRLNumber(value);
  if (!n) return '';
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

module.exports = { parseBRLNumber, formatBRL };
