'use strict';

/**
 * Scraping da Câmara Municipal de Canindé/CE (cmcaninde.ce.gov.br).
 * Layout WordPress tema "cmcaninde" — cards em .cardlist, não .parlamentar-card genérico.
 */

const BASE = 'https://www.cmcaninde.ce.gov.br';

function parseCargoPartido(cargoText) {
  const m = cargoText.match(/Cargo:\s*(.+?)\s*-\s*(.+)/i);
  if (m) return { cargo: m[1].trim(), partido: m[2].trim() };
  return { cargo: cargoText.replace(/Cargo:\s*/i, '').trim(), partido: '' };
}

function slugFromHref(href) {
  if (!href) return '';
  const m = href.match(/\/vereadores\/([^/]+)\/?/i);
  return m ? m[1] : '';
}

function slugifyNome(nome) {
  return nome
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

function parseParlamentarCard($, el) {
  const block = $(el);
  const nomeUrna = block.find('p strong').first().text().trim();
  let nomeCompleto = '';
  block.find('p').each((_, p) => {
    const t = $(p).text().trim();
    if (t.startsWith('Parlamentar:')) {
      nomeCompleto = t.replace(/^Parlamentar:\s*/i, '').trim();
    }
  });
  const cargoLine = block.find('p.cargo').not('.d-none').first().text().trim();
  const { cargo, partido } = parseCargoPartido(cargoLine);
  const foto = block.find('img.img-fluid').attr('src') || '';
  const linkEl = block.find('a[href*="/vereadores/"]').first();
  const href = linkEl.attr('href') || block.closest('a[href*="/vereadores/"]').attr('href') || '';
  const slug = slugFromHref(href) || slugifyNome(nomeUrna || nomeCompleto);
  const profileUrl = slug ? `${BASE}/vereadores/${slug}/` : '';
  const nome = nomeUrna || nomeCompleto;
  if (!nome || nome.length < 2) return null;
  return {
    nome,
    nomeCompleto: nomeCompleto || nomeUrna,
    partido,
    cargo,
    foto,
    slug,
    profileUrl,
    contato: { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' },
    biografia: '',
  };
}

function scrapeParlamentaresFromHtml(html, cheerio) {
  const $ = cheerio.load(html);
  const parlamentares = [];
  const seen = new Set();

  $('.cardlist').each((_, el) => {
    const p = parseParlamentarCard($, el);
    if (p && !seen.has(p.nome)) {
      seen.add(p.nome);
      parlamentares.push(p);
    }
  });

  return parlamentares;
}

function scrapeMesaDiretoraFromHtml(html, cheerio) {
  const $ = cheerio.load(html);
  const mesa = [];
  const seen = new Set();

  $('.box-mesa-diretora').each((_, el) => {
    const p = parseParlamentarCard($, $(el).closest('.cardlist'));
    if (!p) {
      const block = $(el);
      const nome = block.find('p strong').first().text().trim();
      const cargoLine = block.find('p.cargo').not('.d-none').first().text().trim();
      const { cargo } = parseCargoPartido(cargoLine);
      if (nome && !seen.has(nome)) {
        seen.add(nome);
        mesa.push({ nome, cargo });
      }
      return;
    }
    if (!seen.has(p.nome)) {
      seen.add(p.nome);
      mesa.push({ nome: p.nome, cargo: p.cargo });
    }
  });

  return mesa;
}

function scrapeSessoesFromHtml(html, cheerio) {
  const $ = cheerio.load(html);
  const sessoes = [];
  $('h3.mb-0').each((i, el) => {
    if (i >= 12) return false;
    const block = $(el);
    const titulo = block.find('span, a').first().text().trim() || block.text().trim();
    const parent = block.parent();
    const data = parent.find('p, small').first().text().trim().substring(0, 80);
    const url = block.find('a').attr('href') || '';
    if (titulo && titulo.length > 3) {
      sessoes.push({
        titulo: titulo.substring(0, 120),
        data,
        url: url.startsWith('http') ? url : (url ? `${BASE}${url.startsWith('/') ? '' : '/'}${url}` : ''),
        resumo: '',
      });
    }
  });
  return sessoes;
}

function scrapeMateriasFromHtml(html, cheerio) {
  const $ = cheerio.load(html);
  const materias = [];
  $('h3.mb-0 a.btn-link').each((i, el) => {
    if (i >= 12) return false;
    const titulo = $(el).text().trim();
    const href = $(el).attr('href') || '';
    let tipo = 'Matéria';
    if (/requerimento/i.test(titulo) || /requerimento/i.test(href)) tipo = 'Requerimento';
    else if (/projeto|pl-/i.test(titulo)) tipo = 'Projeto de Lei';
    else if (/indica/i.test(titulo)) tipo = 'Indicação';
    const slug = href.match(/\/materia\/([^/]+)\/?/i)?.[1] || '';
    const url = href.startsWith('http') ? href : (href ? `${BASE}${href.startsWith('/') ? '' : '/'}${href}` : '');
    if (titulo && titulo.length > 3) {
      materias.push({
        titulo: titulo.substring(0, 120),
        tipo,
        slug,
        url,
        autor: '',
        dataPublicacao: '',
        pdfUrl: '',
        resumo: '',
      });
    }
  });
  return materias;
}

module.exports = {
  BASE,
  scrapeParlamentaresFromHtml,
  scrapeMesaDiretoraFromHtml,
  scrapeSessoesFromHtml,
  scrapeMateriasFromHtml,
};
