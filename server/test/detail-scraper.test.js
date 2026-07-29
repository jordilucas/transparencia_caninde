'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const fs = require('fs');
const path = require('path');
const { scrapeVereadorDetail } = require('../lib/scraper-detail-camara');
const { scrapeSecretariaDetail, scrapeContratoDetail, scrapeLicitacaoDetail, mergeContratoDetail } = require('../lib/scraper-detail-prefeitura');
const { scrapePublicacaoDetail, scrapePortalPage, mergePublicacaoDetail } = require('../lib/scraper-portal-page');

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

  it('resolveHref absolutiza PDF relativo', () => {
    const { resolveHref } = require('../lib/scraper-detail-camara');
    assert.equal(
      resolveHref('/wp-content/uploads/doc.pdf'),
      'https://www.cmcaninde.ce.gov.br/wp-content/uploads/doc.pdf',
    );
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

  it('extrai detalhe de contrato do portal', () => {
    const html = fs.readFileSync(path.join(__dirname, 'fixtures/contrato-detail-snippet.html'), 'utf8');
    const scraped = scrapeContratoDetail(html, cheerio, '1117');
    assert.ok(scraped.objeto.includes('TRANSPORTE ESCOLAR'));
    assert.equal(scraped.empresa, 'DOMINGOS DENES DOS SANTOS LOPES');
    assert.ok(scraped.pdfUrl.includes('.PDF'));
    const merged = mergeContratoDetail({ numero: '10072026-001', objeto: 'curto' }, scraped);
    assert.ok(merged.objeto.length > scraped.objeto.length / 2);
    assert.ok(merged.anexos.length >= 1);
  });

  it('extrai detalhe de licitação do portal', () => {
    const html = fs.readFileSync(path.join(__dirname, 'fixtures/licitacao-detail-snippet.html'), 'utf8');
    const scraped = scrapeLicitacaoDetail(html, cheerio, '521');
    assert.ok(scraped.objeto.includes('GALPÃO'));
    assert.ok(scraped.valorEstimado.includes('2.488'));
    assert.ok(scraped.andamentos.length >= 1);
    assert.ok(scraped.anexos.length >= 1);
  });

  it('extrai detalhe de publicação do portal', () => {
    const html = fs.readFileSync(path.join(__dirname, 'fixtures/publicacao-detail-snippet.html'), 'utf8');
    const scraped = scrapePublicacaoDetail(html, cheerio, '1187');
    assert.ok(scraped.titulo.includes('001/2026'));
    assert.ok(scraped.resumo.includes('CONTRATAÇÕES'));
    assert.equal(scraped.data, '01/01/2026');
    assert.ok(scraped.anexos[0].url.includes('.pdf'));
    const merged = mergePublicacaoDetail({ id: '1187', titulo: 'Plano' }, scraped);
    assert.ok(merged.anexos.length >= 1);
  });
});

describe('scraper-portal-page', () => {
  it('extrai título de página WordPress da Câmara', () => {
    const html = `
      <html><head>
        <meta property="og:title" content="Canindé Transparente - Câmara" />
      </head><body>
        <article><h1 class="entry-title">Canindé Transparente</h1>
        <div class="entry-content"><p>Portal de transparência legislativa com acesso a contas públicas.</p>
        <a href="/wp-content/uploads/doc.pdf">Baixar PDF</a></div></article>
      </body></html>
    `;
    const scraped = scrapePortalPage(html, cheerio, 'https://www.cmcaninde.ce.gov.br/caninde-transparente/');
    assert.ok(scraped.titulo.includes('Transparente'));
    assert.ok(scraped.resumo.includes('transparência'));
    assert.ok(scraped.anexos.some((a) => a.url.includes('.pdf')));
  });
});
