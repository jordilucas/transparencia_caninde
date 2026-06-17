'use strict';

const BASE = 'https://www.cmcaninde.ce.gov.br';
const WP_API = `${BASE}/wp-json/wp/v2`;

function formatWpDate(iso) {
  if (!iso || typeof iso !== 'string') return '';
  const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!m) return '';
  return `${m[3]}/${m[2]}/${m[1]}`;
}

function parseIsoMs(iso) {
  if (!iso) return 0;
  const t = Date.parse(iso);
  return Number.isNaN(t) ? 0 : t;
}

async function fetchAllPages(http, path, params = {}) {
  const items = [];
  let page = 1;
  const perPage = 100;
  for (;;) {
    const { data, headers } = await http.get(`${WP_API}/${path}`, {
      params: { ...params, per_page: perPage, page },
      validateStatus: (s) => s === 200 || s === 400,
    });
    if (!Array.isArray(data) || data.length === 0) break;
    items.push(...data);
    const totalPages = parseInt(headers['x-wp-totalpages'] || '1', 10);
    if (page >= totalPages) break;
    page += 1;
  }
  return items;
}

async function fetchTaxonomyMap(http, taxonomy) {
  const rows = await fetchAllPages(http, taxonomy);
  const map = new Map();
  for (const row of rows) {
    map.set(row.id, row.name || row.slug || '');
  }
  return map;
}

function termsFromEmbed(item, taxonomy) {
  const groups = item?._embedded?.['wp:term'] || [];
  for (const group of groups) {
    for (const term of group) {
      if (term.taxonomy === taxonomy) return term.name || term.slug || '';
    }
  }
  return '';
}

function legislaturaFromClassList(classList) {
  if (!Array.isArray(classList)) return '';
  const hit = classList.find((c) => /^ano_legislatura-\d{4}-\d{4}$/.test(c));
  if (!hit) return '';
  return hit.replace('ano_legislatura-', '').replace('-', ' - ');
}

function inferTipoMateria(titulo, slug) {
  const t = `${titulo} ${slug}`.toLowerCase();
  if (/requerimento/.test(t)) return 'Requerimento';
  if (/indica/.test(t)) return 'Indicação';
  if (/projeto|pl-/.test(t)) return 'Projeto de Lei';
  return 'Matéria';
}

function mapVereadorFromWp(item, taxMaps) {
  const slug = item.slug || '';
  const cargoId = (item.cargo || [])[0];
  const vinculoId = (item.vinculo || [])[0];
  const cargo = taxMaps.cargo.get(cargoId) || termsFromEmbed(item, 'cargo');
  const vinculo = taxMaps.vinculo.get(vinculoId) || termsFromEmbed(item, 'vinculo');
  const legislatura = legislaturaFromClassList(item.class_list)
    || taxMaps.ano_legislatura.get((item.ano_legislatura || [])[0])
    || '';
  const nome = item.title?.rendered?.trim() || '';
  if (!nome) return null;
  return {
    nome,
    nomeCompleto: nome,
    partido: '',
    cargo,
    vinculo,
    legislatura,
    foto: '',
    slug,
    profileUrl: item.link || `${BASE}/vereadores/${slug}/`,
    totalMaterias: 0,
    totalSessoes: 0,
    modifiedAt: item.modified || item.date || '',
    contato: { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' },
    biografia: '',
  };
}

function mapSessaoFromWp(item) {
  const titulo = item.title?.rendered?.trim() || '';
  if (!titulo) return null;
  const slug = item.slug || '';
  return {
    titulo: titulo.substring(0, 120),
    data: formatWpDate(item.date || item.modified),
    url: item.link || `${BASE}/sessao/${slug}/`,
    slug,
    resumo: '',
    modifiedAt: item.modified || item.date || '',
  };
}

function mapMateriaFromWp(item, tipoMap) {
  const titulo = item.title?.rendered?.trim() || '';
  if (!titulo) return null;
  const slug = item.slug || '';
  const tipoId = (item.tipo_materia || [])[0];
  const tipo = tipoMap.get(tipoId) || inferTipoMateria(titulo, slug);
  return {
    titulo: titulo.substring(0, 120),
    tipo,
    slug,
    url: item.link || `${BASE}/materia/${slug}/`,
    autor: '',
    dataPublicacao: formatWpDate(item.date || item.modified),
    pdfUrl: '',
    resumo: '',
    modifiedAt: item.modified || item.date || '',
  };
}

function buildMesaFromParlamentares(parlamentares) {
  const cargosMesa = /presidente|secret[aá]rio|vice-presidente/i;
  return parlamentares
    .filter((p) => cargosMesa.test(p.cargo))
    .map((p) => ({ nome: p.nome, cargo: p.cargo }));
}

async function scrapeCamaraWp(http) {
  const [cargoMap, vinculoMap, legislaturaMap, tipoMateriaMap] = await Promise.all([
    fetchTaxonomyMap(http, 'cargo'),
    fetchTaxonomyMap(http, 'vinculo'),
    fetchTaxonomyMap(http, 'ano_legislatura'),
    fetchTaxonomyMap(http, 'tipo_materia'),
  ]);

  const taxMaps = {
    cargo: cargoMap,
    vinculo: vinculoMap,
    ano_legislatura: legislaturaMap,
  };

  const [vereadoresRaw, sessoesRaw, materiasRaw] = await Promise.all([
    fetchAllPages(http, 'vereadores', { _embed: 'wp:term', orderby: 'modified', order: 'desc' }),
    fetchAllPages(http, 'sessao', { orderby: 'modified', order: 'desc' }),
    fetchAllPages(http, 'materia', { orderby: 'modified', order: 'desc' }),
  ]);

  const parlamentares = vereadoresRaw
    .map((item) => mapVereadorFromWp(item, taxMaps))
    .filter(Boolean);

  const sessoes = sessoesRaw
    .map((item) => mapSessaoFromWp(item))
    .filter(Boolean)
    .slice(0, 25);

  const materias = materiasRaw
    .map((item) => mapMateriaFromWp(item, tipoMateriaMap))
    .filter(Boolean)
    .slice(0, 25);

  const mesaDiretora = buildMesaFromParlamentares(parlamentares);

  return {
    parlamentares,
    sessoes,
    materias,
    mesaDiretora,
    fonte: `${WP_API}/vereadores`,
    fontesUtilizadas: ['wp-rest'],
  };
}

module.exports = {
  BASE,
  WP_API,
  scrapeCamaraWp,
  mapVereadorFromWp,
  mapSessaoFromWp,
  mapMateriaFromWp,
  formatWpDate,
  parseIsoMs,
};
