'use strict';

const BASE = 'https://www.cmcaninde.ce.gov.br';

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
  const foto = $('img.img-fluid').first().attr('src') || '';
  const bio = extractBiography($);
  return {
    entity: 'vereador',
    entityId: slug,
    parlamentar: {
      nome: nomeUrna || nomeCompleto,
      nomeCompleto: nomeCompleto || nomeUrna,
      partido,
      cargo,
      foto,
      slug,
      profileUrl: `${BASE}/vereadores/${slug}/`,
      contato: parseContatoFromHtml($),
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
  const pdfUrl = $('a[href$=".pdf"]').first().attr('href') || '';
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
  normalizeWhatsapp,
  scrapeVereadorDetail,
  scrapeMateriaDetail,
  scrapeInstitucionalCamara,
};
