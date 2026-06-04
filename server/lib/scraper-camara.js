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
  const nome = nomeUrna || nomeCompleto;
  if (!nome || nome.length < 2) return null;
  return {
    nome,
    nomeCompleto: nomeCompleto || nomeUrna,
    partido,
    cargo,
    foto,
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
      parlamentares.push({
        nome: p.nome,
        partido: p.partido,
        cargo: p.cargo,
        foto: p.foto,
      });
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
  $('h3.mb-0 span').each((i, el) => {
    if (i >= 12) return false;
    const titulo = $(el).text().trim();
    if (titulo && titulo.length > 3) {
      sessoes.push({ titulo: titulo.substring(0, 120), data: '' });
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
    if (titulo && titulo.length > 3) {
      materias.push({ titulo: titulo.substring(0, 120), tipo });
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
