'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const EXPORT_URL = `${BASE}/dadosabertosexportar.php`;

function resolveUrl(href) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${BASE}${t.startsWith('/') ? '' : '/'}${t}`;
}

function formatBRL(value) {
  if (value == null || value === '') return '';
  const n = typeof value === 'number' ? value : parseFloat(String(value).replace(/\./g, '').replace(',', '.'));
  if (Number.isNaN(n)) return String(value);
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function isEmptyResponse(body) {
  if (!body || typeof body !== 'string') return true;
  return body.includes('Não há registros') || body.includes('<SCRIPT');
}

async function fetchDataset(http, dataset, ano) {
  const year = ano || new Date().getFullYear();
  const params = new URLSearchParams({ d: dataset, a: String(year), f: 'json' });
  const { data } = await http.get(`${EXPORT_URL}?${params.toString()}`, {
    responseType: 'text',
    transformResponse: [(r) => r],
  });
  const text = typeof data === 'string' ? data : String(data);
  if (isEmptyResponse(text)) return [];
  try {
    const parsed = JSON.parse(text);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function mapContratos(rows) {
  return rows.map((r) => {
    const pdf = resolveUrl(r.Arquivo || r.DemaisArquivos || '');
    return {
      id: String(r.Id ?? ''),
      numero: String(r.NumeroContrato || r.NumeroProcesso || r.Id || '').trim(),
      objeto: String(r.Objeto || '').trim().substring(0, 300),
      valor: formatBRL(r.ValorGlobal) || String(r.ValorGlobal || '').trim(),
      empresa: String(r.NomeCredor || '').trim(),
      data: String(r.DataContrato || r.VigenciaInicio || '').trim(),
      cnpjCredor: String(r.CNPJCPF || '').trim(),
      secretaria: String(r.Secretaria || '').trim(),
      modalidade: String(r.Modalidade || '').trim(),
      url: resolveUrl(r.Url || ''),
      pdfUrl: pdf,
    };
  }).filter((c) => c.numero || c.objeto);
}

function mapLicitacoes(rows) {
  const { formatDateBR } = require('./licitacao-html');
  return rows.map((r) => ({
    id: String(r.Id ?? ''),
    numero: String(r.NumeroPrecesso || r.NumeroEdital || r.Id || '').trim(),
    modalidade: String(r.Modalidade || r.Tipo || '').trim(),
    objeto: String(r.Objeto || '').trim().substring(0, 300),
    situacao: String(r.Situacao || 'Em andamento').trim() || 'Em andamento',
    dataAbertura: formatDateBR(r.DataAbertura || ''),
    url: resolveUrl(r.Url || ''),
  })).filter((l) => l.numero || l.objeto);
}

function mapSecretarias(rows) {
  return rows.map((r) => ({
    id: String(r.Id ?? ''),
    nome: String(r.Secretaria || '').trim(),
    secretario: String(r.Gestor || '').trim(),
    url: `${BASE}/secretaria.php?sec=${r.Id || ''}`,
    contato: {
      email: String(r.Email || '').trim(),
      telefone: String(r.Telefone1 || r.Telefone2 || '').trim(),
      whatsapp: '',
      endereco: '',
      horarioFuncionamento: String(r.HorarioFunciona || '').trim(),
    },
  })).filter((s) => s.nome);
}

function mapPublicacoes(rows) {
  return rows.map((r) => ({
    id: String(r.Id ?? ''),
    titulo: String(r.Descricao || r.TipoArquivo || 'Publicação').trim().substring(0, 200),
    tipo: String(r.TipoArquivo || '').trim(),
    data: String(r.Data || '').trim(),
    url: resolveUrl(r.Url || ''),
  })).filter((p) => p.titulo);
}

async function scrapePrefeituraDadosAbertos(http, ano) {
  const year = ano || new Date().getFullYear();

  const [licSettled, contSettled, secSettled, pubSettled] = await Promise.allSettled([
    fetchDataset(http, 'licitacoes', year),
    fetchDataset(http, 'contratos', year),
    fetchDataset(http, 'secretarias', year),
    fetchDataset(http, 'publicacoes', year),
  ]);

  const licRows = licSettled.status === 'fulfilled' ? licSettled.value : [];
  const contRows = contSettled.status === 'fulfilled' ? contSettled.value : [];
  const secRows = secSettled.status === 'fulfilled' ? secSettled.value : [];
  const pubRows = pubSettled.status === 'fulfilled' ? pubSettled.value : [];

  if (licSettled.status === 'rejected') {
    console.warn('[DadosAbertos] licitações indisponível:', licSettled.reason?.message || licSettled.reason);
  }
  if (secSettled.status === 'rejected') {
    console.warn('[DadosAbertos] secretarias indisponível:', secSettled.reason?.message || secSettled.reason);
  }

  const licitacoes = mapLicitacoes(licRows);
  const contratos = mapContratos(contRows);
  const secretarias = mapSecretarias(secRows);
  const publicacoes = mapPublicacoes(pubRows);

  return {
    contratos,
    licitacoes,
    secretarias,
    publicacoes,
    fonte: `${EXPORT_URL} (dados abertos JSON, exercício ${year})`,
    dataSource: 'dadosabertos',
  };
}

module.exports = {
  BASE,
  EXPORT_URL,
  fetchDataset,
  isEmptyResponse,
  mapContratos,
  mapLicitacoes,
  mapSecretarias,
  mapPublicacoes,
  scrapePrefeituraDadosAbertos,
};
