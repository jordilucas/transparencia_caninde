'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const cheerio = require('cheerio');
const {
  parseStandardTable,
  parseFuncaoTable,
  formatCompetencia,
  mapAgregadoRow,
} = require('../lib/scraper-sst-pessoal');
const { mergeFolhaPagamento, mapSstSecretariaToSetor } = require('../lib/folha-gt-merge');

const fixture = (name) =>
  fs.readFileSync(path.join(__dirname, 'fixtures', name), 'utf8');

describe('scraper-sst-pessoal', () => {
  it('formatCompetencia converte data ISO', () => {
    assert.equal(formatCompetencia('2026/01/01'), '01/2026');
  });

  it('parseStandardTable extrai secretarias agregadas', () => {
    const html = fixture('sst-pessoal-secretaria.html');
    const parsed = parseStandardTable(html, cheerio, /^(Secretaria|Natureza)$/i);
    assert.equal(parsed.competencia, '01/2026');
    assert.equal(parsed.rows.length, 2);
    assert.equal(parsed.rows[0].nome, 'SECRETARIA DA SAUDE');
    assert.equal(parsed.rows[0].servidores, 120);
    assert.ok(parsed.rows[0].brutoNumerico > 400000);
  });

  it('mapAgregadoRow ignora cabeçalho', () => {
    assert.equal(mapAgregadoRow(['Secretaria', '1', '1', '1', '1']), null);
  });

  it('parseFuncaoTable lê colunas extras', () => {
    const html = `
      <table>
        <tr><td>Função</td><td>Lei</td><td>Servidores</td><td>Limite</td><td>Bruto</td><td>Desconto</td><td>Líquido</td></tr>
        <tr><td>PROFESSOR</td><td>Lei 1</td><td>50</td><td>0</td><td>100.000,00</td><td>10.000,00</td><td>90.000,00</td></tr>
      </table>
      <input name="IWEDIT1" value="2026/01/01">
    `;
    const parsed = parseFuncaoTable(html, cheerio);
    assert.equal(parsed.rows.length, 1);
    assert.equal(parsed.rows[0].nome, 'PROFESSOR');
    assert.equal(parsed.rows[0].lei, 'Lei 1');
  });
});

describe('folha-gt-merge SST', () => {
  it('mergeFolhaPagamento prefere SST sobre GT e portal', () => {
    const sst = {
      disponivel: true,
      competencia: '01/2026',
      porSecretaria: [
        { nome: 'SAUDE', servidores: 10, brutoNumerico: 900000, bruto: 'R$ 900.000,00', liquidoNumerico: 800000, liquido: 'R$ 800.000,00' },
      ],
      porNatureza: [{ nome: 'EFETIVO', servidores: 100, brutoNumerico: 500000, bruto: 'R$ 500.000,00', liquidoNumerico: 400000, liquido: 'R$ 400.000,00' }],
      porFuncao: [],
      totalServidores: 100,
      fonteUrl: 'https://example.com/sst',
    };
    const merged = mergeFolhaPagamento(
      { competencias: [], porSetor: [{ secretaria: 'Portal', totalPagoNumerico: 1000, quantidadePagamentos: 1 }] },
      [{ secretaria: 'GT', totalPagoNumerico: 5000000, quantidadePagamentos: 3, totalPago: 'R$ 5.000.000,00' }],
      2026,
      {},
      sst,
    );
    assert.equal(merged.fontePorSetor, 'sst_quadro_pessoal');
    assert.equal(merged.porSetor[0].secretaria, 'SAUDE');
    assert.equal(merged.porSetor[0].quantidadePagamentos, 10);
    assert.equal(merged.porNatureza.length, 1);
    assert.equal(merged.competenciaSst, '01/2026');
    assert.equal(merged.referenciaCompetencia, '01/2026');
    assert.equal(merged.referenciaExercicio, 2026);
    assert.equal(merged.exercicio, 2026);
    assert.equal(merged.fonteSstUrl, 'https://example.com/sst');
  });

  it('mapSstSecretariaToSetor converte para FolhaSetorResumo', () => {
    const rows = mapSstSecretariaToSetor([
      { nome: 'EDUCACAO', servidores: 5, brutoNumerico: 1000, bruto: 'R$ 1.000,00' },
    ]);
    assert.equal(rows[0].secretaria, 'EDUCACAO');
    assert.equal(rows[0].quantidadePagamentos, 5);
  });
});
