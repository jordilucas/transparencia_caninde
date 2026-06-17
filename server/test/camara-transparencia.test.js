'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { buildLinksTransparenciaCamara } = require('../lib/scraper-camara-transparencia');

describe('scraper-camara-transparencia', () => {
  it('não inclui link de folha pessoal', () => {
    const links = buildLinksTransparenciaCamara();
    assert.ok(links.length >= 4);
    assert.ok(!links.some((l) => l.categoria === 'pessoal'));
    assert.ok(!links.some((l) => /despesasportipo\?tipo=5/i.test(l.url)));
  });
});
