'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  parseWsMessage,
  handleWsMessage,
  buildServerStatusPayload,
  checkWsAuth,
} = require('../lib/ws-handler');

describe('parseWsMessage', () => {
  it('parseia JSON de REQUEST_PREFEITURA', () => {
    const msg = parseWsMessage('{"type":"REQUEST_PREFEITURA"}');
    assert.equal(msg.type, 'REQUEST_PREFEITURA');
  });
});

describe('handleWsMessage', () => {
  it('REQUEST_PREFEITURA retorna PREFEITURA_DATA', () => {
    const out = handleWsMessage({ type: 'REQUEST_PREFEITURA' });
    assert.equal(out.length, 1);
    assert.equal(out[0].type, 'PREFEITURA_DATA');
    assert.equal(out[0].source, 'prefeitura');
  });

  it('REQUEST_CAMARA retorna CAMARA_DATA', () => {
    const out = handleWsMessage({ type: 'REQUEST_CAMARA' });
    assert.equal(out[0].type, 'CAMARA_DATA');
  });

  it('PING retorna PONG', () => {
    const out = handleWsMessage({ type: 'PING' });
    assert.equal(out[0].type, 'PONG');
  });

  it('REQUEST_REFRESH all inclui REFRESHING e duas fontes', () => {
    const out = handleWsMessage({ type: 'REQUEST_REFRESH', source: 'all' });
    assert.equal(out[0].type, 'REFRESHING');
    assert.equal(out.filter((r) => r.broadcast).length, 2);
  });

  it('REQUEST_REFRESH prefeitura só atualiza prefeitura', () => {
    const out = handleWsMessage({ type: 'REQUEST_REFRESH', source: 'prefeitura' });
    assert.equal(out.length, 2);
    assert.equal(out[1].source, 'prefeitura');
    assert.equal(out[1].forceScrape, true);
  });

  it('REQUEST_DETAIL retorna DETAIL_DATA com entity e id', () => {
    const out = handleWsMessage({
      type: 'REQUEST_DETAIL',
      payload: { entity: 'vereador', id: 'karlinda-coelho' },
    });
    assert.equal(out.length, 1);
    assert.equal(out[0].type, 'DETAIL_DATA');
    assert.equal(out[0].entity, 'vereador');
    assert.equal(out[0].entityId, 'karlinda-coelho');
    assert.equal(out[0].detailRequest, true);
  });
});

describe('buildServerStatusPayload', () => {
  it('inclui lastUpdated do cache', () => {
    const cache = { lastUpdated: { prefeitura: '2025-01-01', camara: null } };
    const p = buildServerStatusPayload(cache, { prefeitura: 60000, camara: 90000 });
    assert.equal(p.version, '1.0.0');
    assert.equal(p.lastUpdated.prefeitura, '2025-01-01');
  });
});

describe('checkWsAuth', () => {
  it('permite sem token configurado', () => {
    assert.equal(checkWsAuth({ url: '/' }, ''), true);
  });

  it('rejeita token incorreto', () => {
    assert.equal(checkWsAuth({ url: '/?token=wrong' }, 'secret'), false);
  });

  it('aceita token correto na query', () => {
    assert.equal(checkWsAuth({ url: '/?token=secret' }, 'secret'), true);
  });
});
