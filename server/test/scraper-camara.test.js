'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const fs = require('fs');
const path = require('path');
const {
  scrapeParlamentaresFromHtml,
  scrapeMesaDiretoraFromHtml,
  parseBadgeCounts,
} = require('../lib/scraper-camara');

const fixturePath = path.join(__dirname, 'fixtures', 'parlamentares-page.html');

describe('scraper-camara Canindé/CE', () => {
  it('extrai vereadores reais do layout cmcaninde (.cardlist)', () => {
    const html = fs.readFileSync(fixturePath, 'utf8');
    const lista = scrapeParlamentaresFromHtml(html, cheerio);
    assert.ok(lista.length >= 2);
    const nomes = lista.map((p) => p.nome);
    assert.ok(nomes.some((n) => n.includes('Karlinda') || n.includes('Geovane')));
    assert.ok(nomes.some((n) => n.includes('Karlinda') || n.includes('Geovane')));
  });

  it('extrai badges de matérias e sessões', () => {
    const html = fs.readFileSync(fixturePath, 'utf8');
    const lista = scrapeParlamentaresFromHtml(html, cheerio);
    const karlinda = lista.find((p) => p.nome.includes('Karlinda'));
    assert.ok(karlinda);
    assert.equal(karlinda.totalMaterias, 3);
    assert.equal(karlinda.totalSessoes, 2);
  });

  it('extrai mesa diretora de .box-mesa-diretora', () => {
    const html = fs.readFileSync(fixturePath, 'utf8');
    const mesa = scrapeMesaDiretoraFromHtml(html, cheerio);
    assert.ok(mesa.some((m) => m.cargo.includes('Presidente')));
  });
});
