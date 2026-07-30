'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  createRefreshGuard,
  createScrapeLock,
} = require('../lib/scrape-guard');

describe('scrape-guard', () => {
  it('refresh guard respeita cooldown', () => {
    const guard = createRefreshGuard(1000);
    assert.equal(guard.canRefresh('prefeitura'), true);
    guard.markRefreshed('prefeitura');
    assert.equal(guard.canRefresh('prefeitura'), false);
    assert.equal(guard.canRefresh('camara'), true);
  });

  it('scrape lock deduplica execuções simultâneas', async () => {
    const lock = createScrapeLock();
    let runs = 0;
    const task = () => new Promise((resolve) => {
      runs += 1;
      setTimeout(resolve, 20);
    });

    const [a, b] = await Promise.all([
      lock.run('prefeitura', task),
      lock.run('prefeitura', task),
    ]);
    assert.equal(runs, 1);
    assert.equal(a, undefined);
    assert.equal(b, undefined);
  });
});
