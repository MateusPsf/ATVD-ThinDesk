(() => {
    'use strict';
    const root = document.getElementById('painel');
    if (!root) return;
    const $ = id => document.getElementById(id);
    const form = $('filtros-form');
    const numero = new Intl.NumberFormat('pt-BR');
    const horario = new Intl.DateTimeFormat('pt-BR', {dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo'});
    const cores = ['#5586bd', '#d7a24c', '#179b86', '#9aabb8', '#d4dce2'];
    let aplicados = new URLSearchParams();
    let pagina = 0;
    let paginaExibida = 0;
    let paginas = 0;
    let consulta = 0;
    let controle;
    let possuiDados = false;

    function elemento(tag, classe, texto) {
        const node = document.createElement(tag);
        if (classe) node.className = classe;
        if (texto !== undefined) node.textContent = texto;
        return node;
    }

    function mensagem(texto) {
        $('aviso').textContent = texto;
        $('aviso').hidden = !texto;
    }

    function data(valor) {
        return valor ? horario.format(new Date(valor)) : 'Não informada';
    }

    function opcoes(filtros) {
        for (const [nome, chave] of [['status', 'status'], ['tipo', 'tipos'], ['tecnico', 'tecnicos']]) {
            const select = form.elements.namedItem(nome);
            const anterior = select.value;
            const padrao = select.options[0].cloneNode(true);
            select.replaceChildren(padrao);
            for (const opcao of filtros[chave]) {
                const option = elemento('option', '', opcao.rotulo);
                option.value = opcao.valor;
                select.append(option);
            }
            if (anterior && !filtros[chave].some(opcao => opcao.valor === anterior)) {
                const option = elemento('option', '', anterior);
                option.value = anterior;
                select.append(option);
            }
            select.value = anterior;
            select.disabled = false;
        }
    }

    function resumo(dados) {
        for (const nome of ['total', 'abertos', 'emAndamento', 'concluidos', 'cancelados']) {
            $(nome).textContent = numero.format(dados[nome]);
        }
        const qualidade = dados.qualidade;
        $('qualidade').hidden = !(qualidade.semDataAbertura || qualidade.concluidosSemDataConclusao);
        $('qualidade').textContent = `Qualidade dos dados: ${numero.format(qualidade.semDataAbertura)} chamado(s) sem abertura e ${numero.format(qualidade.concluidosSemDataConclusao)} concluído(s) sem data de conclusão nesta consulta. Datas históricas não são estimadas.`;
        const grafico = $('grafico-situacoes');
        grafico.replaceChildren();
        const donut = elemento('div', 'donut');
        let acumulado = 0;
        const segmentos = dados.situacoes.map((situacao, index) => {
            const inicio = acumulado;
            acumulado += dados.total ? situacao.quantidade * 100 / dados.total : 0;
            return `${cores[index]} ${inicio}% ${acumulado}%`;
        });
        donut.style.background = dados.total ? `conic-gradient(${segmentos.join(',')})` : '#edf1f4';
        donut.setAttribute('aria-hidden', 'true');
        const centro = elemento('div', 'donut-center');
        centro.append(elemento('strong', '', numero.format(dados.total)), elemento('span', '', dados.total ? 'chamados' : 'sem chamados'));
        donut.append(centro);
        const legenda = elemento('div', 'legend');
        for (const [index, situacao] of dados.situacoes.entries()) {
            const linha = elemento('div', 'legend-row');
            const ponto = elemento('i', 'dot');
            ponto.style.background = cores[index];
            ponto.setAttribute('aria-hidden', 'true');
            linha.append(ponto, elemento('span', '', situacao.status), elemento('strong', '', numero.format(situacao.quantidade)),
                elemento('small', '', `${numero.format(situacao.percentual)}%`));
            legenda.append(linha);
        }
        grafico.append(donut, legenda);
    }

    function periodo(valor) {
        const partes = valor.split('-');
        return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : `${partes[1]}/${partes[0]}`;
    }

    function svgNode(tag, atributos, texto) {
        const node = document.createElementNS('http://www.w3.org/2000/svg', tag);
        for (const [nome, valor] of Object.entries(atributos)) node.setAttribute(nome, String(valor));
        if (texto !== undefined) node.textContent = texto;
        return node;
    }

    function atendimentos(dados) {
        const container = $('grafico-periodo');
        container.replaceChildren();
        container.classList.remove('chart-placeholder');
        $('agrupamento-ativo').textContent = dados.agrupamento === 'dia' ? 'Por dia' : 'Por mês';
        const tabela = $('dados-periodo');
        tabela.replaceChildren();
        for (const ponto of dados.serie) {
            const tr = elemento('tr');
            tr.append(elemento('td', '', periodo(ponto.periodo)), elemento('td', '', numero.format(ponto.quantidade)));
            tabela.append(tr);
        }
        if (!dados.serie.length) {
            container.append(elemento('p', 'empty', 'Nenhuma conclusão com data para esta consulta.'));
            const tr = elemento('tr');
            const td = elemento('td', 'empty', 'Sem dados no período.');
            td.colSpan = 2;
            tr.append(td);
            tabela.append(tr);
            return;
        }
        const maximo = Math.max(1, ...dados.serie.map(ponto => ponto.quantidade));
        const svg = svgNode('svg', {viewBox: '0 0 760 250', role: 'img', 'aria-labelledby': 'grafico-titulo grafico-descricao'});
        svg.append(svgNode('title', {id: 'grafico-titulo'}, 'Evolução dos chamados concluídos'),
            svgNode('desc', {id: 'grafico-descricao'}, 'Quantidade de conclusões por período. Os valores estão disponíveis em Ver dados do gráfico.'));
        const x = index => dados.serie.length === 1 ? 400 : 50 + index * 680 / (dados.serie.length - 1);
        const y = valor => 200 - valor * 165 / maximo;
        const marcas = [...new Set([0, Math.ceil(maximo / 2), maximo])];
        for (const valor of marcas) {
            svg.append(svgNode('line', {x1: 50, x2: 730, y1: y(valor), y2: y(valor), stroke: '#e9eef2', 'stroke-dasharray': '3 4'}),
                svgNode('text', {x: 38, y: y(valor) + 4, 'text-anchor': 'end'}, numero.format(valor)));
        }
        const pontos = dados.serie.map((ponto, index) => `${x(index)},${y(ponto.quantidade)}`).join(' ');
        svg.append(svgNode('polygon', {points: `${x(0)},200 ${pontos} ${x(dados.serie.length - 1)},200`, fill: '#e7f5f0'}),
            svgNode('polyline', {points: pontos, fill: 'none', stroke: '#139b83', 'stroke-width': 2.5, 'stroke-linejoin': 'round'}));
        const passo = Math.max(1, Math.ceil((dados.serie.length - 1) / 5));
        dados.serie.forEach((ponto, index) => {
            if (dados.serie.length <= 90) {
                const circle = svgNode('circle', {cx: x(index), cy: y(ponto.quantidade), r: dados.serie.length > 31 ? 2 : 3.5, fill: '#139b83', stroke: '#fff', 'stroke-width': 1});
                circle.append(svgNode('title', {}, `${periodo(ponto.periodo)}: ${numero.format(ponto.quantidade)} concluído(s)`));
                svg.append(circle);
            }
            if (index % passo === 0 || index === dados.serie.length - 1) {
                // Evita sobrepor a última legenda quando ela estiver muito perto da anterior.
                if (index !== dados.serie.length - 1 && dados.serie.length - 1 - index < passo / 2) return;
                svg.append(svgNode('text', {x: x(index), y: 226, 'text-anchor': 'middle'}, periodo(ponto.periodo)));
            }
        });
        container.append(svg);
        if (!dados.serie.some(ponto => ponto.quantidade > 0)) {
            container.append(elemento('p', 'no-results', 'Nenhuma conclusão no período selecionado.'));
        }
    }

    function ranking(dados) {
        const container = $('ranking');
        container.replaceChildren();
        if (!dados.itens.length) {
            container.append(elemento('p', 'empty', 'Nenhum atendimento concluído para compor o ranking.'));
            return;
        }
        const maximo = dados.itens[0].quantidade || 1;
        dados.itens.forEach((item, index) => {
            const linha = elemento('div', 'rank-row');
            const trilho = elemento('div', 'rank-track');
            trilho.setAttribute('aria-hidden', 'true');
            const barra = elemento('div', 'rank-fill');
            barra.style.width = `${item.quantidade / maximo * 100}%`;
            trilho.append(barra);
            linha.append(elemento('span', 'rank-number', String(index + 1).padStart(2, '0')), elemento('span', 'rank-name', item.tipo), trilho,
                elemento('span', 'rank-count', numero.format(item.quantidade)));
            container.append(linha);
        });
    }

    function chamados(dados) {
        const lista = $('lista-chamados');
        lista.replaceChildren();
        paginas = dados.totalPaginas;
        paginaExibida = dados.pagina;
        $('total-lista').textContent = `${numero.format(dados.totalElementos)} chamado(s)`;
        $('pagina-info').textContent = dados.totalPaginas ? `Página ${numero.format(dados.pagina + 1)} de ${numero.format(dados.totalPaginas)}` : 'Nenhum chamado encontrado';
        if (!dados.itens.length) {
            const tr = elemento('tr');
            const td = elemento('td', 'empty', dados.totalElementos ? 'Não há chamados nesta página. Volte à página anterior.' : 'Nenhum chamado corresponde aos filtros.');
            td.colSpan = 6;
            tr.append(td);
            lista.append(tr);
        }
        for (const item of dados.itens) {
            const tr = elemento('tr');
            const descricao = elemento('td');
            descricao.append(elemento('p', 'ticket-description', item.descricao || 'Sem descrição'), elemento('span', 'ticket-id', `# ${item.id}`));
            const detalhes = elemento('details', 'ticket-notes');
            detalhes.append(elemento('summary', '', 'Considerações técnicas'), elemento('p', '', item.consideracoesTecnicas || 'Nenhuma consideração técnica registrada.'));
            descricao.append(detalhes);
            const situacao = elemento('td');
            const estilos = {'Aberto': 'status-open', 'Em andamento': 'status-progress', 'Concluído': 'status-done', 'Cancelado': 'status-cancel'};
            situacao.append(elemento('span', `status-chip ${estilos[item.status] || ''}`, item.status));
            const pessoa = elemento('td', 'person', item.tecnico);
            pessoa.append(elemento('small', '', `Solicitante: ${item.usuario || 'Não informado'}`));
            tr.append(descricao, situacao, elemento('td', '', item.tipo), pessoa, elemento('td', '', data(item.dataAbertura)), elemento('td', '', data(item.dataConclusao)));
            lista.append(tr);
        }
    }

    function paginacao() {
        $('anterior').disabled = !possuiDados || paginaExibida <= 0;
        $('proxima').disabled = !possuiDados || paginaExibida + 1 >= paginas;
    }

    async function carregar() {
        const atual = ++consulta;
        controle?.abort();
        controle = new AbortController();
        const controller = controle;
        let expirou = false;
        const timer = setTimeout(() => { expirou = true; controller.abort(); }, 20000);
        root.classList.add('is-loading');
        root.setAttribute('aria-busy', 'true');
        $('erro').hidden = true;
        mensagem('Atualizando dados da operação…');
        $('anterior').disabled = true;
        $('proxima').disabled = true;
        const parametros = new URLSearchParams(aplicados);
        async function buscar(caminho, extras = {}) {
            const query = caminho === 'filtros' ? new URLSearchParams() : new URLSearchParams(parametros);
            for (const [nome, valor] of Object.entries(extras)) query.set(nome, String(valor));
            const response = await fetch(`${root.dataset.api}${caminho}?${query}`, {
                signal: controller.signal, credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'}
            });
            let body;
            try { body = await response.json(); }
            catch { throw new Error('O servidor não retornou os dados esperados. Tente novamente.'); }
            if (!response.ok) {
                const erro = new Error(body.mensagem || 'Não foi possível consultar os dados.');
                erro.sessao = response.status === 401;
                throw erro;
            }
            return body;
        }
        try {
            const [indicadores, serie, tipos, lista, filtros] = await Promise.all([
                buscar('resumo'), buscar('atendimentos'), buscar('tipos', {limite: 5}),
                buscar('chamados', {pagina, tamanho: 10}), buscar('filtros')
            ]);
            if (atual !== consulta) return;
            opcoes(filtros);
            resumo(indicadores);
            atendimentos(serie);
            ranking(tipos);
            chamados(lista);
            possuiDados = true;
            root.classList.remove('is-stale');
            $('ultima-atualizacao').textContent = `Atualizado em ${data(indicadores.atualizadoEm)}`;
            mensagem(indicadores.total ? '' : 'Nenhum chamado encontrado. Ajuste os filtros ou cadastre chamados no sistema.');
        } catch (erro) {
            if (atual !== consulta) return;
            controller.abort();
            mensagem('');
            root.classList.toggle('is-stale', possuiDados);
            const texto = expirou ? 'A consulta demorou mais que o esperado. Tente novamente.' :
                erro.sessao ? 'Sua sessão expirou. Entre novamente para continuar.' :
                erro instanceof TypeError ? 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.' : erro.message;
            $('erro-texto').textContent = texto + (possuiDados ? ' Os dados exibidos são da consulta anterior e estão desatualizados.' : '');
            $('erro').hidden = false;
            $('entrar').hidden = !erro.sessao;
            $('tentar').hidden = Boolean(erro.sessao);
            if (!possuiDados) {
                for (const id of ['grafico-periodo', 'grafico-situacoes', 'ranking']) {
                    $(id).replaceChildren(elemento('p', 'empty', 'Dados indisponíveis. Tente carregar novamente.'));
                }
                const tr = elemento('tr');
                const td = elemento('td', 'empty', 'Não foi possível carregar os chamados.');
                td.colSpan = 6;
                tr.append(td);
                $('lista-chamados').replaceChildren(tr);
            }
        } finally {
            clearTimeout(timer);
            if (atual === consulta) {
                root.classList.remove('is-loading');
                root.setAttribute('aria-busy', 'false');
                paginacao();
            }
        }
    }

    form.addEventListener('submit', event => {
        event.preventDefault();
        const inicio = form.elements.namedItem('dataInicio');
        const fim = form.elements.namedItem('dataFim');
        fim.setCustomValidity(inicio.value && fim.value && inicio.value > fim.value ? 'A data final deve ser posterior ou igual à data inicial.' : '');
        if (!form.reportValidity()) return;
        aplicados = new URLSearchParams();
        for (const [nome, valor] of new FormData(form)) if (valor) aplicados.set(nome, valor);
        pagina = 0;
        carregar();
    });
    for (const nome of ['dataInicio', 'dataFim']) form.elements.namedItem(nome).addEventListener('input', () => form.elements.namedItem('dataFim').setCustomValidity(''));
    $('limpar').addEventListener('click', () => {
        form.reset();
        form.elements.namedItem('dataFim').setCustomValidity('');
        aplicados = new URLSearchParams();
        pagina = 0;
        carregar();
    });
    $('atualizar').addEventListener('click', carregar);
    $('tentar').addEventListener('click', carregar);
    $('anterior').addEventListener('click', () => { pagina = Math.max(0, paginaExibida - 1); carregar(); });
    $('proxima').addEventListener('click', () => { pagina = paginaExibida + 1; carregar(); });
    carregar();
})();
