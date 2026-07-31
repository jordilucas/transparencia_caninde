'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { parseTargetUrl } = require('../lib/media-proxy');

describe('media-proxy', () => {
  it('extrai parâmetro url da query string', () => {
    const target = 'https://www.cmcaninde.ce.gov.br/wp-content/uploads/foto.jpg';
    const parsed = parseTargetUrl(`/media?url=${encodeURIComponent(target)}`);
    assert.equal(parsed, target);
  });

  it('rejeita URL ausente ou inválida', () => {
    assert.equal(parseTargetUrl('/media'), null);
    assert.equal(parseTargetUrl('/media?url='), '');
  });
});
