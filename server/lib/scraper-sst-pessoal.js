'use strict';

const axios = require('axios');
const { parseBRLNumber, formatBRL } = require('./brl');
const { assertAllowedOutboundUrl } = require('./allowed-hosts');

const ORIGIN = 'https://www.sstransparenciamunicipal.net';
const API = `${ORIGIN}/transparencia/transparenciaisapi.dll`;
const ENCODED_ENTCOD = 117;
const PORTAL_URL = `${API}/$/?entcod=${ENCODED_ENTCOD}`;
const USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

function formatCompetencia(isoDate) {
  const m = String(isoDate || '').match(/(\d{4})[/-](\d{2})/);
  if (!m) return '';
  return `${m[2]}/${m[1]}`;
}

function mapAgregadoRow(cells, opts = {}) {
  const { labelIndex = 0, servidoresIndex = 1, brutoIndex = 2, descontoIndex = 3, liquidoIndex = 4 } = opts;
  const nome = String(cells[labelIndex] || '').replace(/\s+/g, ' ').trim();
  if (!nome || /^(Secretaria|Natureza|Função|Função)$/i.test(nome)) return null;

  const brutoNumerico = parseBRLNumber(cells[brutoIndex]);
  const liquidoNumerico = parseBRLNumber(cells[liquidoIndex]);
  const servidores = parseInt(String(cells[servidoresIndex] || '').replace(/\D/g, ''), 10) || 0;
  if (brutoNumerico <= 0 && liquidoNumerico <= 0 && servidores <= 0) return null;

  return {
    nome,
    servidores,
    bruto: formatBRL(cells[brutoIndex]) || formatBRL(brutoNumerico),
    brutoNumerico,
    desconto: formatBRL(cells[descontoIndex]) || '',
    liquido: formatBRL(cells[liquidoIndex]) || formatBRL(liquidoNumerico),
    liquidoNumerico,
    lei: opts.leiIndex != null ? String(cells[opts.leiIndex] || '').trim() : '',
  };
}

function parseStandardTable(html, cheerio, headerPattern) {
  const $ = cheerio.load(html);
  const rows = [];
  $('table tr').each((_, tr) => {
    const cells = $(tr).find('td').map((_, cell) => $(cell).text().replace(/\s+/g, ' ').trim()).get();
    if (cells.length < 5) return;
    if (headerPattern.test(cells[0])) return;
    const item = mapAgregadoRow(cells);
    if (item) rows.push(item);
  });
  const period = ($('input[name="IWEDIT1"]').attr('value') || '').trim();
  return { rows, competencia: formatCompetencia(period) };
}

function parseFuncaoTable(html, cheerio) {
  const $ = cheerio.load(html);
  const rows = [];
  $('table tr').each((_, tr) => {
    const cells = $(tr).find('td').map((_, cell) => $(cell).text().replace(/\s+/g, ' ').trim()).get();
    if (cells.length < 7) return;
    if (/^Função$/i.test(cells[0])) return;
    const item = mapAgregadoRow(cells, {
      labelIndex: 0,
      leiIndex: 1,
      servidoresIndex: 2,
      brutoIndex: 4,
      descontoIndex: 5,
      liquidoIndex: 6,
    });
    if (item) rows.push(item);
  });
  const period = ($('input[name="IWEDIT1"]').attr('value') || '').trim();
  return { rows, competencia: formatCompetencia(period) };
}

function extractFormFields($, clickName, clickValue) {
  const params = new URLSearchParams();
  $('input[name]').each((_, el) => {
    const name = $(el).attr('name');
    const type = String($(el).attr('type') || '').toLowerCase();
    if (!name) return;
    if (type === 'submit' || type === 'button') {
      if (name === clickName) params.set(name, clickValue);
      return;
    }
    params.set(name, $(el).attr('value') ?? '');
  });
  if (clickName && !params.has(clickName)) params.set(clickName, clickValue);
  return params;
}

class SstSessionClient {
  constructor(http) {
    this.http = http;
    this.cookies = [];
  }

  mergeCookies(setCookie) {
    if (!setCookie) return;
    const list = Array.isArray(setCookie) ? setCookie : [setCookie];
    for (const raw of list) {
      const name = raw.split('=')[0];
      this.cookies = this.cookies.filter((item) => !item.startsWith(`${name}=`));
      this.cookies.push(raw.split(';')[0]);
    }
  }

  headers(extra = {}) {
    return {
      'User-Agent': USER_AGENT,
      Accept: 'text/html,*/*',
      ...(this.cookies.length ? { Cookie: this.cookies.join('; ') } : {}),
      ...extra,
    };
  }

  async get(url, config = {}) {
    assertAllowedOutboundUrl(url);
    const response = await this.http.get(url, {
      headers: this.headers(config.headers),
      responseType: 'text',
      transformResponse: [(body) => body],
      validateStatus: (status) => status >= 200 && status < 500,
      timeout: 30_000,
      ...config,
    });
    this.mergeCookies(response.headers['set-cookie']);
    return response;
  }

  async post(url, body, config = {}) {
    assertAllowedOutboundUrl(url);
    const response = await this.http.post(url, body, {
      headers: this.headers({
        'Content-Type': 'application/x-www-form-urlencoded',
        ...config.headers,
      }),
      responseType: 'text',
      transformResponse: [(raw) => raw],
      validateStatus: (status) => status >= 200 && status < 500,
      timeout: 30_000,
      ...config,
    });
    this.mergeCookies(response.headers['set-cookie']);
    return response;
  }

  async open(entcod = ENCODED_ENTCOD) {
    const boot = await this.get(`${API}/$/?entcod=${entcod}`);
    const startCheckMatch = String(boot.data).match(
      /get\("(\/transparencia\/transparenciaisapi\.dll\/[^"]+StartCheck\?entcod=\d+)"/,
    );
    if (!startCheckMatch) throw new Error('SST StartCheck não encontrado');

    const check = await this.get(
      `${ORIGIN}${startCheckMatch[1]}&IW_AjaxID=${Date.now()}&IW_width=1280&IW_height=800`,
      { headers: { Referer: `${API}/$/?entcod=${entcod}` } },
    );
    const formMatch = String(check.data).match(
      /action="([^"]+)"[\s\S]*?IW_SessionID_" value="([^"]+)"[\s\S]*?IW_TrackID_" value="([^"]+)"/,
    );
    if (!formMatch) throw new Error('SST formulário de sessão não encontrado');

    this.sessionId = formMatch[2];
    this.baseUrl = `${API}/${this.sessionId}`;
    const body = new URLSearchParams({
      IW_width: '1280',
      IW_height: '800',
      IW_SessionID_: formMatch[2],
      IW_TrackID_: formMatch[3],
    });
    await this.post(`${ORIGIN}${formMatch[1]}`, body.toString(), {
      headers: { Referer: `${ORIGIN}${startCheckMatch[1]}` },
    });
    this.portalUrl = `${this.baseUrl}/$/?entcod=${entcod}`;
    return this;
  }

  async fetchPath(path, { paginate = false, cheerio } = {}) {
    const url = `${this.baseUrl}/${path}`;
    let response = await this.get(url);
    let parsed = path === 'PESSOALFUNCAO'
      ? parseFuncaoTable(response.data, cheerio)
      : parseStandardTable(response.data, cheerio, /^(Secretaria|Natureza)$/i);

    if (paginate) {
      const $ = cheerio.load(response.data);
      const nextBody = extractFormFields($, 'IWBUTTON2', '»');
      response = await this.post(url, nextBody.toString());
      const nextParsed = path === 'PESSOALFUNCAO'
        ? parseFuncaoTable(response.data, cheerio)
        : parseStandardTable(response.data, cheerio, /^(Secretaria|Natureza)$/i);
      parsed.rows.push(...nextParsed.rows);
    }

    return parsed;
  }
}

async function scrapeSstPessoal(cheerio, exercicio = new Date().getFullYear()) {
  const client = new SstSessionClient(axios);

  try {
    await client.open(ENCODED_ENTCOD);

    const secretaria = await client.fetchPath('PESSOALSECRETARIA', { paginate: true, cheerio });
    const natureza = await client.fetchPath('PESSOALNATUREZA', { cheerio });
    const funcao = await client.fetchPath('PESSOALFUNCAO', { cheerio });

    const competencia = secretaria.competencia || natureza.competencia || funcao.competencia || '';
    const totalServidores = natureza.rows.reduce((sum, row) => sum + (row.servidores || 0), 0);
    const disponivel = secretaria.rows.length > 0 || natureza.rows.length > 0 || funcao.rows.length > 0;

    if (!disponivel) {
      return {
        disponivel: false,
        exercicio,
        competencia: '',
        porSecretaria: [],
        porNatureza: [],
        porFuncao: [],
        totalServidores: 0,
        fonteUrl: PORTAL_URL,
      };
    }

    return {
      disponivel: true,
      exercicio,
      competencia,
      porSecretaria: secretaria.rows.sort((a, b) => b.brutoNumerico - a.brutoNumerico),
      porNatureza: natureza.rows.sort((a, b) => b.brutoNumerico - a.brutoNumerico),
      porFuncao: funcao.rows.sort((a, b) => b.brutoNumerico - a.brutoNumerico),
      totalServidores,
      funcaoParcial: funcao.rows.length > 0,
      fonteUrl: client.portalUrl || PORTAL_URL,
    };
  } catch (err) {
    console.warn('[SST] quadro de pessoal indisponível:', err.message);
    return {
      disponivel: false,
      exercicio,
      competencia: '',
      porSecretaria: [],
      porNatureza: [],
      porFuncao: [],
      totalServidores: 0,
      fonteUrl: PORTAL_URL,
      error: err.message,
    };
  }
}

module.exports = {
  ORIGIN,
  API,
  PORTAL_URL,
  ENCODED_ENTCOD,
  formatCompetencia,
  mapAgregadoRow,
  parseStandardTable,
  parseFuncaoTable,
  SstSessionClient,
  scrapeSstPessoal,
};
