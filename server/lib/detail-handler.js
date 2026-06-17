'use strict';

const scraperCamara = require('./scraper-camara');
const detailCamara = require('./scraper-detail-camara');
const detailPref = require('./scraper-detail-prefeitura');
const { mergeParlamentar } = require('./merge-camara-sources');
const { createDetailCache } = require('./detail-cache');

function createDetailHandler({ http, cheerio, getCache }) {
  const detailCache = createDetailCache();

  async function fetchHtml(url) {
    const { data } = await http.get(url);
    return data;
  }

  function findContrato(cache, id) {
    const list = cache?.prefeitura?.contratos || [];
    return list.find((c) => c.numero === id || String(c.id) === String(id)) || null;
  }

  function findLicitacao(cache, id) {
    const list = cache?.prefeitura?.licitacoes || [];
    return list.find((l) => l.numero === id || String(l.id) === String(id)) || null;
  }

  function findSessao(cache, id) {
    const list = cache?.camara?.sessoes || [];
    const idx = parseInt(id, 10);
    if (!Number.isNaN(idx) && list[idx]) return list[idx];
    return list.find((s) => s.slug === id || s.titulo === id) || null;
  }

  async function loadDetail(entity, id) {
    const cached = detailCache.get(entity, id);
    if (cached) return cached;

    const cache = getCache();
    let result = null;

    switch (entity) {
      case 'vereador': {
        const html = await fetchHtml(`${scraperCamara.BASE}/vereadores/${id}/`);
        result = detailCamara.scrapeVereadorDetail(html, cheerio, id);
        const listItem = (cache?.camara?.parlamentares || []).find((p) => p.slug === id);
        if (listItem && result?.parlamentar) {
          result.parlamentar = mergeParlamentar(result.parlamentar, listItem);
        }
        break;
      }
      case 'materia': {
        const html = await fetchHtml(`${scraperCamara.BASE}/materia/${id}/`);
        result = detailCamara.scrapeMateriaDetail(html, cheerio, id);
        break;
      }
      case 'secretaria': {
        const html = await fetchHtml(`${detailPref.BASE}/secretaria.php?sec=${id}`);
        result = detailPref.scrapeSecretariaDetail(html, cheerio, id);
        break;
      }
      case 'gestor':
      case 'gestores': {
        const html = await fetchHtml(`${detailPref.BASE}/gestores.php`);
        result = detailPref.scrapeGestores(html, cheerio);
        break;
      }
      case 'institucional': {
        if (id === 'camara') {
          const html = await fetchHtml(`${scraperCamara.BASE}/`);
          result = detailCamara.scrapeInstitucionalCamara(html, cheerio);
        } else {
          const html = await fetchHtml(`${detailPref.BASE}/acessoainformacao.php`);
          result = detailPref.scrapeInstitucionalPrefeitura(html, cheerio);
        }
        break;
      }
      case 'contrato': {
        const c = findContrato(cache, id);
        result = {
          entity: 'contrato',
          entityId: id,
          contrato: c || { numero: id, objeto: '', valor: '', empresa: '', data: '', url: '' },
          error: c ? null : 'Contrato não encontrado na listagem atual.',
        };
        break;
      }
      case 'licitacao': {
        const l = findLicitacao(cache, id);
        result = {
          entity: 'licitacao',
          entityId: id,
          licitacao: l || { numero: id, modalidade: '', objeto: '', situacao: '', url: '' },
          error: l ? null : 'Licitação não encontrada na listagem atual.',
        };
        break;
      }
      case 'sessao': {
        const s = findSessao(cache, id);
        result = {
          entity: 'sessao',
          entityId: id,
          sessao: s || { titulo: id, data: '', url: '', resumo: '' },
          error: s ? null : 'Sessão não encontrada na listagem atual.',
        };
        break;
      }
      default:
        return { error: `Entidade desconhecida: ${entity}` };
    }

    if (result && !result.error) {
      detailCache.set(entity, id, result);
    }
    return result;
  }

  return { loadDetail };
}

module.exports = { createDetailHandler };
