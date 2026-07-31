'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const { scrapeDocumentList, resolveUrl } = require('../lib/scraper-camara-portal');

describe('scraper-camara-portal', () => {
  it('resolveUrl monta URL absoluta do portal', () => {
    assert.equal(
      resolveUrl('/caninde-transparente/licitacoes/'),
      'https://www.cmcaninde.ce.gov.br/caninde-transparente/licitacoes/',
    );
  });

  it('scrapeDocumentList extrai links de licitações', () => {
    const html = `
      <main>
        <article>
          <h2><a href="/caninde-transparente/licitacao/edital-001/">Edital de Pregão 001/2026</a></h2>
          <time>15/07/2026</time>
        </article>
        <article>
          <h2><a href="/caninde-transparente/contrato/contrato-99/">Contrato 99/2026</a></h2>
          <small>20/07/2026</small>
        </article>
      </main>
    `;
    const docs = scrapeDocumentList(html, cheerio, 'licitacao');
    assert.ok(docs.length >= 2);
    assert.ok(docs.some((d) => d.titulo.includes('Edital')));
    assert.ok(docs.every((d) => d.url.startsWith('https://www.cmcaninde.ce.gov.br/')));
  });
});
