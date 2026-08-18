'use strict';

function countBy(items, keyFn) {
  const map = new Map();
  for (const item of items) {
    const k = keyFn(item) || 'Outros';
    map.set(k, (map.get(k) || 0) + 1);
  }
  return [...map.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([label, valor]) => ({ label, valor }));
}

function contratosPorMes(contratos) {
  const map = new Map();
  for (const c of contratos) {
    const m = (c.data || '').match(/\d{2}\/(\d{2})\/(\d{4})/);
    const label = m ? `${m[2]}-${m[1]}` : 'Sem data';
    map.set(label, (map.get(label) || 0) + 1);
  }
  const entries = [...map.entries()].filter(([k]) => k !== 'Sem data').sort((a, b) => a[0].localeCompare(b[0]));
  if (entries.length === 0) return [];
  return entries.map(([label, valor]) => ({ label, valor }));
}

function sessoesPorMes(sessoes) {
  const map = new Map();
  for (const s of sessoes) {
    const m = (s.data || '').match(/\d{2}\/(\d{2})\/(\d{4})/);
    const label = m ? `${m[2]}-${m[1]}` : '';
    if (!label) continue;
    map.set(label, (map.get(label) || 0) + 1);
  }
  const entries = [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  if (entries.length === 0) return [];
  return entries.slice(-12).map(([label, valor]) => ({ label, valor }));
}

function buildPrefeituraCharts(data) {
  const series = [];
  const lic = countBy(data.licitacoes || [], (l) => l.situacao);
  if (lic.length) series.push({ titulo: 'Licitações por situação', labels: lic.map((x) => x.label), valores: lic.map((x) => x.valor) });
  const mes = contratosPorMes(data.contratos || []);
  if (mes.length) series.push({ titulo: 'Contratos por período', labels: mes.map((x) => x.label), valores: mes.map((x) => x.valor) });
  if ((data.secretarias || []).length) {
    series.push({
      titulo: 'Secretarias cadastradas',
      labels: ['Total'],
      valores: [data.secretarias.length],
    });
  }
  const rubricas = data.resumoFinanceiro?.receitasPorRubrica || [];
  if (rubricas.length) {
    series.push({
      titulo: 'Receitas por rubrica (top)',
      labels: rubricas.map((r) => r.nome),
      valores: rubricas.map((r) => Math.round(Number(r.valorNumerico ?? 0) / 1000) || 0),
    });
  }
  return { prefeitura: series, camara: [] };
}

function buildCamaraCharts(data) {
  const series = [];
  const mat = countBy(data.materias || [], (m) => m.tipo);
  if (mat.length) {
    series.push({
      titulo: 'Matérias por tipo',
      labels: mat.map((x) => x.label),
      valores: mat.map((x) => x.valor),
    });
  }
  const partidos = countBy(
    (data.parlamentares || []).filter((p) => (p.partido || '').trim()),
    (p) => p.partido.trim(),
  );
  if (partidos.length) {
    series.push({
      titulo: 'Vereadores por partido',
      labels: partidos.map((x) => x.label),
      valores: partidos.map((x) => x.valor),
    });
  }
  const sessMes = sessoesPorMes(data.sessoes || []);
  if (sessMes.length) {
    series.push({
      titulo: 'Sessões por mês',
      labels: sessMes.map((x) => x.label),
      valores: sessMes.map((x) => x.valor),
    });
  }
  return { prefeitura: [], camara: series };
}

function attachCharts(prefeitura, camara) {
  const p = buildPrefeituraCharts(prefeitura || {});
  const c = buildCamaraCharts(camara || {});
  if (prefeitura) prefeitura.graficos = { prefeitura: p.prefeitura, camara: [] };
  if (camara) camara.graficos = { prefeitura: [], camara: c.camara };
}

module.exports = {
  buildPrefeituraCharts,
  buildCamaraCharts,
  attachCharts,
  countBy,
  sessoesPorMes,
};
