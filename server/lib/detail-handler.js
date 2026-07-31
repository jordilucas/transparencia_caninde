'use strict';

const scraperCamara = require('./scraper-camara');
const detailCamara = require('./scraper-detail-camara');
const detailPref = require('./scraper-detail-prefeitura');
const portalPage = require('./scraper-portal-page');
const camaraPortal = require('./scraper-camara-portal');
const { mergeParlamentar, mergeMateria } = require('./merge-camara-sources');
const { createDetailCache } = require('./detail-cache');
const { assertAllowedOutboundUrl } = require('./allowed-hosts');

function idFromPortalUrl(url, page) {
  const m = String(url || '').match(new RegExp(`${page}\\?id=(\\d+)`, 'i'));
  return m ? m[1] : '';
}

function createDetailHandler({ http, cheerio, getCache }) {
  const detailCache = createDetailCache();

  async function fetchHtml(url) {
    assertAllowedOutboundUrl(url);
    const { data } = await http.get(url);
    return data;
  }

  function findContrato(cache, id) {
    const list = cache?.prefeitura?.contratos || [];
    return list.find(
      (c) => c.numero === id
        || String(c.id) === String(id)
        || idFromPortalUrl(c.url, 'contratos') === String(id),
    ) || null;
  }

  function findLicitacao(cache, id) {
    const list = cache?.prefeitura?.licitacoes || [];
    return list.find(
      (l) => l.numero === id
        || String(l.id) === String(id)
        || idFromPortalUrl(l.url, 'licitacaolista') === String(id),
    ) || null;
  }

  function findSessao(cache, id) {
    const list = cache?.camara?.sessoes || [];
    const raw = String(id || '');
    const fromList = list.find((s) => {
      if (s.slug && s.slug === raw) return true;
      if (s.titulo && s.titulo === raw) return true;
      if (s.url && raw && s.url.includes(raw)) return true;
      return false;
    });
    if (fromList) return fromList;
    if (/^\d+$/.test(raw)) {
      const idx = parseInt(raw, 10);
      if (list[idx]) return list[idx];
    }
    const slugFromPath = raw.match(/^(?:sessao|video)[/-](.+)$/i);
    if (slugFromPath) {
      const slug = slugFromPath[1].replace(/\/$/, '');
      return list.find((s) => s.slug === slug) || null;
    }
    return null;
  }

  function findPublicacao(cache, id) {
    const list = cache?.prefeitura?.publicacoes || [];
    return list.find((p) => String(p.id) === String(id)) || null;
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
        const listItem = (cache?.camara?.materias || []).find((m) => m.slug === id);
        if (listItem && result?.materia) {
          result.materia = mergeMateria(result.materia, listItem);
        }
        break;
      }
      case 'secretaria': {
        const html = await fetchHtml(`${detailPref.BASE}/secretaria.php?sec=${id}`);
        result = detailPref.scrapeSecretariaDetail(html, cheerio, id);
        const listItem = (cache?.prefeitura?.secretarias || []).find((s) => String(s.id) === String(id));
        if (listItem && result?.secretaria) {
          result.secretaria = {
            ...listItem,
            ...result.secretaria,
            nome: result.secretaria.nome || listItem.nome,
            secretario: listItem.secretario || result.secretaria.secretario,
            cargoGestor: listItem.cargoGestor || result.secretaria.cargoGestor || '',
            contato: {
              ...(listItem.contato || {}),
              ...(result.secretaria.contato || {}),
              email: listItem.contato?.email || result.secretaria.contato?.email || '',
              telefone: listItem.contato?.telefone || result.secretaria.contato?.telefone || '',
              endereco: listItem.contato?.endereco || result.secretaria.contato?.endereco || '',
              horarioFuncionamento: listItem.contato?.horarioFuncionamento
                || result.secretaria.contato?.horarioFuncionamento || '',
            },
            resumoFinanceiro: listItem.resumoFinanceiro || result.secretaria.resumoFinanceiro,
            contratos: listItem.contratos || [],
            licitacoes: listItem.licitacoes || [],
            projetosAndamento: listItem.projetosAndamento || [],
          };
        }
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
        let c = findContrato(cache, id);
        const detailUrl = c?.url || `${detailPref.BASE}/contratos.php?id=${c?.id || id}`;
        try {
          const html = await fetchHtml(detailUrl);
          const scraped = detailPref.scrapeContratoDetail(html, cheerio, id);
          const base = c || {
            id: String(id),
            numero: scraped?.numero || String(id),
            url: detailUrl,
          };
          result = {
            entity: 'contrato',
            entityId: id,
            contrato: detailPref.mergeContratoDetail(base, scraped),
          };
        } catch (err) {
          if (c) {
            result = { entity: 'contrato', entityId: id, contrato: c };
          } else {
            result = {
              entity: 'contrato',
              entityId: id,
              error: err.message || 'Contrato não encontrado.',
            };
          }
        }
        break;
      }
      case 'licitacao': {
        let l = findLicitacao(cache, id);
        const detailUrl = l?.url || `${detailPref.BASE}/licitacaolista.php?id=${l?.id || id}`;
        try {
          const html = await fetchHtml(detailUrl);
          const scraped = detailPref.scrapeLicitacaoDetail(html, cheerio, id);
          const base = l || {
            id: String(id),
            numero: scraped?.numero || String(id),
            url: detailUrl,
          };
          result = {
            entity: 'licitacao',
            entityId: id,
            licitacao: detailPref.mergeLicitacaoDetail(base, scraped),
          };
        } catch (err) {
          if (l) {
            result = { entity: 'licitacao', entityId: id, licitacao: l };
          } else {
            result = {
              entity: 'licitacao',
              entityId: id,
              error: err.message || 'Licitação não encontrada.',
            };
          }
        }
        break;
      }
      case 'sessao': {
        let s = findSessao(cache, id);
        if (!s && id && !/^\d+$/.test(String(id))) {
          s = { slug: id, url: `${scraperCamara.BASE}/sessao/${id}/`, titulo: id };
        }
        if (!s) {
          result = {
            entity: 'sessao',
            entityId: id,
            error: 'Sessão não encontrada na listagem atual.',
          };
          break;
        }
        const isVideo = s.url && /\/video\//i.test(s.url);
        const sessaoUrl = isVideo
          ? s.url
          : (s.url && /\/sessao\//i.test(s.url)
            ? s.url
            : (s.slug ? `${scraperCamara.BASE}/sessao/${s.slug}/` : ''));
        const videoUrl = isVideo
          ? s.url
          : (s.slug ? `${scraperCamara.BASE}/video/${s.slug}/` : '');
        if (sessaoUrl) {
          try {
            let html = await fetchHtml(sessaoUrl);
            let scraped = detailCamara.scrapeSessaoDetail(html, cheerio, s.slug || id);
            if (!scraped.videoEmbedUrl && videoUrl && videoUrl !== sessaoUrl) {
              try {
                const videoHtml = await fetchHtml(videoUrl);
                const videoScraped = detailCamara.scrapeSessaoDetail(videoHtml, cheerio, s.slug || id);
                if (videoScraped.videoEmbedUrl) {
                  scraped = { ...scraped, videoEmbedUrl: videoScraped.videoEmbedUrl };
                }
              } catch {
                // página de vídeo opcional
              }
            }
            result = {
              entity: 'sessao',
              entityId: id,
              sessao: detailCamara.mergeSessaoDetail(
                { ...s, url: isVideo ? s.url : (s.url || sessaoUrl) },
                scraped,
              ),
            };
          } catch {
            result = { entity: 'sessao', entityId: id, sessao: s };
          }
        } else {
          result = { entity: 'sessao', entityId: id, sessao: s };
        }
        break;
      }
      case 'documento_camara': {
        const url = portalPage.decodePortalPageId(id);
        if (!url) {
          result = { entity: 'documento_camara', entityId: id, error: 'URL inválida.' };
          break;
        }
        const listItem = (cache?.camara?.documentosTransparencia || []).find((d) => d.url === url);
        try {
          const html = await fetchHtml(url);
          const scraped = camaraPortal.scrapeDocumentoCamaraDetail(html, cheerio, url);
          result = {
            entity: 'documento_camara',
            entityId: id,
            documentoCamara: camaraPortal.mergeDocumentoCamara(listItem, scraped),
          };
        } catch (err) {
          result = {
            entity: 'documento_camara',
            entityId: id,
            documentoCamara: listItem || { url, titulo: url },
            error: err.message || 'Documento não encontrado.',
          };
        }
        break;
      }
      case 'publicacao': {
        const p = findPublicacao(cache, id);
        const detailUrl = p?.url || `${detailPref.BASE}/publicacoes.php?id=${id}`;
        try {
          const html = await fetchHtml(detailUrl);
          const scraped = portalPage.scrapePublicacaoDetail(html, cheerio, id);
          result = {
            entity: 'publicacao',
            entityId: id,
            publicacao: portalPage.mergePublicacaoDetail(
              p || { id, url: detailUrl },
              scraped,
            ),
          };
        } catch {
          result = {
            entity: 'publicacao',
            entityId: id,
            publicacao: p || { id, url: detailUrl },
          };
        }
        break;
      }
      case 'pagina_portal': {
        const url = portalPage.decodePortalPageId(id);
        if (!url) {
          result = { entity: 'pagina_portal', entityId: id, error: 'URL inválida.' };
          break;
        }
        if (!portalPage.isAllowedPortalUrl(url)) {
          result = {
            entity: 'pagina_portal',
            entityId: id,
            error: 'URL fora dos portais oficiais permitidos.',
          };
          break;
        }
        const meta = portalPage.findLinkMeta(cache, url);
        try {
          const html = await fetchHtml(url);
          const scraped = portalPage.scrapePortalPage(html, cheerio, url);
          result = {
            entity: 'pagina_portal',
            entityId: id,
            paginaPortal: portalPage.mergePortalPageMeta(scraped, meta, url),
          };
        } catch (err) {
          result = {
            entity: 'pagina_portal',
            entityId: id,
            paginaPortal: portalPage.fallbackPortalPage(url, meta, err),
          };
        }
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
