const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const base = path.resolve(__dirname, '../..', 'main/resources');
const fonte = fs.readFileSync(path.join(base, 'static/js/dashboard.js'), 'utf8');
const html = fs.readFileSync(path.join(base, 'templates/dashboard.html'), 'utf8');

// DOM mínimo para verificar os fluxos de consulta, sem navegador ou dependências extras.
class Elemento {
    constructor(tag = 'div') {
        this.tag = tag; this.children = []; this.listeners = {}; this.attrs = {};
        this.style = {}; this.hidden = false; this.disabled = false; this.value = ''; this.texto = '';
        const classes = new Set();
        this.classList = {
            add: (...nomes) => nomes.forEach(n => classes.add(n)),
            remove: (...nomes) => nomes.forEach(n => classes.delete(n)),
            contains: n => classes.has(n),
            toggle: (n, ativo) => ativo ? classes.add(n) : classes.delete(n)
        };
    }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this.children = nodes; this.texto = ''; }
    set textContent(valor) { this.texto = String(valor); this.children = []; }
    get textContent() { return this.texto + this.children.map(n => n.textContent).join(' '); }
    get options() { return this.children; }
    cloneNode() { return new Elemento(this.tag); }
    setAttribute(nome, valor) { this.attrs[nome] = valor; }
    addEventListener(nome, callback) { this.listeners[nome] = callback; }
    setCustomValidity(texto) { this.validacao = texto; }
    dispatch(nome) { this.listeners[nome]?.({preventDefault() {}}); }
}

const fixtures = total => ({
    resumo: {total, abertos: total, emAndamento: 0, concluidos: 0, cancelados: 0,
        qualidade: {semDataAbertura: 0, concluidosSemDataConclusao: 0}, atualizadoEm: '2026-11-14T03:00:00Z',
        situacoes: ['Aberto', 'Em andamento', 'Concluído', 'Cancelado', 'Não informado'].map((status, i) => ({status, quantidade: i ? 0 : total, percentual: i || !total ? 0 : 100}))},
    atendimentos: {agrupamento: 'dia', serie: [{periodo: '2026-11-14', quantidade: 0}]},
    tipos: {itens: []}, chamados: {itens: [], pagina: 0, totalElementos: total, totalPaginas: total ? 2 : 0},
    filtros: {status: [{valor: 'Aberto', rotulo: 'Aberto'}], tipos: [], tecnicos: []}
});

function ambiente(responder) {
    const nodes = new Map([...html.matchAll(/id="([^"]+)"/g)].map(match => [match[1], new Elemento()]));
    const campos = Object.fromEntries(['dataInicio', 'dataFim', 'status', 'tipo', 'tecnico', 'agrupamento'].map(nome => [nome, new Elemento()]));
    for (const nome of ['status', 'tipo', 'tecnico']) campos[nome].append(new Elemento('option'));
    nodes.get('painel').dataset = {api: '/api/dashboard/'};
    nodes.get('filtros-form').elements = {namedItem: nome => campos[nome]};
    nodes.get('filtros-form').reportValidity = () => !campos.dataFim.validacao;
    nodes.get('filtros-form').reset = () => Object.values(campos).forEach(campo => { campo.value = ''; });
    const requisicoes = [];
    let modo = responder;
    vm.runInNewContext(fonte, {
        document: {getElementById: nome => nodes.get(nome), createElement: tag => new Elemento(tag), createElementNS: (_, tag) => new Elemento(tag)},
        Intl, Date, URLSearchParams, AbortController, Error, TypeError, setTimeout, clearTimeout,
        FormData: class { *[Symbol.iterator]() { for (const [nome, campo] of Object.entries(campos)) yield [nome, campo.value]; } },
        fetch: async (url, options) => {
            requisicoes.push({url, options});
            return modo(url, options);
        }
    });
    return {nodes, campos, requisicoes, responder: novo => { modo = novo; }};
}

function resposta(dados) {
    return url => ({ok: true, status: 200, json: async () => dados[url.split('/').at(-1).split('?')[0]]});
}
async function finalizar() { for (let i = 0; i < 5; i++) await new Promise(resolve => setImmediate(resolve)); }

test('carrega os cinco endpoints e aplica filtros na atualização', async () => {
    const app = ambiente(resposta(fixtures(5)));
    await finalizar();
    assert.equal(app.nodes.get('total').textContent, '5');
    assert.equal(app.requisicoes.length, 5);
    app.campos.status.value = 'Aberto';
    app.nodes.get('filtros-form').dispatch('submit');
    await finalizar();
    assert.ok(app.requisicoes.at(-2).url.includes('status=Aberto'));
    app.nodes.get('atualizar').dispatch('click');
    await finalizar();
    assert.ok(app.requisicoes.at(-2).url.includes('status=Aberto'));
    app.nodes.get('limpar').dispatch('click');
    await finalizar();
    assert.ok(!app.requisicoes.at(-2).url.includes('status='));
});

test('falha parcial preserva dados anteriores e identifica desatualização', async () => {
    const app = ambiente(resposta(fixtures(7)));
    await finalizar();
    app.responder(url => url.includes('/tipos?') ? {ok: false, status: 503, json: async () => ({mensagem: 'Banco indisponível'})} : resposta(fixtures(99))(url));
    app.nodes.get('atualizar').dispatch('click');
    await finalizar();
    assert.equal(app.nodes.get('total').textContent, '7');
    assert.equal(app.nodes.get('erro').hidden, false);
    assert.match(app.nodes.get('erro-texto').textContent, /desatualizados/);
    assert.equal(app.nodes.get('painel').classList.contains('is-stale'), true);
    app.responder(resposta(fixtures(9)));
    app.nodes.get('tentar').dispatch('click');
    await finalizar();
    assert.equal(app.nodes.get('total').textContent, '9');
    assert.equal(app.nodes.get('painel').classList.contains('is-stale'), false);
});

test('sessão expirada oferece login e falha inicial não mantém carregamento', async () => {
    const app = ambiente(() => ({ok: false, status: 401, json: async () => ({mensagem: 'Entre novamente'})}));
    await finalizar();
    assert.equal(app.nodes.get('entrar').hidden, false);
    assert.equal(app.nodes.get('tentar').hidden, true);
    assert.equal(app.nodes.get('painel').attrs['aria-busy'], 'false');
    assert.match(app.nodes.get('lista-chamados').textContent, /Não foi possível carregar/);
});

test('resposta antiga nunca sobrescreve consulta mais recente', async () => {
    let liberar;
    const barreira = new Promise(resolve => { liberar = resolve; });
    const app = ambiente(async url => { await barreira; return resposta(fixtures(1))(url); });
    app.responder(resposta(fixtures(12)));
    app.nodes.get('atualizar').dispatch('click');
    await finalizar();
    liberar();
    await finalizar();
    assert.equal(app.nodes.get('total').textContent, '12');
    assert.equal(app.nodes.get('painel').attrs['aria-busy'], 'false');
});

test('datas invertidas não disparam consulta e limpar remove validação', async () => {
    const app = ambiente(resposta(fixtures(0)));
    await finalizar();
    app.campos.dataInicio.value = '2026-11-15';
    app.campos.dataFim.value = '2026-11-01';
    app.nodes.get('filtros-form').dispatch('submit');
    assert.equal(app.requisicoes.length, 5);
    assert.match(app.campos.dataFim.validacao, /posterior/);
    app.nodes.get('limpar').dispatch('click');
    await finalizar();
    assert.equal(app.campos.dataFim.validacao, '');
    assert.equal(app.nodes.get('total').textContent, '0');
});
