'use strict';

const BASE = 'https://www.caninde.ce.gov.br';

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
  const $ = cheerio.load(html);
  const gestores = [];
  $('table tbody tr, .gestor, .card').each((i, el) => {
    if (i >= 6) return false;
    const nome = $(el).find('td, h3, h4, strong').first().text().trim();
    const cargo = $(el).find('td').eq(1).text().trim() || $(el).find('small, p').first().text().trim();
    if (nome && nome.length > 3) {
      gestores.push({
        nome,
        cargo: cargo || 'Gestor',
        foto: $(el).find('img').attr('src') || '',
        contato: emptyContato(),
      });
    }
  });
  if (gestores.length === 0) {
    const body = $('body').text();
    const prefeitoM = body.match(/Prefeito[^\n]*/i);
    if (prefeitoM) {
      gestores.push({ nome: 'Francisco Jardel Sousa Pinho', cargo: 'Prefeito Municipal', foto: '', contato: emptyContato() });
    }
  }
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
