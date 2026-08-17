'use strict';

/**
 * Auditoria de consistência — contratos e licitações (dados abertos Prefeitura).
 * Uso: node scripts/audit-dados.js [ano...]
 */

const axios = require('axios');
const dadosAbertos = require('../lib/scraper-prefeitura-dadosabertos');

const http = axios.create({ timeout: 30_000 });

function onlyDigits(value) {
  return String(value || '').replace(/\D/g, '');
}

function formatCnpj(digits) {
  if (digits.length !== 14) return digits;
  return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12)}`;
}

function formatCpf(digits) {
  if (digits.length !== 11) return digits;
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

function cpfCheckDigit(digits, length) {
  let sum = 0;
  for (let i = 0; i < length; i += 1) {
    sum += Number(digits[i]) * (length + 1 - i);
  }
  const mod = (sum * 10) % 11;
  return mod === 10 ? 0 : mod;
}

function isValidCpf(digits) {
  if (digits.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(digits)) return false;
  const d1 = cpfCheckDigit(digits, 9);
  const d2 = cpfCheckDigit(digits, 10);
  return d1 === Number(digits[9]) && d2 === Number(digits[10]);
}

function isValidCnpj(digits) {
  if (digits.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(digits)) return false;
  const w1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const w2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  let sum = 0;
  for (let i = 0; i < 12; i += 1) sum += Number(digits[i]) * w1[i];
  let mod = sum % 11;
  const d1 = mod < 2 ? 0 : 11 - mod;
  sum = 0;
  for (let i = 0; i < 12; i += 1) sum += Number(digits[i]) * w2[i];
  sum += d1 * w2[12];
  mod = sum % 11;
  const d2 = mod < 2 ? 0 : 11 - mod;
  return d1 === Number(digits[12]) && d2 === Number(digits[13]);
}

function classifyDocument(raw) {
  const digits = onlyDigits(raw);
  if (!digits) return { kind: 'ausente', digits: '', valid: false };
  if (digits.length === 11) {
    return { kind: 'cpf', digits, valid: isValidCpf(digits), formatted: formatCpf(digits) };
  }
  if (digits.length === 14) {
    return { kind: 'cnpj', digits, valid: isValidCnpj(digits), formatted: formatCnpj(digits) };
  }
  return { kind: 'invalido', digits, valid: false };
}

function normalizeName(name) {
  return String(name || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toUpperCase();
}

async function loadYear(ano) {
  const [contratosRaw, licitacoesRaw] = await Promise.all([
    dadosAbertos.fetchDataset(http, 'contratos', ano),
    dadosAbertos.fetchDataset(http, 'licitacoes', ano),
  ]);
  return {
    ano,
    contratos: dadosAbertos.mapContratos(contratosRaw),
    licitacoes: dadosAbertos.mapLicitacoes(licitacoesRaw),
  };
}

function auditContratos(contratos, ano) {
  const issues = [];
  const byCnpj = new Map();
  const byNumero = new Map();

  for (const c of contratos) {
    const doc = classifyDocument(c.cnpjCredor);
    const key = `${ano}:${c.numero}`;

    if (byNumero.has(c.numero)) {
      issues.push({
        tipo: 'numero_duplicado',
        ano,
        numero: c.numero,
        ids: [byNumero.get(c.numero).id, c.id],
      });
    } else {
      byNumero.set(c.numero, c);
    }

    if (doc.kind === 'ausente') {
      issues.push({
        tipo: 'documento_ausente',
        ano,
        numero: c.numero,
        empresa: c.empresa,
        valor: c.valor,
      });
    } else if (doc.kind === 'invalido') {
      issues.push({
        tipo: 'documento_formato_invalido',
        ano,
        numero: c.numero,
        empresa: c.empresa,
        raw: c.cnpjCredor,
        digits: doc.digits,
      });
    } else if (!doc.valid) {
      issues.push({
        tipo: 'documento_digito_invalido',
        ano,
        numero: c.numero,
        empresa: c.empresa,
        kind: doc.kind,
        raw: c.cnpjCredor,
        formatted: doc.formatted,
      });
    }

    if (doc.digits) {
      const prev = byCnpj.get(doc.digits);
      const nomeNorm = normalizeName(c.empresa);
      if (prev && prev.nomeNorm !== nomeNorm) {
        issues.push({
          tipo: 'mesmo_documento_nomes_diferentes',
          ano,
          documento: doc.formatted || doc.digits,
          nomes: [...new Set([prev.empresa, c.empresa])],
          contratos: [prev.numero, c.numero],
        });
      } else if (!prev) {
        byCnpj.set(doc.digits, { empresa: c.empresa, nomeNorm, numero: c.numero });
      }
    }

    if (c.vigenciaFim && c.data && c.vigenciaFim < c.data) {
      issues.push({
        tipo: 'vigencia_inconsistente',
        ano,
        numero: c.numero,
        inicio: c.data,
        fim: c.vigenciaFim,
      });
    }

    if (c.valorNumerico <= 0 && c.valor) {
      issues.push({
        tipo: 'valor_zero_ou_invalido',
        ano,
        numero: c.numero,
        valor: c.valor,
      });
    }
  }

  const credores = [...byCnpj.entries()]
    .map(([digits, info]) => ({ digits, ...info }))
    .sort((a, b) => a.empresa.localeCompare(b.empresa, 'pt-BR'));

  const topCredores = aggregateByCredor(contratos);

  return { issues, credores: topCredores, total: contratos.length };
}

function aggregateByCredor(contratos) {
  const map = new Map();
  for (const c of contratos) {
    const doc = classifyDocument(c.cnpjCredor);
    const key = doc.digits || normalizeName(c.empresa);
    const prev = map.get(key) || {
      empresa: c.empresa,
      documento: doc.formatted || c.cnpjCredor || '(sem doc)',
      kind: doc.kind,
      valid: doc.valid,
      qtd: 0,
      total: 0,
      secretarias: new Set(),
    };
    prev.qtd += 1;
    prev.total += c.valorNumerico || 0;
    if (c.secretaria) prev.secretarias.add(c.secretaria);
    map.set(key, prev);
  }
  return [...map.values()]
    .map((v) => ({ ...v, secretarias: [...v.secretarias] }))
    .sort((a, b) => b.total - a.total);
}

function auditLicitacoes(licitacoes, ano) {
  const issues = [];
  const now = new Date();

  for (const l of licitacoes) {
    if (!l.objeto || l.objeto.length < 10) {
      issues.push({ tipo: 'licitacao_objeto_curto', ano, numero: l.numero, objeto: l.objeto });
    }
    if (l.situacao && /aberta|em andamento/i.test(l.situacao) && l.dataAbertura) {
      const [d, m, y] = l.dataAbertura.split('/').map(Number);
      if (d && m && y) {
        const abertura = new Date(y, m - 1, d);
        const dias = Math.floor((now - abertura) / 86_400_000);
        if (dias > 365) {
          issues.push({
            tipo: 'licitacao_aberta_antiga',
            ano,
            numero: l.numero,
            situacao: l.situacao,
            dataAbertura: l.dataAbertura,
            diasAberta: dias,
          });
        }
      }
    }
  }
  return issues;
}

function printReport(results) {
  console.log('\n=== AUDITORIA DE DADOS — CANINDÉ/CE ===\n');

  let totalContratos = 0;
  let totalIssues = [];

  for (const r of results) {
    totalContratos += r.contratos.total;
    totalIssues = totalIssues.concat(r.contratos.issues, r.licitacoesIssues);
    console.log(`--- Exercício ${r.ano} ---`);
    console.log(`Contratos: ${r.contratos.total} | Licitações: ${r.licitacoes.length}`);
    console.log(`Inconsistências contratos: ${r.contratos.issues.length}`);
    console.log(`Inconsistências licitações: ${r.licitacoesIssues.length}`);

    const byTipo = groupBy(r.contratos.issues, 'tipo');
    for (const [tipo, items] of Object.entries(byTipo)) {
      console.log(`  • ${tipo}: ${items.length}`);
    }
    console.log('');
  }

  console.log('--- Top 10 credores por valor (todos os anos) ---');
  const allContratos = results.flatMap((r) => r.rawContratos);
  const top = aggregateByCredor(allContratos).slice(0, 10);
  for (const c of top) {
    const flag = c.valid === false && c.kind !== 'ausente' ? ' ⚠ dígito inválido' : '';
    console.log(
      `  ${c.empresa.substring(0, 45).padEnd(45)} | ${c.documento} | ${c.qtd} contrato(s) | R$ ${c.total.toLocaleString('pt-BR')}${flag}`,
    );
  }

  console.log('\n--- Amostra de inconsistências (até 15) ---');
  const sample = totalIssues.slice(0, 15);
  if (sample.length === 0) {
    console.log('  Nenhuma inconsistência detectada nas regras aplicadas.');
  } else {
    for (const item of sample) {
      console.log(`  [${item.tipo}]`, JSON.stringify(item));
    }
  }

  console.log('\n--- Resumo ---');
  console.log(`Total contratos analisados: ${totalContratos}`);
  console.log(`Total alertas: ${totalIssues.length}`);
  const invalidDigits = totalIssues.filter((i) => i.tipo === 'documento_digito_invalido');
  const missingDoc = totalIssues.filter((i) => i.tipo === 'documento_ausente');
  const nameMismatch = totalIssues.filter((i) => i.tipo === 'mesmo_documento_nomes_diferentes');
  console.log(`  CNPJ/CPF com dígito inválido: ${invalidDigits.length}`);
  console.log(`  Sem documento: ${missingDoc.length}`);
  console.log(`  Mesmo doc, nomes diferentes: ${nameMismatch.length}`);
  console.log('');
}

function groupBy(items, key) {
  return items.reduce((acc, item) => {
    const k = item[key];
    acc[k] = acc[k] || [];
    acc[k].push(item);
    return acc;
  }, {});
}

async function main() {
  const years = process.argv.slice(2).map(Number).filter(Boolean);
  const anos = years.length ? years : [2024, 2025, 2026];

  const results = [];
  for (const ano of anos) {
    process.stderr.write(`Carregando ${ano}...\n`);
    const data = await loadYear(ano);
    results.push({
      ano,
      rawContratos: data.contratos,
      licitacoes: data.licitacoes,
      contratos: auditContratos(data.contratos, ano),
      licitacoesIssues: auditLicitacoes(data.licitacoes, ano),
    });
  }
  printReport(results);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
