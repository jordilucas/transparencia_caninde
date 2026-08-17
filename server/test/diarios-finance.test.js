'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const { scrapeDiariosEstruturados } = require('../lib/scraper-prefeitura');
const { buildResumoFinanceiro } = require('../lib/finance-summary');

describe('scraper-prefeitura diarios', () => {
  it('scrapeDiariosEstruturados extrai PDF e data', () => {
    const html = `
      <a href='diario/1142/1068_2026_0000001.pdf' class='list-group-item'>
        <h4>DIÁRIO: 1068/2026 14/08/2026</h4>
      </a>
    `;
    const $ = cheerio.load(html);
    const list = scrapeDiariosEstruturados($, 5);
    assert.equal(list.length, 1);
    assert.equal(list[0].numero, '1068/2026');
    assert.equal(list[0].data, '14/08/2026');
    assert.ok(list[0].pdfUrl.includes('.pdf'));
  });
});

describe('finance-summary', () => {
  it('buildResumoFinanceiro soma contratos', () => {
    const resumo = buildResumoFinanceiro(
      [{ valorNumerico: 1000 }, { valorNumerico: 2500.5 }],
      [{ situacao: 'Aberta' }, { situacao: 'Encerrada' }],
    );
    assert.equal(resumo.totalContratos, 2);
    assert.ok(resumo.totalContratosValor.includes('3.500'));
    assert.equal(resumo.licitacoesAbertas, 1);
    assert.ok(resumo.gtReceitasUrl.includes('11979490'));
  });
});
