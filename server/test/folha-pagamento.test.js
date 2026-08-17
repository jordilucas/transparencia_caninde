'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const {
  scrapeFolhaMensal,
  scrapeFolhaPorSetor,
  parseBRL,
} = require('../lib/scraper-folha-pagamento');

describe('scraper-folha-pagamento', () => {
  it('parseBRL converte formato brasileiro', () => {
    assert.equal(parseBRL('40.736,99'), 40736.99);
  });

  it('scrapeFolhaMensal extrai competências agregadas', () => {
    const html = `
      <table><tbody>
        <tr>
          <td>JANEIRO/2024</td>
          <td>12.659.761,15</td>
          <td>3.674.848,75</td>
          <td>8.984.912,40</td>
        </tr>
      </tbody></table>`;
    const $ = cheerio.load(html);
    const rows = scrapeFolhaMensal($);
    assert.equal(rows.length, 1);
    assert.match(rows[0].competencia, /JANEIRO\/2024/);
    assert.ok(rows[0].liquido.includes('8.984.912,40') || rows[0].liquido.includes('R$'));
  });

  it('scrapeFolhaPorSetor agrega folha sem nomes', () => {
    const html = `
      <table><tbody>
        <tr>
          <td><span itemprop="creditorName">FOLHA DE PAGAMENTO - SEC. DE SAUDE</span></td>
          <td><strong itemprop="managementUnitID">08 - SECRETARIA MUNICIPAL DE SAUDE - SMS</strong></td>
          <td itemprop="paymentAmount"><strong>100,00</strong></td>
        </tr>
        <tr>
          <td><span itemprop="creditorName">FOLHA DE PAGAMENTO - SEC. DE SAUDE</span></td>
          <td><strong itemprop="managementUnitID">08 - SECRETARIA MUNICIPAL DE SAUDE - SMS</strong></td>
          <td itemprop="paymentAmount"><strong>50,00</strong></td>
        </tr>
        <tr>
          <td><span itemprop="creditorName">FORNECEDOR XYZ</span></td>
          <td><strong itemprop="managementUnitID">01 - GABINETE</strong></td>
          <td itemprop="paymentAmount"><strong>999,00</strong></td>
        </tr>
      </tbody></table>`;
    const $ = cheerio.load(html);
    const setores = scrapeFolhaPorSetor($);
    assert.equal(setores.length, 1);
    assert.match(setores[0].secretaria, /SAUDE/);
    assert.equal(setores[0].quantidadePagamentos, 2);
    assert.equal(setores[0].totalPagoNumerico, 150);
  });
});
