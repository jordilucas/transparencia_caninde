'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const { scrapeGestoresFromHtml } = require('./gestor-html');

function emptyContato() {
  return { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' };
}

function resolvePrefUrl(href) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${BASE}${t.startsWith('/') ? '' : '/'}${t}`;
}

function normalizeLabel(text) {
  return String(text || '').replace(/:$/, '').trim();
}

function fieldMapKey(rotulo) {
  return normalizeLabel(rotulo).toLowerCase();
}

function extractColStrongFields($, scope) {
  const fields = [];
  const seen = new Set();
  $(scope).find('.col-md-12').each((_, el) => {
    const $el = $(el);
    const strong = $el.find('strong').first();
    if (!strong.length) return;
    const rotulo = normalizeLabel(strong.text());
    if (!rotulo || rotulo.length > 100) return;
    if (/informações do objeto/i.test(rotulo)) return;
    let valor = $el.text().replace(/\s+/g, ' ').trim();
    valor = valor.replace(new RegExp(`^.*?${rotulo.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}:?\\s*`, 'i'), '').trim();
    if (!valor) return;
    const key = fieldMapKey(rotulo);
    if (seen.has(key)) return;
    seen.add(key);
    fields.push({ rotulo, valor: valor.substring(0, 600) });
  });
  return fields;
}

function fieldsToMap(fields) {
  return Object.fromEntries(fields.map((f) => [fieldMapKey(f.rotulo), f.valor]));
}

function extractObjetoText($) {
  let objeto = '';
  $('.col-md-12').each((_, el) => {
    const $el = $(el);
    const strongText = $el.find('strong').text().trim();
    if (/informações do objeto/i.test(strongText)) {
      const next = $el.next('.col-md-12');
      if (next.length) {
        objeto = next.text().replace(/\s+/g, ' ').trim();
      }
    }
  });
  return objeto.substring(0, 2000);
}

function extractVigenciaDates($) {
  let vigenciaInicio = '';
  let vigenciaFim = '';
  $('.progresso-vigencia').closest('.col-md-12').find('span').each((_, el) => {
    const t = $(el).text().trim();
    if (/^\d{2}\/\d{2}\/\d{4}$/.test(t)) {
      if (!vigenciaInicio) vigenciaInicio = t;
      else if (!vigenciaFim) vigenciaFim = t;
    }
  });
  const vigenciaStatus = $('p.text-center span').first().text().replace(/\s+/g, ' ').trim();
  return { vigenciaInicio, vigenciaFim, vigenciaStatus };
}

function extractPdfLinks($) {
  const anexos = [];
  $('a[href]').each((_, el) => {
    const href = $(el).attr('href') || '';
    if (!/\.pdf/i.test(href)) return;
    const url = resolvePrefUrl(href);
    if (!url || anexos.some((a) => a.url === url)) return;
    const titulo = $(el).text().replace(/\s+/g, ' ').trim()
      || $(el).attr('title')
      || 'Documento PDF';
    anexos.push({ titulo: titulo.substring(0, 200), url, extensao: 'PDF' });
  });
  return anexos;
}

function extractTableRows($, tableSelector, mapper) {
  const rows = [];
  $(tableSelector).each((_, table) => {
    $(table).find('tbody tr').each((__, tr) => {
      const row = mapper($, tr);
      if (row) rows.push(row);
    });
  });
  return rows;
}

function scrapeContratoDetail(html, cheerio, id) {
  const $ = cheerio.load(html);
  const titulo = $('.titulo strong').first().text().replace(/\s+/g, ' ').trim();
  const campos = extractColStrongFields($, '.public_paginas');
  const map = fieldsToMap(campos);
  const objeto = extractObjetoText($);
  const { vigenciaInicio, vigenciaFim, vigenciaStatus } = extractVigenciaDates($);
  const anexos = extractPdfLinks($);

  const licitacaoRows = extractTableRows($, 'table.table', ($t, tr) => {
    const cols = $(tr).find('td').map((__, td) => $(td).text().replace(/\s+/g, ' ').trim()).get();
    if (cols.length < 3) return null;
    return {
      rotulo: 'Licitação vinculada',
      valor: `${cols[1] || ''} ${cols[2] || ''} (${cols[0] || ''})`.trim(),
    };
  });

  const skipKeys = new Set([
    'credor/contratado', 'cpf/cnpj', 'valor contratado', 'secretaria/contratante', 'data da publicação',
  ]);
  const camposExtras = [
    ...campos.filter((c) => !skipKeys.has(fieldMapKey(c.rotulo))),
    ...licitacaoRows,
  ];

  return {
    titulo,
    objeto,
    empresa: map['credor/contratado'] || '',
    cnpjCredor: map['cpf/cnpj'] || '',
    valor: map['valor contratado'] || '',
    secretaria: map['secretaria/contratante'] || '',
    dataPublicacao: map['data da publicação'] || '',
    vigenciaInicio,
    vigenciaFim,
    vigenciaStatus,
    pdfUrl: anexos[0]?.url || '',
    anexos,
    camposExtras,
  };
}

function scrapeLicitacaoDetail(html, cheerio, id) {
  const $ = cheerio.load(html);
  const titulo = $('.titulo strong').first().text().replace(/\s+/g, ' ').trim();
  const campos = extractColStrongFields($, '.public_paginas');
  const map = fieldsToMap(campos);
  const objeto = extractObjetoText($);

  const responsaveis = extractTableRows($, '#responsaveis table, .tab-pane#responsaveis table', ($t, tr) => {
    const cols = $(tr).find('td').map((__, td) => $(td).text().replace(/\s+/g, ' ').trim()).get();
    if (cols.length < 2) return null;
    return { rotulo: cols[0], valor: cols[1] };
  });

  const orgaos = extractTableRows($, '#orgao table, .tab-pane#orgao table', ($t, tr) => {
    const cols = $(tr).find('td').map((__, td) => $(td).text().replace(/\s+/g, ' ').trim()).get();
    if (cols.length < 2) return null;
    return { rotulo: 'Órgão', valor: cols[1] || cols[0] };
  });

  const publicacoes = extractTableRows($, '#formas table, .tab-pane#formas table', ($t, tr) => {
    const cols = $(tr).find('td').map((__, td) => $(td).text().replace(/\s+/g, ' ').trim()).get();
    if (cols.length < 2) return null;
    return { rotulo: `Publicação (${cols[1] || 'tipo'})`, valor: `${cols[0]} — ${cols[2] || cols[1]}` };
  });

  const andamentos = [];
  $('#andamentos .cbp_tmlabel p, .tab-pane#andamentos .cbp_tmlabel p').each((_, el) => {
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    if (t.length > 15) andamentos.push(t.substring(0, 400));
  });

  const anexos = [];
  $('table a[href*="arquivos_download"], table a[href*=".pdf" i]').each((_, el) => {
    const href = $(el).attr('href') || '';
    const url = resolvePrefUrl(href);
    if (!url) return;
    const row = $(el).closest('tr');
    const tituloAnexo = row.find('td[data-title="Descrição"], td').first().text().replace(/\s+/g, ' ').trim()
      || 'Arquivo';
    const extensao = row.find('td[data-title="Extensão"]').text().trim() || 'PDF';
    if (!anexos.some((a) => a.url === url)) {
      anexos.push({ titulo: tituloAnexo.substring(0, 200), url, extensao });
    }
  });
  if (anexos.length === 0) {
    anexos.push(...extractPdfLinks($));
  }

  const skipKeys = new Set([
    'tipo', 'data da abertura', 'hora da abertura', 'eletrônica', 'valor estimado',
  ]);
  const camposExtras = [
    ...campos.filter((c) => !skipKeys.has(fieldMapKey(c.rotulo))),
    ...responsaveis,
    ...orgaos.slice(0, 3),
    ...publicacoes.slice(0, 5),
  ];

  let plataformaEletronica = map.eletrônica || map.eletronica || '';
  if (!plataformaEletronica) {
    const link = $('a.lici[href]').first().attr('href') || '';
    if (link) plataformaEletronica = link.replace(/^https?:\/\//i, '');
  }

  return {
    titulo,
    objeto,
    modalidade: titulo.split('-')[0]?.trim() || map.modalidade || '',
    situacao: titulo.includes('ABERTA') ? 'Aberta' : (map.situação || map.situacao || ''),
    dataAbertura: map['data da abertura'] || '',
    horaAbertura: map['hora da abertura'] || '',
    valorEstimado: map['valor estimado'] || '',
    tipoJulgamento: map.tipo || '',
    plataformaEletronica,
    anexos,
    camposExtras,
    andamentos: andamentos.slice(0, 12),
  };
}

function mergeContratoDetail(listItem, scraped) {
  if (!scraped) return listItem;
  const vigenciaLabel = [scraped.vigenciaInicio, scraped.vigenciaFim].filter(Boolean).join(' — ');
  return {
    ...listItem,
    numero: listItem.numero || scraped.titulo || listItem.numero,
    objeto: scraped.objeto || listItem.objeto,
    empresa: scraped.empresa || listItem.empresa,
    cnpjCredor: scraped.cnpjCredor || listItem.cnpjCredor,
    valor: scraped.valor || listItem.valor,
    secretaria: scraped.secretaria || listItem.secretaria,
    dataPublicacao: scraped.dataPublicacao || listItem.dataPublicacao || '',
    vigenciaInicio: scraped.vigenciaInicio || listItem.vigenciaInicio || '',
    vigenciaFim: scraped.vigenciaFim || listItem.vigenciaFim || '',
    vigenciaStatus: scraped.vigenciaStatus || listItem.vigenciaStatus || '',
    data: vigenciaLabel || listItem.data,
    pdfUrl: scraped.pdfUrl || listItem.pdfUrl,
    url: listItem.url || `${BASE}/contratos.php?id=${listItem.id || ''}`,
    anexos: scraped.anexos?.length ? scraped.anexos : (listItem.anexos || []),
    camposExtras: scraped.camposExtras || listItem.camposExtras || [],
  };
}

function mergeLicitacaoDetail(listItem, scraped) {
  if (!scraped) return listItem;
  return {
    ...listItem,
    numero: listItem.numero || scraped.titulo || listItem.numero,
    modalidade: scraped.modalidade || listItem.modalidade,
    objeto: scraped.objeto || listItem.objeto,
    situacao: scraped.situacao || listItem.situacao,
    dataAbertura: scraped.dataAbertura || listItem.dataAbertura,
    horaAbertura: scraped.horaAbertura || listItem.horaAbertura || '',
    valorEstimado: scraped.valorEstimado || listItem.valorEstimado || '',
    tipoJulgamento: scraped.tipoJulgamento || listItem.tipoJulgamento || '',
    plataformaEletronica: scraped.plataformaEletronica || listItem.plataformaEletronica || '',
    url: listItem.url || `${BASE}/licitacaolista.php?id=${listItem.id || ''}`,
    anexos: scraped.anexos?.length ? scraped.anexos : (listItem.anexos || []),
    camposExtras: scraped.camposExtras || listItem.camposExtras || [],
    andamentos: scraped.andamentos?.length ? scraped.andamentos : (listItem.andamentos || []),
  };
}

function scrapeSecretariaDetail(html, cheerio, secId) {
  const $ = cheerio.load(html);
  const nome = $('h1, h2, .titulo').first().text().trim() || `Secretaria ${secId}`;
  let secretario = '';
  $('h6, p, div').each((_, el) => {
    const t = $(el).text().trim();
    if (/secretário/i.test(t) && t.length < 120) secretario = t.replace(/secretário\(a\):?/i, '').trim();
  });
  const contato = emptyContato();
  const text = $('body').text();
  const emailM = text.match(/[\w.+-]+@[\w.-]+\.[a-z]{2,}/i);
  if (emailM) contato.email = emailM[0];
  const horM = text.match(/Horário:\s*([^\n]+)/i);
  if (horM) contato.horarioFuncionamento = horM[1].trim();
  const endM = text.match(/Endereço:\s*([^\n]+)/i);
  if (endM) contato.endereco = endM[1].trim();
  return {
    entity: 'secretaria',
    entityId: secId,
    secretaria: {
      id: secId,
      nome,
      secretario,
      url: `${BASE}/secretaria.php?sec=${secId}`,
      contato,
    },
  };
}

function scrapeGestores(html, cheerio) {
  const gestores = scrapeGestoresFromHtml(html, cheerio);
  return {
    entity: 'gestores',
    entityId: 'all',
    gestores,
  };
}

function scrapeInstitucionalPrefeitura(html, cheerio) {
  const $ = cheerio.load(html);
  const contato = emptyContato();
  const text = $('body').text();
  const emailM = text.match(/[\w.+-]+@[\w.-]+\.[a-z]{2,}/i);
  if (emailM) contato.email = emailM[0];
  return {
    entity: 'institucional',
    entityId: 'prefeitura',
    institucional: {
      orgao: 'Prefeitura Municipal de Canindé',
      endereco: contato.endereco,
      contato,
      siteUrl: BASE,
    },
  };
}

module.exports = {
  BASE,
  resolvePrefUrl,
  extractColStrongFields,
  extractPdfLinks,
  scrapeContratoDetail,
  scrapeLicitacaoDetail,
  mergeContratoDetail,
  mergeLicitacaoDetail,
  scrapeSecretariaDetail,
  scrapeGestores,
  scrapeInstitucionalPrefeitura,
};
