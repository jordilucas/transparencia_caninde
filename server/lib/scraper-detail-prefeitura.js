'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const { scrapeGestoresFromHtml } = require('./gestor-html');

function emptyContato() {
  return { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' };
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
  scrapeSecretariaDetail,
  scrapeGestores,
  scrapeInstitucionalPrefeitura,
};
