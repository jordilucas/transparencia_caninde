'use strict';

const BASE = 'https://www.cmcaninde.ce.gov.br';

function resolveHref(href, base = BASE) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${base}${t.startsWith('/') ? '' : '/'}${t}`;
}

function emptyContato() {
  return { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' };
}

function normalizeWhatsapp(raw) {
  if (!raw || typeof raw !== 'string') return '';
  const trimmed = raw.trim();
  if (/addtoany/i.test(trimmed)) return '';
  const waMe = trimmed.match(/wa\.me\/(\d{8,15})/i);
  if (waMe) return waMe[1];
  const phoneParam = trimmed.match(/[?&]phone=(\d{8,15})/i);
  if (phoneParam) return phoneParam[1];
  const digits = trimmed.replace(/\D/g, '');
  if (digits.length >= 10 && digits.length <= 15) return digits;
  if (/wa\.me|whatsapp/i.test(trimmed)) return '';
  return '';
}

function parseContatoFromHtml($) {
  const contato = emptyContato();
  $('p, li, div').each((_, el) => {
    const t = $(el).text().trim();
    const emailM = t.match(/E-?mail:\s*([^\s<]+@[^\s<]+)/i);
    if (emailM) contato.email = emailM[1].trim();
    const telM = t.match(/Telefone:\s*([+\d\s()-]+)/i);
    if (telM) contato.telefone = telM[1].trim();
    const wppM = t.match(/(?:whatsapp|wpp)[:\s]*([+\d\s()-]{10,})/i);
    if (wppM) {
      const n = normalizeWhatsapp(wppM[1]);
      if (n) contato.whatsapp = n;
    }
  });
  $('a[href^="mailto:"]').each((_, el) => {
    if (!contato.email) contato.email = ($(el).attr('href') || '').replace('mailto:', '').trim();
  });
  $('a[href*="wa.me"], a[href*="whatsapp"]').each((_, el) => {
    if (contato.whatsapp) return;
    const href = $(el).attr('href') || '';
    if (/addtoany|share/i.test(href) || $(el).closest('.addtoany').length) return;
    const n = normalizeWhatsapp(href);
    if (n) contato.whatsapp = n;
  });
  const bodyText = $('body').text();
  const endIdx = bodyText.indexOf('Endereço');
  if (endIdx >= 0) {
    const chunk = bodyText.substring(endIdx, endIdx + 200);
    const lines = chunk.split('\n').map((l) => l.trim()).filter(Boolean);
    if (lines.length > 1) contato.endereco = lines.slice(1, 3).join(' ').substring(0, 200);
  }
  const horM = bodyText.match(/De Segunda[^\n]+/i);
  if (horM) contato.horarioFuncionamento = horM[0].trim();
  return contato;
}

function extractBiography($) {
  const root = $('.entry-content, .parlamentar-content, article .entry-content, article').first();
  if (!root.length) return '';
  const parts = [];
  root.find('p').each((_, p) => {
    const t = $(p).text().replace(/\s+/g, ' ').trim();
    if (t.length < 25) return;
    if (/^(parlamentar|cargo|e-?mail|telefone|endereço|horário|de segunda)/i.test(t)) return;
    if (/addtoany|compartilhar|whatsapp|wa\.me/i.test(t)) return;
    parts.push(t);
  });
  if (parts.length === 0) {
    return root.text().replace(/\s+/g, ' ').trim().substring(0, 1500);
  }
  return parts.join('\n\n').substring(0, 2000);
}

function extractFieldValue($, labelPattern) {
  let value = '';
  $('p, li, div').each((_, el) => {
    if (value) return;
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    const m = t.match(new RegExp(`${labelPattern}:\\s*(.+)`, 'i'));
    if (m) value = m[1].trim();
  });
  return value;
}

function scrapeSessoesPresentes($) {
  const sessoes = [];
  const panel = $('#painel-sessao');
  const root = panel.length ? panel : $('body');
  root.find('a[href*="/sessao/"]').each((i, el) => {
    if (i >= 8) return false;
    const href = $(el).attr('href') || '';
    const titulo = $(el).text().replace(/\s+/g, ' ').trim();
    const slug = href.match(/\/sessao\/([^/]+)\/?/i)?.[1] || '';
    if (!titulo || titulo.length < 5 || !slug) return;
    sessoes.push({
      titulo: titulo.substring(0, 120),
      data: '',
      url: resolveHref(href),
      slug,
      resumo: '',
      modifiedAt: '',
    });
  });
  return sessoes;
}

function scrapeVereadorDetail(html, cheerio, slug) {
  const $ = cheerio.load(html);
  const nomeUrna = $('h1, h2').first().text().trim();
  let nomeCompleto = '';
  $('p').each((_, p) => {
    const t = $(p).text().trim();
    if (t.startsWith('Parlamentar:')) nomeCompleto = t.replace(/^Parlamentar:\s*/i, '').trim();
  });
  const cargoLine = $('p.cargo').not('.d-none').first().text().trim();
  let cargo = '';
  let partido = '';
  const m = cargoLine.match(/Cargo:\s*(.+?)\s*-\s*(.+)/i);
  if (m) {
    cargo = m[1].trim();
    partido = m[2].trim();
  }
  const vinculo = $('p.vinculo').first().text().trim();
  const foto = $('img.img-fluid').first().attr('src') || '';
  const bio = extractBiography($);
  const { totalMaterias, totalSessoes } = require('./scraper-camara').parseBadgeCounts($('body'), $);
  const sessoesPresentes = scrapeSessoesPresentes($);
  const totalSessoesFinal = Math.max(totalSessoes, sessoesPresentes.length);

  return {
    entity: 'vereador',
    entityId: slug,
    parlamentar: {
      nome: nomeUrna || nomeCompleto,
      nomeCompleto: nomeCompleto || nomeUrna,
      partido,
      cargo,
      vinculo,
      legislatura: extractFieldValue($, 'Legislatura') || '',
      foto,
      slug,
      profileUrl: `${BASE}/vereadores/${slug}/`,
      totalMaterias,
      totalSessoes: totalSessoesFinal,
      mandatoInicio: extractFieldValue($, 'Data In[ií]cio'),
      mandatoFim: extractFieldValue($, 'Data Final'),
      naturalidade: extractFieldValue($, 'Naturalidade'),
      dataNascimento: extractFieldValue($, 'Data de Nascimento'),
      estadoCivil: extractFieldValue($, 'Estado Civil'),
      sessoesPresentes,
      contato: emptyContato(),
      biografia: bio,
    },
  };
}

function scrapeMateriaDetail(html, cheerio, slug) {
  const $ = cheerio.load(html);
  const titulo = $('h1, h2').first().text().trim();
  let tipo = '';
  $('p, span').each((_, el) => {
    const t = $(el).text().trim();
    if (/tipo/i.test(t) && t.length < 80) tipo = t.replace(/tipo:/i, '').trim();
  });
  let pdfUrl = '';
  $('a[href*=".pdf" i]').each((_, el) => {
    if (pdfUrl) return;
    const href = $(el).attr('href') || '';
    if (href) pdfUrl = resolveHref(href);
  });
  const autor = $('h6:contains("AUTOR"), h6').filter((_, el) => /autor/i.test($(el).text())).first().text().replace(/autor:/i, '').trim();
  const resumo = $('.entry-content, article p').first().text().trim().substring(0, 500);
  return {
    entity: 'materia',
    entityId: slug,
    materia: {
      titulo,
      tipo,
      slug,
      url: `${BASE}/materia/${slug}/`,
      autor,
      pdfUrl,
      resumo,
    },
  };
}

function extractVideoEmbed($) {
  let embedUrl = '';
  $('iframe[src]').each((_, el) => {
    if (embedUrl) return false;
    const src = ($(el).attr('src') || '').trim();
    if (/youtube|youtu\.be|vimeo|facebook\.com\/plugins\/video/i.test(src)) {
      embedUrl = resolveHref(src);
    }
  });
  if (embedUrl) return embedUrl;

  $('a[href*="youtube.com"], a[href*="youtu.be"]').each((_, el) => {
    if (embedUrl) return false;
    const href = $(el).attr('href') || '';
    const id = href.match(/(?:v=|youtu\.be\/|embed\/)([A-Za-z0-9_-]{6,})/)?.[1];
    if (id) embedUrl = `https://www.youtube.com/embed/${id}`;
  });
  return embedUrl;
}

function scrapeSessaoDetail(html, cheerio, slug) {
  const $ = cheerio.load(html);
  const titulo = $('h1, h2').first().text().replace(/\s+/g, ' ').trim();
  const resumo = $('.entry-content, article .entry-content, article p')
    .map((_, el) => $(el).text().replace(/\s+/g, ' ').trim())
    .get()
    .filter((t) => t.length > 30)
    .slice(0, 3)
    .join('\n\n')
    .substring(0, 2000);
  const camposExtras = [];
  $('.entry-content p, article p').each((_, el) => {
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    const m = t.match(/^([^:]{3,40}):\s*(.+)$/);
    if (m && m[2].length > 2) {
      camposExtras.push({ rotulo: m[1].trim(), valor: m[2].trim().substring(0, 400) });
    }
  });
  const anexos = [];
  $('a[href]').each((_, el) => {
    const href = $(el).attr('href') || '';
    if (!/\.pdf/i.test(href)) return;
    const url = resolveHref(href);
    if (!url || anexos.some((a) => a.url === url)) return;
    anexos.push({
      titulo: $(el).text().replace(/\s+/g, ' ').trim() || 'Documento PDF',
      url,
      extensao: 'PDF',
    });
  });
  let data = '';
  const dateM = $('body').text().match(/\d{2}\/\d{2}\/\d{4}/);
  if (dateM) data = dateM[0];
  const videoEmbedUrl = extractVideoEmbed($);
  return {
    titulo,
    resumo,
    data,
    videoEmbedUrl,
    camposExtras: camposExtras.slice(0, 10),
    anexos,
  };
}

function mergeSessaoDetail(listItem, scraped) {
  if (!scraped) return listItem;
  return {
    ...listItem,
    titulo: scraped.titulo || listItem.titulo,
    resumo: scraped.resumo || listItem.resumo,
    data: scraped.data || listItem.data,
    videoEmbedUrl: scraped.videoEmbedUrl || listItem.videoEmbedUrl || '',
    camposExtras: scraped.camposExtras?.length ? scraped.camposExtras : (listItem.camposExtras || []),
    anexos: scraped.anexos?.length ? scraped.anexos : (listItem.anexos || []),
  };
}

function scrapeInstitucionalCamara(html, cheerio) {
  const $ = cheerio.load(html);
  const contato = emptyContato();
  const footer = $('footer, .footer').text();
  const emailM = footer.match(/[\w.+-]+@[\w.-]+\.[a-z]{2,}/i);
  if (emailM) contato.email = emailM[0];
  const horM = footer.match(/De Segunda[^\n]+/i) || $('body').text().match(/De Segunda[^\n]+/i);
  if (horM) contato.horarioFuncionamento = horM[0].trim();
  if (footer.includes('Largo Francisco')) {
    contato.endereco = 'Largo Francisco Xavier de Medeiros, S/N, Imaculada Conceição, Canindé/CE';
  }
  return {
    entity: 'institucional',
    entityId: 'camara',
    institucional: {
      orgao: 'Câmara Municipal de Canindé',
      endereco: contato.endereco,
      contato,
      siteUrl: BASE,
    },
  };
}

module.exports = {
  BASE,
  resolveHref,
  normalizeWhatsapp,
  scrapeVereadorDetail,
  scrapeMateriaDetail,
  scrapeSessaoDetail,
  mergeSessaoDetail,
  scrapeInstitucionalCamara,
};
