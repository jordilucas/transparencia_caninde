'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const EXPORT_URL = `${BASE}/dadosabertosexportar.php`;
const CANINDE_REFERER = `${BASE}/acessoainformacao.php`;
const { parseBRLNumber, formatBRL } = require('./brl');
const { sleep } = require('./scrape-guard');

function resolveUrl(href) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${BASE}${t.startsWith('/') ? '' : '/'}${t}`;
}

function isEmptyResponse(body) {
  if (!body || typeof body !== 'string') return true;
  return body.includes('Não há registros') || body.includes('<SCRIPT');
}

function looksLikeHtml(body) {
  const text = String(body || '').trimStart().slice(0, 32).toLowerCase();
  return text.startsWith('<!doctype') || text.startsWith('<html') || text.startsWith('<!');
}

function buildExportRequestConfig() {
  return {
    responseType: 'text',
    transformResponse: [(r) => r],
    headers: {
      Referer: CANINDE_REFERER,
      Accept: 'application/json,text/plain,*/*',
    },
    validateStatus: (status) => status >= 200 && status < 500,
  };
}

async function fetchDataset(http, dataset, ano, attempt = 1) {
  const params = new URLSearchParams({ d: dataset, f: 'json' });
  if (dataset !== 'secretarias') {
    params.set('a', String(ano || new Date().getFullYear()));
  }
  const url = `${EXPORT_URL}?${params.toString()}`;

  try {
    const { data, status } = await http.get(url, buildExportRequestConfig());
    const text = typeof data === 'string' ? data : String(data);

    if (status >= 400) {
      throw new Error(`HTTP ${status}`);
    }
    if (looksLikeHtml(text)) {
      throw new Error(`resposta HTML (${text.length} bytes)`);
    }
    if (isEmptyResponse(text)) return [];

    const parsed = JSON.parse(text);
    return Array.isArray(parsed) ? parsed : [];
  } catch (err) {
    if (attempt < 3) {
      console.warn(
        `[DadosAbertos] ${dataset} tentativa ${attempt} falhou:`,
        err.message || err,
      );
      await sleep(350 * attempt);
      return fetchDataset(http, dataset, ano, attempt + 1);
    }
    throw err;
  }
}

function mapContratos(rows) {
  return rows.map((r) => {
    const pdf = resolveUrl(r.Arquivo || r.DemaisArquivos || '');
    const valorNumerico = parseBRLNumber(r.ValorGlobal);
    return {
      id: String(r.Id ?? ''),
      numero: String(r.NumeroContrato || r.NumeroProcesso || r.Id || '').trim(),
      objeto: String(r.Objeto || '').trim().substring(0, 300),
      valor: formatBRL(r.ValorGlobal) || String(r.ValorGlobal || '').trim(),
      valorNumerico,
      empresa: String(r.NomeCredor || '').trim(),
      data: String(r.DataContrato || r.VigenciaInicio || '').trim(),
      vigenciaFim: String(r.VigenciaFim || '').trim(),
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
  return rows.map((r) => {
    const rua = String(r.Rua || '').trim();
    const numero = String(r.Numero || '').trim();
    const bairro = String(r.Bairro || '').trim();
    const enderecoParts = [rua, numero, bairro].filter(Boolean);
    return {
      id: String(r.Id ?? ''),
      nome: String(r.Secretaria || '').trim(),
      secretario: String(r.Gestor || '').trim(),
      cargoGestor: String(r.Cargo || '').trim(),
      url: `${BASE}/secretaria.php?sec=${r.Id || ''}`,
      contato: {
        email: String(r.Email || '').trim(),
        telefone: String(r.Telefone1 || r.Telefone2 || '').trim(),
        whatsapp: '',
        endereco: enderecoParts.join(', '),
        horarioFuncionamento: String(r.HorarioFunciona || '').trim(),
      },
      resumoFinanceiro: {
        totalContratos: 0,
        totalLicitacoes: 0,
        totalProjetosAndamento: 0,
        totalGastos: '',
      },
      contratos: [],
      licitacoes: [],
      projetosAndamento: [],
    };
  }).filter((s) => s.nome);
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

function mapObras(rows) {
  return rows.map((r) => ({
    id: String(r.Id ?? ''),
    titulo: String(r.Obra || r.Descricao || r.Titulo || r.Nome || 'Obra').trim().substring(0, 200),
    descricao: String(r.Descricao || r.Objeto || '').trim().substring(0, 400),
    valor: formatBRL(r.ValorGlobal || r.Valor || r.ValorTotal || ''),
    secretaria: String(r.Secretaria || r.Orgao || '').trim(),
    situacao: String(r.Situacao || r.Status || '').trim(),
    data: String(r.DataInicio || r.Data || '').trim(),
    url: resolveUrl(r.Url || r.Arquivo || ''),
  })).filter((o) => o.titulo);
}

function mapLrf(rows) {
  return rows.map((r) => ({
    id: String(r.Id ?? ''),
    titulo: String(r.Descricao || r.TipoArquivo || r.Titulo || 'Documento LRF').trim().substring(0, 200),
    tipo: String(r.TipoArquivo || r.Tipo || 'LRF').trim(),
    exercicio: String(r.Exercicio || r.Ano || '').trim(),
    data: String(r.Data || '').trim(),
    url: resolveUrl(r.Url || r.Arquivo || ''),
  })).filter((d) => d.titulo);
}

async function scrapePrefeituraDadosAbertos(http, ano) {
  const year = ano || new Date().getFullYear();

  const [licSettled, contSettled, secSettled, pubSettled, obrasSettled, lrfSettled] = await Promise.allSettled([
    fetchDataset(http, 'licitacoes', year),
    fetchDataset(http, 'contratos', year),
    fetchDataset(http, 'secretarias', year),
    fetchDataset(http, 'publicacoes', year),
    fetchDataset(http, 'obras', year),
    fetchDataset(http, 'LRF', year),
  ]);

  const licRows = licSettled.status === 'fulfilled' ? licSettled.value : [];
  const contRows = contSettled.status === 'fulfilled' ? contSettled.value : [];
  const secRows = secSettled.status === 'fulfilled' ? secSettled.value : [];
  const pubRows = pubSettled.status === 'fulfilled' ? pubSettled.value : [];
  const obrasRows = obrasSettled.status === 'fulfilled' ? obrasSettled.value : [];
  const lrfRows = lrfSettled.status === 'fulfilled' ? lrfSettled.value : [];

  const failures = [];
  const track = (name, settled) => {
    if (settled.status === 'rejected') {
      const msg = settled.reason?.message || String(settled.reason);
      failures.push(`${name}: ${msg}`);
      console.warn(`[DadosAbertos] ${name} indisponível:`, msg);
    }
  };
  track('licitações', licSettled);
  track('contratos', contSettled);
  track('secretarias', secSettled);
  track('publicações', pubSettled);
  track('obras', obrasSettled);
  track('LRF', lrfSettled);

  const licitacoes = mapLicitacoes(licRows);
  const contratos = mapContratos(contRows);
  const secretarias = mapSecretarias(secRows);
  const publicacoes = mapPublicacoes(pubRows);
  const obras = mapObras(obrasRows);
  const lrf = mapLrf(lrfRows);

  const hasCoreData = contratos.length > 0 || licitacoes.length > 0 || secretarias.length > 0;
  const scrapeError = !hasCoreData && failures.length > 0
    ? `Portal municipal indisponível (${failures.slice(0, 3).join('; ')})`
    : null;

  return {
    contratos,
    licitacoes,
    secretarias,
    publicacoes,
    obras,
    lrf,
    fonte: `${EXPORT_URL} (dados abertos JSON, exercício ${year})`,
    dataSource: 'dadosabertos',
    scrapeError,
  };
}

module.exports = {
  BASE,
  EXPORT_URL,
  fetchDataset,
  isEmptyResponse,
  looksLikeHtml,
  mapContratos,
  mapLicitacoes,
  mapSecretarias,
  mapPublicacoes,
  mapObras,
  mapLrf,
  scrapePrefeituraDadosAbertos,
};
