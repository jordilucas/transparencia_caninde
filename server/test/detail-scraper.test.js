'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const fs = require('fs');
const path = require('path');
const { scrapeVereadorDetail } = require('../lib/scraper-detail-camara');
const { scrapeSecretariaDetail } = require('../lib/scraper-detail-prefeitura');

describe('scraper-detail-camara', () => {
  it('extrai contato do HTML de vereador', () => {
    const html = `
      <html><body>
        <h1>Karlinda Coelho</h1>
        <p>Parlamentar: Karlinda Coelho da Silva</p>
        <p class="cargo">Cargo: Presidente - REPUBLICANOS</p>
        <p>E-mail: vereador@cmcaninde.ce.gov.br</p>
        <p>Endereço: Largo Francisco Xavier</p>
        <p>De Segunda a Sexta das 08:00 às 17:00</p>
      </body></html>
    `;
    const r = scrapeVereadorDetail(html, cheerio, 'karlinda-coelho');
    assert.equal(r.entity, 'vereador');
    assert.ok(r.parlamentar.nome.includes('Karlinda'));
    assert.equal(r.parlamentar.contato.email, 'vereador@cmcaninde.ce.gov.br');
    assert.ok(r.parlamentar.contato.horarioFuncionamento.includes('Segunda'));
  });

  it('normalizeWhatsapp extrai número e rejeita URL de compartilhamento', () => {
    const { normalizeWhatsapp } = require('../lib/scraper-detail-camara');
    assert.equal(normalizeWhatsapp('https://wa.me/5585987112233'), '5585987112233');
    assert.equal(normalizeWhatsapp('https://wa.me/?text=x'), '');
    assert.equal(normalizeWhatsapp('https://www.addtoany.com/share'), '');
  });
});

describe('scraper-detail-prefeitura', () => {
  it('extrai secretaria com secretário', () => {
    const html = `
      <h1>Secretaria de Educação</h1>
      <h6>Secretário(a): João Silva</h6>
      <p>Horário: 08h às 17h</p>
      <p>Endereço: Rua Principal, 100</p>
    `;
    const r = scrapeSecretariaDetail(html, cheerio, '3');
    assert.equal(r.secretaria.nome, 'Secretaria de Educação');
    assert.ok(r.secretaria.secretario.includes('João'));
  });
});
