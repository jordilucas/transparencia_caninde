package br.gov.caninde.transparencia.domain

/**
 * Fontes oficiais de onde o servidor captura dados públicos (espelha os scrapers em server/lib/).
 */
data class FonteCaptura(
    val titulo: String,
    val url: String,
    val descricao: String = "",
)

data class ReferenciaLegal(
    val titulo: String,
    val descricao: String,
    val url: String = "",
)

object DataSourcesInfo {
    const val SITE_URL = "https://transparenciacaninde.com.br"
    const val MUNICIPIO = "Canindé, CE"
    const val IBGE = "2302800"
    const val PREFEITURA_SITE = "https://www.caninde.ce.gov.br"
    const val CAMARA_SITE = "https://www.cmcaninde.ce.gov.br"

    val sobreDestaques: List<String> = listOf(
        "Finanças — receita arrecadada, despesa paga e maiores fornecedores (Governo Transparente)",
        "Folha de pagamento — totais por secretaria e por mês, sem nomes (LGPD)",
        "Contratos, licitações, obras, LRF e diário oficial da Prefeitura",
        "Vereadores, sessões, matérias e transparência da Câmara",
        "Água — registro anônimo de falta de abastecimento com painel público",
        "Busca unificada, gráficos, PWA (instalar como app) e seletor de exercício",
    )

    val portaisOficiais: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Prefeitura Municipal de Canindé",
            url = PREFEITURA_SITE,
            descricao = "Portal oficial do Executivo municipal — fonte primária de contratos, licitações, secretarias e publicações.",
        ),
        FonteCaptura(
            titulo = "Câmara Municipal de Canindé",
            url = CAMARA_SITE,
            descricao = "Portal oficial do Legislativo municipal — vereadores, sessões, matérias e transparência legislativa.",
        ),
        FonteCaptura(
            titulo = "Portal de transparência — Prefeitura",
            url = "$PREFEITURA_PORTAL_BASE/acessoainformacao.php",
            descricao = "Seção de acesso à informação e dados abertos da Prefeitura.",
        ),
        FonteCaptura(
            titulo = "Canindé Transparente — Câmara",
            url = "$CAMARA_PORTAL_BASE/caninde-transparente/",
            descricao = "Portal de transparência legislativa da Câmara.",
        ),
    )

    val comoFuncionaPassos: List<String> = listOf(
        "Um servidor consulta, em intervalos regulares, apenas URLs públicas da Prefeitura e da Câmara — " +
            "as mesmas páginas que qualquer cidadão pode abrir no navegador.",
        "Contratos, licitações, vereadores, sessões e demais listagens são normalizados em JSON e enviados " +
            "ao aplicativo via WebSocket (conexão em tempo real).",
        "Ao abrir um item (contrato, vereador, matéria etc.), o servidor busca o detalhe na página oficial " +
            "correspondente e exibe o conteúdo com link para o documento de origem.",
        "O app guarda a última sincronização no cache local do dispositivo, permitindo consulta mesmo " +
            "com conexão instável — sempre com indicação de quando os dados foram atualizados.",
        "Receitas, despesas e fornecedores da Prefeitura vêm do Governo Transparente (dados oficiais): " +
            "o app exibe totais do exercício, período e data de atualização; consultas detalhadas abrem o painel oficial.",
        "Folha de pagamento exibe apenas totais agregados por secretaria — sem nomes ou matrículas de servidores.",
        "A aba Água funciona de forma distinta: reclamações de falta de água são registradas pelos próprios " +
            "cidadãos (de forma anônima), com comprovantes em armazenamento seguro — não vêm dos portais oficiais.",
    )

    const val conformidadeLegalResumo =
        "Este portal não infringe a legislação brasileira. Exibe somente informações já tornadas públicas " +
            "pelos órgãos municipais, sem acesso a áreas restritas, sistemas internos ou dados sigilosos. " +
            "Não alteramos o conteúdo oficial: facilitamos a consulta e indicamos sempre a fonte de origem. " +
            "A republicação de dados públicos para fins de transparência e controle social encontra amparo na " +
            "Constituição Federal, na Lei de Acesso à Informação (LAI) e nas normas de transparência fiscal. " +
            "Trata-se de um projeto independente de facilitação — não substitui os sites oficiais da Prefeitura e da Câmara."

    val prefeituraJson: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Contratos (JSON)",
            url = "$PREFEITURA_PORTAL_BASE/dadosabertosexportar.php?d=contratos&f=json",
            descricao = "Exportação oficial de contratos por exercício.",
        ),
        FonteCaptura(
            titulo = "Licitações (JSON)",
            url = "$PREFEITURA_PORTAL_BASE/dadosabertosexportar.php?d=licitacoes&f=json",
            descricao = "Exportação oficial de licitações por exercício.",
        ),
        FonteCaptura(
            titulo = "Secretarias (JSON)",
            url = "$PREFEITURA_PORTAL_BASE/dadosabertosexportar.php?d=secretarias&f=json",
            descricao = "Cadastro de secretarias municipais.",
        ),
        FonteCaptura(
            titulo = "Publicações (JSON)",
            url = "$PREFEITURA_PORTAL_BASE/dadosabertosexportar.php?d=publicacoes&f=json",
            descricao = "Planos, relatórios e publicações oficiais.",
        ),
    )

    val prefeituraHtml: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Acesso à Informação",
            url = "$PREFEITURA_PORTAL_BASE/acessoainformacao.php",
            descricao = "Portal principal de transparência da Prefeitura.",
        ),
        FonteCaptura(
            titulo = "Contratos (HTML)",
            url = "$PREFEITURA_PORTAL_BASE/contratos.php",
            descricao = "Listagem complementar de contratos.",
        ),
        FonteCaptura(
            titulo = "Licitações (HTML)",
            url = "$PREFEITURA_PORTAL_BASE/licitacao.php",
            descricao = "Listagem complementar de licitações.",
        ),
        FonteCaptura(
            titulo = "Diário Oficial",
            url = "$PREFEITURA_PORTAL_BASE/diariolista.php",
            descricao = "Edições do diário oficial municipal.",
        ),
        FonteCaptura(
            titulo = "Gestores",
            url = "$PREFEITURA_PORTAL_BASE/gestores.php",
            descricao = "Prefeito, vice e equipe de gestão.",
        ),
        FonteCaptura(
            titulo = "Dados abertos (portal)",
            url = "$PREFEITURA_PORTAL_BASE/dadosabertos.php",
            descricao = "Página pública de dados abertos da Prefeitura.",
        ),
    )

    val prefeituraDetalhe: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Detalhe de secretaria",
            url = "$PREFEITURA_PORTAL_BASE/secretaria.php?sec={id}",
            descricao = "Carregado ao abrir uma secretaria.",
        ),
        FonteCaptura(
            titulo = "Detalhe de contrato",
            url = "$PREFEITURA_PORTAL_BASE/contratos.php?id={id}",
            descricao = "Carregado ao abrir um contrato.",
        ),
        FonteCaptura(
            titulo = "Detalhe de licitação",
            url = "$PREFEITURA_PORTAL_BASE/licitacaolista.php?id={id}",
            descricao = "Carregado ao abrir uma licitação.",
        ),
        FonteCaptura(
            titulo = "Detalhe de publicação",
            url = "$PREFEITURA_PORTAL_BASE/publicacoes.php?id={id}",
            descricao = "Carregado ao abrir uma publicação.",
        ),
    )

    val camaraWp: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Vereadores (REST)",
            url = "$CAMARA_PORTAL_BASE/wp-json/wp/v2/vereadores",
            descricao = "API pública WordPress da Câmara.",
        ),
        FonteCaptura(
            titulo = "Sessões (REST)",
            url = "$CAMARA_PORTAL_BASE/wp-json/wp/v2/sessao",
            descricao = "Sessões legislativas publicadas.",
        ),
        FonteCaptura(
            titulo = "Matérias (REST)",
            url = "$CAMARA_PORTAL_BASE/wp-json/wp/v2/materia",
            descricao = "Projetos, requerimentos e demais matérias.",
        ),
    )

    val camaraHtml: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Parlamentares",
            url = "$CAMARA_PORTAL_BASE/parlamentares/",
            descricao = "Listagem HTML de vereadores.",
        ),
        FonteCaptura(
            titulo = "Sessões",
            url = "$CAMARA_PORTAL_BASE/sessoes/",
            descricao = "Listagem HTML de sessões.",
        ),
        FonteCaptura(
            titulo = "Matérias",
            url = "$CAMARA_PORTAL_BASE/materias/",
            descricao = "Listagem HTML de matérias legislativas.",
        ),
        FonteCaptura(
            titulo = "Canindé Transparente",
            url = "$CAMARA_PORTAL_BASE/caninde-transparente/",
            descricao = "Portal de transparência legislativa.",
        ),
    )

    val camaraDetalhe: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Perfil do vereador",
            url = "$CAMARA_PORTAL_BASE/vereadores/{slug}/",
            descricao = "Biografia e produção legislativa ao abrir um vereador.",
        ),
        FonteCaptura(
            titulo = "Detalhe de matéria",
            url = "$CAMARA_PORTAL_BASE/materia/{slug}/",
            descricao = "Texto e anexos ao abrir uma matéria.",
        ),
        FonteCaptura(
            titulo = "Detalhe de sessão",
            url = "$CAMARA_PORTAL_BASE/sessao/{slug}/",
            descricao = "Ata e documentos ao abrir uma sessão.",
        ),
    )

    val governoTransparente: List<FonteCaptura> = listOf(
        FonteCaptura(
            titulo = "Receitas — Prefeitura (ID 11979490)",
            url = "https://www.governotransparente.com.br/transparencia/receitas/11979490",
            descricao = "Painel financeiro oficial da Prefeitura de Canindé.",
        ),
        FonteCaptura(
            titulo = "Despesas — Prefeitura",
            url = "https://www.governotransparente.com.br/transparencia/despesas/opcoes/11979490",
            descricao = "Consulta detalhada de despesas municipais.",
        ),
        FonteCaptura(
            titulo = "Dados abertos — Governo Transparente (Prefeitura)",
            url = "https://www.governotransparente.com.br/dadosabertos/11979490?clean=false",
            descricao = "Exportação em massa de receitas, despesas, fornecedores e convênios.",
        ),
        FonteCaptura(
            titulo = "Receitas — Câmara (ID 11979588)",
            url = "https://www.governotransparente.com.br/transparencia/receitas/11979588",
            descricao = "Painel financeiro oficial da Câmara Municipal.",
        ),
        FonteCaptura(
            titulo = "Despesas — Câmara",
            url = "https://www.governotransparente.com.br/transparencia/despesas/opcoes/11979588",
            descricao = "Consulta detalhada de despesas da Câmara.",
        ),
    )

    val baseLegal: List<ReferenciaLegal> = listOf(
        ReferenciaLegal(
            titulo = "Constituição Federal, art. 5º, XXXIII",
            descricao = "Garante a todos o direito de receber dos órgãos públicos informações de interesse particular ou coletivo.",
            url = "https://www.planalto.gov.br/ccivil_03/constituicao/constituicao.htm",
        ),
        ReferenciaLegal(
            titulo = "Lei nº 12.527/2011 — Lei de Acesso à Informação (LAI)",
            descricao = "Assegura o acesso a informações públicas e reforça a transparência da administração pública.",
            url = "https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2011/lei/l12527.htm",
        ),
        ReferenciaLegal(
            titulo = "Lei Complementar nº 131/2009",
            descricao = "Exige divulgação em tempo real de receitas, despesas e demais dados fiscais por municípios, estados e União.",
            url = "https://www.planalto.gov.br/ccivil_03/leis/lcp/lcp131.htm",
        ),
        ReferenciaLegal(
            titulo = "Lei nº 14.129/2021 — Governo Digital",
            descricao = "Estimula a transparência pública, o uso de dados abertos e a melhoria dos serviços digitais.",
            url = "https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2021/lei/L14129.htm",
        ),
        ReferenciaLegal(
            titulo = "Lei nº 13.709/2018 — LGPD (dados públicos)",
            descricao = "Dados institucionais de agentes públicos e informações divulgadas para transparência " +
                "são tratados conforme o interesse público; contatos pessoais de vereadores não são replicados neste app.",
            url = "https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm",
        ),
        ReferenciaLegal(
            titulo = "Marco Civil da Internet (Lei nº 12.965/2014)",
            descricao = "Garantias de liberdade de expressão e acesso à informação na internet, em consonância " +
                "com a divulgação de dados já publicados por órgãos públicos.",
            url = "https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm",
        ),
    )

    val changelog: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            titulo = "Correção de valores",
            data = "17/08/2026",
            itens = listOf(
                "Total de contratos corrigido — valores do portal não eram mais inflados",
                "Card de contratos indica exercício e quantidade publicada no portal municipal",
            ),
        ),
        ChangelogEntry(
            titulo = "Sobre e divulgação",
            data = "17/08/2026",
            itens = listOf(
                "Seção de destaques do portal na aba Sobre",
                "Texto atualizado sobre finanças integradas do Governo Transparente",
                "Materiais para compartilhar novidades no Instagram",
            ),
        ),
        ChangelogEntry(
            titulo = "Finanças em destaque",
            data = "17/08/2026",
            itens = listOf(
                "Nova aba Finanças na Prefeitura com receitas e despesas do Governo Transparente",
                "Card de execução orçamentária sempre visível no topo",
                "Período acumulado e data de atualização do Governo Transparente",
                "Atalhos para receita arrecadada, despesas pagas e consultas detalhadas",
            ),
        ),
        ChangelogEntry(
            titulo = "Governo Transparente",
            data = "17/08/2026",
            itens = listOf(
                "Receita arrecadada e despesa paga do exercício no resumo financeiro",
                "Ranking dos maiores fornecedores (empresas, sem folha nominal)",
                "Link para exportar dados abertos do Governo Transparente",
            ),
        ),
        ChangelogEntry(
            titulo = "Folha de pagamento",
            data = "17/08/2026",
            itens = listOf(
                "Nova aba Folha com totais por secretaria e por mês",
                "Dados agregados do portal oficial — sem nomes de servidores (LGPD)",
                "Gráfico de participação por órgão e link para consulta nominal",
            ),
        ),
        ChangelogEntry(
            titulo = "Experiência e transparência",
            data = "17/08/2026",
            itens = listOf(
                "Card na Prefeitura para registrar falta de água",
                "Atualizado há X min/horas nas abas Prefeitura e Câmara",
                "Seletor de exercício para contratos e licitações anteriores",
                "Detalhe nativo de documentos LRF com link ao PDF",
                "Busca inclui links da Prefeitura e buscas recentes",
                "Exportar reclamações de água em CSV no painel",
                "Instalação como app (PWA) e aviso de cookies antes do analytics",
            ),
        ),
        ChangelogEntry(
            titulo = "Dados e aba Água",
            data = "17/08/2026",
            itens = listOf(
                "Diário Oficial com PDF direto na aba Publicações",
                "Detalhe de obras municipais (secretaria, valor, situação)",
                "Resumo financeiro: total em contratos e licitações abertas",
                "Autor nas matérias legislativas quando disponível",
                "Painel de falta de água com gráfico por setor e top endereços",
            ),
        ),
        ChangelogEntry(
            titulo = "Sobre — fontes e funcionamento",
            data = "17/08/2026",
            itens = listOf(
                "Passo a passo de como o portal captura e exibe dados públicos",
                "Fontes oficiais da Prefeitura e da Câmara em destaque, com links",
                "Seção de conformidade legal — uso permitido de dados já publicados",
                "Google Analytics reativado (IP anonimizado) para melhorar o serviço",
            ),
        ),
        ChangelogEntry(
            titulo = "Privacidade e vereadores",
            data = "17/08/2026",
            itens = listOf(
                "Contatos pessoais de vereadores não aparecem mais no app",
                "Política de privacidade atualizada na aba Sobre",
                "Botão compartilhar na aba Água para divulgar o registro de reclamações",
            ),
        ),
        ChangelogEntry(
            titulo = "Falta de água — reclamações",
            data = "15/08/2026",
            itens = listOf(
                "Nova aba Água para registrar falta de abastecimento em Canindé",
                "Formulário com endereço, setor (1 ou 2), dias sem água e foto ou vídeo",
                "Painel público com totais por setor e lista de reclamações",
                "Registro anônimo; comprovantes hospedados com armazenamento gratuito",
                "Rota web: transparenciacaninde.com.br#/agua",
            ),
        ),
        ChangelogEntry(
            titulo = "Web — CORS e fotos",
            data = "31/07/2026",
            itens = listOf(
                "Health check do servidor com CORS para transparenciacaninde.com.br",
                "Fotos dos vereadores na web via proxy seguro do backend",
            ),
        ),
        ChangelogEntry(
            titulo = "Correção servidor",
            data = "31/07/2026",
            itens = listOf(
                "Correção de falha no deploy do backend (Render)",
            ),
        ),
        ChangelogEntry(
            titulo = "Câmara — etapa 3",
            data = "31/07/2026",
            itens = listOf(
                "Busca avançada com filtros por tipo e Ver mais por seção",
                "Player de vídeo em sessões (YouTube no app Android)",
                "Detalhe nativo de licitações/contratos do portal Canindé Transparente",
                "Seção Finanças públicas com links do Governo Transparente",
                "Rotas /documento-camara e /video para links estáveis",
            ),
        ),
        ChangelogEntry(
            titulo = "Câmara — etapa 2",
            data = "31/07/2026",
            itens = listOf(
                "Até 50 sessões e matérias (antes 25)",
                "Documentos de licitações e contratos do portal Canindé Transparente",
                "Ver mais nas listas de sessões e matérias",
                "Gráfico de matérias por tipo na aba Legislativo",
                "Busca inclui mesa diretora, links e documentos da Câmara",
                "Sessões abrem por slug estável (não só por posição na lista)",
            ),
        ),
        ChangelogEntry(
            titulo = "Web mobile",
            data = "31/07/2026",
            itens = listOf(
                "Correção de carregamento em celular (Safari/iOS)",
                "Mensagem clara se o navegador não for suportado",
                "Botão recarregar quando a primeira carga demora",
            ),
        ),
        ChangelogEntry(
            titulo = "Câmara — melhorias",
            data = "30/07/2026",
            itens = listOf(
                "Fotos dos vereadores na lista e no perfil",
                "Mesa diretora abre o perfil do vereador",
                "Filtro por tipo de matéria legislativa",
                "Botão Assistir sessão quando há vídeo",
                "Links corrigidos de licitações e contratos da Câmara",
            ),
        ),
        ChangelogEntry(
            titulo = "Navegação e links",
            data = "30/07/2026",
            itens = listOf(
                "Voltar do navegador sincronizado com o app",
                "Links compartilháveis abrem direto no item (#/contrato/…)",
                "Compartilhar inclui URL do detalhe, não só a home",
            ),
        ),
        ChangelogEntry(
            titulo = "Dados e estabilidade",
            data = "30/07/2026",
            itens = listOf(
                "Cache local na web (dados após primeiro acesso)",
                "Obras e documentos LRF na Prefeitura",
                "Correção de telas de detalhe (contratos, licitações, sessões)",
                "Tela de erro quando sem conexão com o servidor",
                "Reconexão automática após cold start do servidor",
                "Coleta responsável: só portais oficiais, cooldown e limites anti-sobrecarga",
            ),
        ),
        ChangelogEntry(
            titulo = "Busca e compartilhar",
            data = "30/07/2026",
            itens = listOf(
                "Busca com ignorar acentos e filtros Prefeitura/Câmara",
                "Filtros vigentes (contratos) e abertas (licitações)",
                "Botão compartilhar nos detalhes",
                "Tela de carregamento na web",
                "Changelog na página Sobre",
            ),
        ),
        ChangelogEntry(
            titulo = "Julho 2026",
            data = "29/07/2026",
            itens = listOf(
                "Página Sobre com fontes oficiais e base legal",
                "Detalhes inline: publicações, transparência e páginas externas",
                "Detalhes enriquecidos de contrato, licitação e sessão",
            ),
        ),
        ChangelogEntry(
            titulo = "Julho 2026",
            data = "27/07/2026",
            itens = listOf(
                "Site publicado em transparenciacaninde.com.br",
                "App web Kotlin/Wasm + dados em tempo real",
            ),
        ),
    )
}

data class ChangelogEntry(
    val titulo: String,
    val data: String,
    val itens: List<String>,
)

fun FonteCaptura.urlParaAbrir(): String {
    if (!url.contains('{')) return url
    return when {
        "secretaria.php" in url -> "$PREFEITURA_PORTAL_BASE/acessoainformacao.php"
        "contratos.php" in url -> "$PREFEITURA_PORTAL_BASE/contratos.php"
        "licitacaolista.php" in url -> "$PREFEITURA_PORTAL_BASE/licitacao.php"
        "publicacoes.php" in url -> "$PREFEITURA_PORTAL_BASE/publicacoes.php"
        "vereadores/" in url -> "$CAMARA_PORTAL_BASE/parlamentares/"
        "/materia/" in url -> "$CAMARA_PORTAL_BASE/materias/"
        "/sessao/" in url -> "$CAMARA_PORTAL_BASE/sessoes/"
        else -> url.substringBefore('{').trimEnd('?', '/')
    }
}
