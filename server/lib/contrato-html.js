'use strict';

const DESC_START = /(?:NOVO )?CONTRATAÇÃO|AQUISIÇÃO|LOCAÇÃO|REGISTRO|CREDENCIAMENTO|REFORMA|MANUNTENÇÃO|EXERCUÇÃO/i;

function formatBRLFromRaw(raw) {
  if (raw == null || raw === '') return '';
  const s = String(raw).trim();
  if (s.includes('R$')) return s;
  const n = parseFloat(s.replace(/\./g, '').replace(',', '.'));
  if (Number.isNaN(n)) return s;
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function splitEmpresaCnpj(text) {
  const t = String(text || '').trim();
  const m = t.match(/^(.+?)\s+(\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})$/);
  if (m) return { empresa: m[1].trim(), cnpjCredor: m[2] };
  return { empresa: t, cnpjCredor: '' };
}

function splitSecretariaObjeto(text) {
  const t = String(text || '').trim();
  if (!t) return { secretaria: '', objeto: '' };
  const idx = t.search(DESC_START);
  if (idx > 0) {
    return {
      secretaria: t.substring(0, idx).trim(),
      objeto: t.substring(idx).trim(),
    };
  }
  return { secretaria: '', objeto: t };
}

function splitDataValor(text) {
  const t = String(text || '').trim();
  const glued = t.match(/^(\d{2}\/\d{2}\/\d{4})([\d.,]+)$/);
  if (glued) {
    return { data: glued[1], valor: formatBRLFromRaw(glued[2]) };
  }
  if (t.includes('R$')) return { data: '', valor: t };
  return { data: t, valor: '' };
}

function cleanNumero(text) {
  return String(text || '').replace(/\s+CONTRATO ORIGINAL\s*$/i, '').trim();
}

function formatVigencia(text) {
  const t = String(text || '').trim();
  if (!t) return '';
  return t.replace(/VIGENTE/i, 'Vigente');
}

function parseContratoHtmlRow(cols, link) {
  if (!cols || cols.length < 3) return null;
  const numero = cleanNumero(cols[0]);
  const { empresa, cnpjCredor } = splitEmpresaCnpj(cols[1]);
  const { secretaria, objeto } = splitSecretariaObjeto(cols[2]);
  const { data, valor } = splitDataValor(cols[3]);
  const vigencia = formatVigencia(cols[4]);
  const dataLabel = [data, vigencia].filter(Boolean).join(' · ');

  return {
    numero,
    secretaria,
    objeto,
    valor,
    empresa,
    cnpjCredor,
    data: dataLabel,
    url: link || '',
  };
}

module.exports = {
  DESC_START,
  formatBRLFromRaw,
  splitEmpresaCnpj,
  splitSecretariaObjeto,
  splitDataValor,
  parseContratoHtmlRow,
};
