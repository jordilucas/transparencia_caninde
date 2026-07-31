'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const { scrapeSessaoDetail } = require('../lib/scraper-detail-camara');

describe('scraper-detail-camara video', () => {
  it('scrapeSessaoDetail extrai embed do YouTube', () => {
    const html = `
      <article>
        <h1>Sessão Ordinária</h1>
        <iframe src="https://www.youtube.com/embed/dQw4w9WgXcQ"></iframe>
      </article>
    `;
    const detail = scrapeSessaoDetail(html, cheerio, 'sessao-ordinaria');
    assert.match(detail.videoEmbedUrl, /youtube\.com\/embed\/dQw4w9WgXcQ/);
  });
});
