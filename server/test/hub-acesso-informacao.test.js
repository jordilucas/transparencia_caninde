'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const {
  scrapeAcessoInformacaoHub,
  inferCategoria,
  HUB_URL,
} = require('../lib/scraper-acesso-informacao');
const { mergeTransparenciaLinks } = require('../lib/transparencia-links-merge');
const { mergeEntitiesByRecency } = require('../lib/entity-recency');

describe('scraper-acesso-informacao', () => {
  it('extrai links do hub com seção e categoria', () => {
    const html = `
      <main>
        <h2>Atendimento ao Cidadão</h2>
        <ul>
          <li><a href="/esic.php">e-SIC</a></li>
          <li><a href="/ouvidoria">Ouvidoria</a></li>
        </ul>
        <h2>Execução orçamentária e financeira</h2>
        <ul>
          <li><a href="/lrf.php">Lei de Responsabilidade Fiscal</a></li>
          <li><a href="https://www.governotransparente.com.br/acessu/11979490">Governo Transparente</a></li>
        </ul>
      </main>
    `;
    const result = scrapeAcessoInformacaoHub(html, cheerio);
    assert.ok(result.links.length >= 3);
    const esic = result.links.find((l) => l.titulo === 'e-SIC');
    assert.equal(esic.categoria, 'cidadania');
    assert.match(esic.url, /esic\.php/);
    const lrf = result.links.find((l) => /Responsabilidade Fiscal/.test(l.titulo));
    assert.equal(lrf.categoria, 'fiscal');
  });

  it('classifica e-SIC e ouvidoria como cidadania', () => {
    assert.equal(inferCategoria('https://x/esic.php', 'e-SIC', 'Atendimento'), 'cidadania');
    assert.equal(inferCategoria('https://x/ouvidoria', 'Ouvidoria', ''), 'cidadania');
  });
});

describe('transparencia-links-merge', () => {
  it('deduplica URLs e inclui hub oficial', () => {
    const merged = mergeTransparenciaLinks(
      [{ titulo: 'GT', url: 'https://www.governotransparente.com.br/acessu/11979490', categoria: 'financeiro' }],
      [{ titulo: 'e-SIC', url: 'https://www.caninde.ce.gov.br/esic.php', categoria: 'cidadania', secao: 'Atendimento' }],
      [{ titulo: 'e-SIC duplicado', url: 'https://www.caninde.ce.gov.br/esic.php', categoria: 'cidadania' }],
    );
    const hub = merged.find((l) => l.url === HUB_URL);
    assert.ok(hub);
    const esicCount = merged.filter((l) => /esic\.php/.test(l.url)).length;
    assert.equal(esicCount, 1);
  });
});

describe('mergeEntitiesByRecency', () => {
  it('prioriza documento com data mais recente', () => {
    const merged = mergeEntitiesByRecency([[
      { id: '1', titulo: 'RREO 1º bimestre', exercicio: '2024', data: '15/05/2024' },
      { id: '1', titulo: 'RREO 1º bimestre', exercicio: '2025', data: '20/05/2025' },
    ]]);
    assert.equal(merged.length, 1);
    assert.equal(merged[0].exercicio, '2025');
  });
});
