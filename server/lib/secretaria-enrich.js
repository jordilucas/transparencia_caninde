'use strict';

function normalizeText(str) {
  if (!str || typeof str !== 'string') return '';
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

function buildMatchers(secretarias) {
  return secretarias.map((s) => {
    const nome = String(s.nome || '').trim();
    const normalized = normalizeText(nome);
    const aliases = new Set([normalized]);
    const patterns = [
      /^secretaria municipal da?\s+/,
      /^secretaria municipal de\s+/,
      /^secretaria de\s+/,
      /^fundacao municipal de\s+/,
      /^fundacao de\s+/,
    ];
    let stripped = normalized;
    for (const re of patterns) {
      stripped = stripped.replace(re, '').trim();
    }
    if (stripped && stripped.length >= 3) aliases.add(stripped);
    const words = stripped.split(' ').filter((w) => w.length >= 5);
    if (words[0]) aliases.add(words[0]);
    return { id: String(s.id), nome, aliases: [...aliases] };
  });
}

function matchByText(text, matchers) {
  const t = normalizeText(text);
  if (!t) return null;
  let best = null;
  let bestLen = 0;
  for (const m of matchers) {
    const mn = normalizeText(m.nome);
    if (t === mn || (mn.length >= 8 && t.includes(mn))) {
      if (mn.length > bestLen) {
        best = m.id;
        bestLen = mn.length;
      }
    }
    for (const alias of m.aliases) {
      if (alias.length >= 5 && t.includes(alias) && alias.length > bestLen) {
        best = m.id;
        bestLen = alias.length;
      }
    }
  }
  return best;
}

function parseBRLNumber(value) {
  if (value == null || value === '') return 0;
  if (typeof value === 'number' && !Number.isNaN(value)) return value;
  const s = String(value).trim();
  if (!s) return 0;
  const cleaned = s.replace(/[^\d,.-]/g, '').replace(/\./g, '').replace(',', '.');
  const n = parseFloat(cleaned);
  return Number.isNaN(n) ? 0 : n;
}

function formatBRL(value) {
  const n = typeof value === 'number' ? value : parseBRLNumber(value);
  if (!n) return '';
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function isLicitacaoEmAndamento(lic) {
  const sit = normalizeText(lic.situacao || '');
  if (!sit || sit === 'em andamento' || sit === 'aberta' || sit === 'em abertura') return true;
  if (/homolog|revog|desert|cancel|encerr|finaliz|conclu|suspens/.test(sit)) return false;
  return true;
}

function isContratoEmAndamento(contrato) {
  const vigencia = String(contrato.vigenciaFim || contrato.data || '').trim();
  if (!vigencia) return true;
  const m = vigencia.match(/(\d{1,2})\/(\d{1,2})\/(\d{4})/);
  if (!m) return true;
  const end = new Date(+m[3], +m[2] - 1, +m[1]);
  return end.getTime() >= Date.now();
}

function toProjetoFromLicitacao(l) {
  return {
    titulo: (l.objeto || l.numero || 'Licitação').substring(0, 200),
    tipo: 'Licitação',
    situacao: (l.situacao || 'Em andamento').trim() || 'Em andamento',
    valor: '',
    url: l.url || '',
    numero: l.numero || '',
  };
}

function toProjetoFromContrato(c) {
  return {
    titulo: (c.objeto || c.numero || 'Contrato').substring(0, 200),
    tipo: 'Contrato',
    situacao: 'Vigente',
    valor: c.valor || '',
    url: c.url || c.pdfUrl || '',
    numero: c.numero || '',
  };
}

function emptyResumo() {
  return {
    totalContratos: 0,
    totalLicitacoes: 0,
    totalProjetosAndamento: 0,
    totalGastos: '',
  };
}

function enrichSecretarias(secretarias, contratos = [], licitacoes = []) {
  if (!secretarias?.length) return [];

  const matchers = buildMatchers(secretarias);
  const buckets = new Map(
    secretarias.map((s) => [
      String(s.id),
      {
        ...s,
        resumoFinanceiro: emptyResumo(),
        contratos: [],
        licitacoes: [],
        projetosAndamento: [],
        _gastosNum: 0,
      },
    ]),
  );

  for (const contrato of contratos) {
    const secId = matchByText(contrato.secretaria, matchers);
    if (!secId || !buckets.has(secId)) continue;
    const bucket = buckets.get(secId);
    bucket.contratos.push(contrato);
    bucket._gastosNum += parseBRLNumber(contrato.valorNumerico ?? contrato.valor);
    if (isContratoEmAndamento(contrato)) {
      bucket.projetosAndamento.push(toProjetoFromContrato(contrato));
    }
  }

  for (const lic of licitacoes) {
    const secId = matchByText(lic.objeto, matchers);
    if (!secId || !buckets.has(secId)) continue;
    const bucket = buckets.get(secId);
    bucket.licitacoes.push(lic);
    if (isLicitacaoEmAndamento(lic)) {
      bucket.projetosAndamento.push(toProjetoFromLicitacao(lic));
    }
  }

  const MAX_ITEMS = 12;
  const MAX_PROJETOS = 8;

  return secretarias.map((s) => {
    const bucket = buckets.get(String(s.id)) || { ...s, ...emptyResumo(), contratos: [], licitacoes: [], projetosAndamento: [] };
    const totalContratos = bucket.contratos.length;
    const totalLicitacoes = bucket.licitacoes.length;
    const totalProjetosAndamento = bucket.projetosAndamento.length;
    const { _gastosNum, ...rest } = bucket;
    return {
      ...rest,
      resumoFinanceiro: {
        totalContratos,
        totalLicitacoes,
        totalProjetosAndamento,
        totalGastos: _gastosNum > 0 ? formatBRL(_gastosNum) : '',
      },
      contratos: bucket.contratos.slice(0, MAX_ITEMS),
      licitacoes: bucket.licitacoes.slice(0, MAX_ITEMS),
      projetosAndamento: bucket.projetosAndamento.slice(0, MAX_PROJETOS),
    };
  });
}

module.exports = {
  normalizeText,
  buildMatchers,
  matchByText,
  parseBRLNumber,
  formatBRL,
  isLicitacaoEmAndamento,
  isContratoEmAndamento,
  enrichSecretarias,
};
