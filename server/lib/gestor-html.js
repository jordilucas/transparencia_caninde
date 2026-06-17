'use strict';

const BASE = 'https://www.caninde.ce.gov.br';

function emptyContato() {
  return { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' };
}

function resolveUrl(href) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${BASE}${t.startsWith('/') ? '' : '/'}${t}`;
}

function normalizeCargo(text) {
  const t = String(text || '').trim();
  if (/vice/i.test(t)) return 'Vice-Prefeito(a)';
  if (/prefeito/i.test(t)) return 'Prefeito(a)';
  return t || 'Gestor(a)';
}

function parseGestoresAtual($) {
  const gestores = [];
  const seen = new Set();

  $('.titlepre').each((_, el) => {
    const $el = $(el);
    const nome = $el.find('strong').first().text().trim();
    if (!nome || nome.length < 4 || seen.has(nome.toUpperCase())) return;

    let cargo = '';
    $el.find('p').each((__, p) => {
      const t = $(p).text().trim();
      if (/prefeito/i.test(t)) cargo = normalizeCargo(t);
    });

    const foto = resolveUrl(
      $el.closest('.row').find('img[src*="prefeitos"], img.aparecerNoResponsive, img').first().attr('src') || '',
    );

    seen.add(nome.toUpperCase());
    gestores.push({ nome, cargo, foto, contato: emptyContato() });
  });

  if (gestores.length === 0) {
    $('.modal-title').each((i, el) => {
      const nome = $(el).text().trim();
      if (!nome || nome.length < 4 || seen.has(nome.toUpperCase())) return;
      seen.add(nome.toUpperCase());
      gestores.push({
        nome,
        cargo: i === 0 ? 'Prefeito(a)' : 'Vice-Prefeito(a)',
        foto: '',
        contato: emptyContato(),
      });
    });
  }

  return gestores
    .sort((a, b) => {
      const rank = (c) => (/^prefeito/i.test(c) ? 0 : 1);
      return rank(a.cargo) - rank(b.cargo);
    })
    .slice(0, 4);
}

function parseGestoresHistoricoTable($) {
  const gestores = [];
  $('table tbody tr').each((_, row) => {
    const cells = {};
    $(row).find('td').each((__, td) => {
      const label = ($(td).attr('data-title') || '').trim().toLowerCase();
      const value = $(td).text().trim();
      if (label) cells[label] = value;
    });

    const nome = cells.nome || '';
    const cargo = cells.cargo || '';
    if (!nome || nome.length < 4) return;

    gestores.push({
      nome,
      cargo: normalizeCargo(cargo),
      foto: '',
      contato: emptyContato(),
      mandato: [cells['data inicio'], cells['data fim']].filter(Boolean).join(' – '),
    });
  });
  return gestores;
}

function scrapeGestoresFromHtml(html, cheerio) {
  const $ = cheerio.load(html);
  const atuais = parseGestoresAtual($);
  if (atuais.length > 0) return atuais;
  return parseGestoresHistoricoTable($).slice(0, 6);
}

module.exports = {
  BASE,
  emptyContato,
  parseGestoresAtual,
  scrapeGestoresFromHtml,
};
