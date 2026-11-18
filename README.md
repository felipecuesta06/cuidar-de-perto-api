# Cuidar de Perto 
> FIAP · Graduação · 2º ano ADS · Turma **2TDSPR** · Global Solution 2026/2 · CitizenTech

Monitoramento de rotina e emergência de idosos que vivem sozinhos, com alertas automáticos para cuidadores e responsáveis.
API em **Spring Boot 3 / Java 17**, 
## Integrantes

| Nome completo | RM |
|---|---|
|Felipe Cuesta Puerta de Oliveira| 567703|
| Vitor Costa de Godoi| 568448|
| Vitor Santos Domingues|568375|
| João Victor De Souza Braz| 566862|
| Lucas Marcelino Pompeu|567010|

## LINK PITCH

https://youtu.be/fdUkNQqSLpg


---

## 1. A solução

**Problema:** muitos idosos moram sozinhos e ficam sem acompanhamento: esquecem remédios, têm alterações de saúde que passam despercebidas e, numa emergência, podem demorar a receber ajuda.

**Cidadão beneficiado:** o idoso que mora sozinho (ou com poucos recursos) e a família que o acompanha à distância.

**Como funciona:** a API conecta quatro atores — idoso, cuidador profissional, responsável (familiar) e sensores.

- O **cuidador** cadastra os idosos que acompanha, define os remédios e os limites ideais de saúde (batimentos, glicemia, SpO2, pressão) e vincula os **responsáveis** (com parentesco e prioridade de contato).
- O **idoso** consulta seus lembretes, confirma a tomada, vê seu estado atual e aciona o **botão de pânico**.
- Os **sensores** enviam *eventos* à API, que compara cada leitura com os limites do idoso e classifica a gravidade (BAIXA, MÉDIA, ALTA, CRÍTICA).
- Leituras fora do ideal geram **alertas**. Alertas ALTA/CRÍTICA notificam também os responsáveis.

### ODS

| ODS | Contribuição |
|---|---|
| **3 — Saúde e bem-estar** | Prevenção e resposta rápida a emergências, adesão ao tratamento por lembretes de medicamentos e acompanhamento contínuo de sinais vitais. |
| **10 — Redução das desigualdades** | Leva monitoramento de qualidade a idosos isolados ou com poucos recursos e inclui a família na rede de cuidado, mesmo à distância. |

---

## 2. Como rodar

### Pré-requisitos
- Java 17 ou superior
- Maven 3.9+ (ou use o Maven da sua IDE)

### Variáveis de ambiente


| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | **Sim** | Chave de assinatura do JWT, **mínimo 32 caracteres**. |


### Executar com H2 (desenvolvimento — padrão)

```bash
# Linux
export JWT_SECRET="troque-por-uma-chave-longa-e-aleatoria-com-32+-caracteres"
mvn spring-boot:run
```

```powershell
# Windows 
$env:JWT_SECRET = "troque-por-uma-chave-longa-e-aleatoria-com-32+-caracteres"
mvn spring-boot:run
```

A API sobe em **http://localhost:8080**. O banco H2 é em memória e o seed (`data.sql`) cria os usuários de teste e dados de exemplo a cada inicialização.




### Endereços úteis

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Health (público) | http://localhost:8080/actuator/health |
| Info / Metrics (ADMIN) | http://localhost:8080/actuator/info · `/actuator/metrics` |

---

## 3. Usuários de teste (somente ambiente local)

| Papel | E-mail | Senha |
|---|---|---|
| ROLE_ADMIN | admin@cuidar.com | `Admin@123` |
| ROLE_CUIDADOR | cuidador@cuidar.com | `Cuidador@123` |
| ROLE_RESPONSAVEL | responsavel@cuidar.com | `Responsavel@123` |
| ROLE_IDOSO | idoso@cuidar.com | `Idoso@123` |

O seed também cria a idosa *Dona Maria Aparecida* vinculada ao cuidador e ao responsável de teste, com dois lembretes (Losartana 08:00 e Metformina 20:00).

### Fluxo rápido (curl)

```bash
# 1) Login
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"cuidador@cuidar.com","senha":"Cuidador@123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# 2) Enviar leitura crítica de um sensor (batimentos 145 bpm) -> gera alerta CRITICO
curl -s -X POST localhost:8080/api/eventos-sensor -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"idosoId":1,"tipo":"BATIMENTO","valor":145,"dispositivo":"pulseira-001"}'

# 3) Painel urgente do cuidador
curl -s localhost:8080/api/alertas/painel-urgente -H "Authorization: Bearer $TOKEN"
```

No Swagger UI: faça `POST /api/auth/login`, copie o `token`, clique em **Authorize** e cole o JWT.

---

## 4. Segurança (JWT, papéis, vínculo e CORS)

**Fluxo:** login → token → filtro → papel → `@PreAuthorize` → regra de vínculo.

1. `POST /api/auth/login` valida e-mail/senha (BCrypt) e devolve um JWT assinado (HS256) com expiração de 2 h (`app.jwt.expiration-seconds`).
2. `JwtAuthFilter` valida o token a cada requisição e carrega o papel no contexto de segurança.
3. Os controllers usam `@PreAuthorize` por papel e, para recursos de um idoso, a **regra de vínculo** (`AcessoService`, bean `@acesso`):
   - cuidador só enxerga e altera idosos **vinculados a ele**;
   - responsável só enxerga idosos **aos quais está vinculado** (somente leitura);
   - idoso só enxerga **os próprios dados**;
   - violações retornam **403**.
4. Cadastro público (`POST /api/auth/cadastro`) aceita somente CUIDADOR ou RESPONSAVEL; ADMIN e IDOSO são criados pelo seed/administração.
5. **CORS** liberado para as origens configuradas em `app.cors.allowed-origins`.

**LGPD:** dados de saúde são sensíveis — acesso restrito por papel e vínculo, CPF **nunca** aparece em listagens (no detalhe sai mascarado) e senhas usam BCrypt.

### Erros padronizados

Corpo único: `timestamp, status, erro, mensagem, caminho, campos[]`.

| Status | Quando |
|---|---|
| 400 | validação (Bean Validation), JSON/enum inválido, campo de ordenação inexistente |
| 401 | sem token, token inválido/expirado ou credenciais erradas |
| 403 | sem permissão ou sem vínculo com o idoso |
| 404 | recurso não encontrado |
| 409 | conflito (CPF/e-mail duplicado) |
| 422 | regra de negócio violada |

---

## 5. Modelo de dados

```
+-------------+ 1    N +------------------+ N      1 +--------------+
|  Cuidador   |--------|      Idoso       |----------| Responsavel  |
+-------------+  (N:N) +------------------+  (N:N)   +--------------+
                         |1       |1      |1
                         |N       |N      |N
              +------------------+ |  +----------------+
              | LembreteRemedio  | |  |  EventoSensor  |
              +------------------+ |  +----------------+
                                   |N          |1
                                   |           |0..N
                                +------------------+
                                |      Alerta      |
                                +------------------+

Usuario (login) 1---1 Cuidador | Responsavel | Idoso   (um usuário por pessoa)
```

A relação N:N Idoso × Responsável é uma **entidade própria** (`IdosoResponsavel`), pois guarda `parentesco` e `prioridadeContato`. A relação Idoso × Cuidador usa `@ManyToMany` simples (`idoso_cuidador`).

### Decisões de modelagem

- **`fetch = LAZY` em todos os `@OneToMany`, `@ManyToMany` e `@ManyToOne`**, para não carregar o grafo inteiro. Os DTOs expõem só o necessário e evitam `LazyInitializationException` (`open-in-view=false`).
- **Cascade:**
  - `Idoso → LembreteRemedio`: `CascadeType.ALL` + `orphanRemoval` — o lembrete não existe sem o idoso.
  - `Idoso → EventoSensor` e `Idoso → Alerta`: **sem cascade de remoção**, para preservar o histórico de saúde. O idoso é **inativado logicamente** (`ativo = false`), nunca apagado.
- **N:N com atributos** (Idoso × Responsável): entidade de vínculo própria com `parentesco` e `prioridadeContato`.
- **Índices:** `EventoSensor(idoso_id, data_hora)` e `Alerta(idoso_id, status)`, para as consultas mais frequentes (histórico/estado atual e alertas abertos).

---

## 6. Regras de negócio

**Classificação** (limites padrão, ajustáveis por idoso pelo cuidador — referências de exemplo do projeto acadêmico, não orientação médica):

| Sinal | Normal | MÉDIA | ALTA | CRÍTICA |
|---|---|---|---|---|
| Batimentos (bpm) | 60–100 | 50–59 ou 101–120 | 40–49 ou 121–140 | < 40 ou > 140 |
| Glicemia (mg/dL) | 70–140 | 60–69 ou 141–200 | 54–59 ou 201–300 | < 54 ou > 300 |
| SpO2 (%) | 95–100 | 92–94 | 88–91 | < 88 |
| Queda | — | — | detectada (`valor=1`) | sem resposta em 2 min (`valor=2` ou escalonamento automático) |
| Botão de pânico | — | — | — | sempre CRÍTICA |

**Escalonamento**

| Gravidade | Quem é notificado | Ação |
|---|---|---|
| BAIXA / MÉDIA | Cuidador | Alerta ABERTO no painel do cuidador |
| ALTA | Cuidador + responsável de maior prioridade | Sem atendimento em 5 min, escala para todos os responsáveis |
| CRÍTICA | Cuidador + **todos** os responsáveis | Notifica imediatamente e registra a lista de notificados no alerta |

**Outras regras:** lembrete não confirmado em 30 min gera alerta MÉDIA (3 omissões seguidas do mesmo remédio sobem para ALTA); **anti-duplicidade** (alerta ABERTO do mesmo tipo para o mesmo idoso é agregado, incrementando `ocorrencias`); resolução só por cuidador vinculado ou admin e **exige observação**; notificação **simulada** (log + registro no alerta) atrás da interface `NotificacaoService`, para trocar por push/SMS/e-mail sem alterar o domínio. As verificações automáticas rodam em `MonitoramentoScheduler` (a cada 60 s).

### Premissas assumidas (a spec não define)

- **Pressão arterial:** classificada pela sistólica; mínima assumida de 90 mmHg (hipotensão) e margens de 20/40 mmHg acima do `pressaoSistMax`.
- **"Dispositivo"** (envio de leitura): não há papel próprio; o envio é feito com o token do idoso, do cuidador vinculado ou do admin.
- **Queda:** `valor = 1` (detectada) e `valor = 2` (sem resposta); se a queda ALTA ficar ABERTA por 2 min, o agendador a eleva para CRÍTICA.

---

## 7. Consultas com Spring Data JPA

| Tipo | Onde | Por que esse tipo |
|---|---|---|
| **Query method derivado** | `AlertaRepository.findByIdosoIdAndStatus`, `LembreteRepository.findByIdosoIdAndAtivoTrue`, `IdosoRepository.existsByCpf` | Consultas simples e diretas: o nome do método já documenta a regra e dispensa SQL. |
| **`@Query` (JPQL)** | `AlertaRepository.painelUrgente` (JOIN FETCH + subconsulta + ordenação), `contagemPorGravidade` (GROUP BY), `EventoSensorRepository.historico*` | Filtros combinados, ordenação, agregação e `JOIN FETCH` que o nome do método não expressa bem. |
| **Specification (filtros dinâmicos)** | `IdosoSpecs` (nome, faixa de idade, condição médica, ativo) e `AlertaSpecs` (gravidade, status, idoso, período) | A busca do painel tem filtros opcionais; Specification evita dezenas de métodos para cada combinação — os filtros só entram quando o parâmetro vem preenchido. |

**Consulta crítica do domínio** (`painelUrgente`): alertas ABERTOS de gravidade ALTA/CRÍTICA dos idosos do cuidador. A gravidade é gravada como texto, então a ordenação usa `CASE` (CRÍTICA primeiro, depois os mais antigos) em vez de `ORDER BY gravidade`, que seria alfabético.

---

## 8. Cache

Caffeine (em memória) com TTL curto (**45 s**, `app.cache.ttl-seconds`) para limitar a defasagem. Estatísticas habilitadas: veja hits/misses em `/actuator/metrics/cache.gets`.

| Cache | `@Cacheable` | Invalidação (`@CacheEvict`) | Justificativa |
|---|---|---|---|
| `idoso-estado` | `GET /idosos/{id}/estado` (chave: idosoId) | Ao receber EventoSensor, criar/resolver Alerta do idoso | Tela mais consultada (app do idoso e da família); evita varrer o histórico a cada abertura. |
| `painel-cuidador` | `painelUrgente` (chave: cuidadorId + página) | Ao criar ou alterar alerta de qualquer idoso vinculado | Painel é atualizado com frequência e tem agregações pesadas. |
| `lembretes-dia` | Lembretes ativos do idoso (chave: idosoId) | Ao criar, alterar, excluir ou confirmar lembrete | Dado muda pouco durante o dia e é lido a cada abertura do app. |

**O que NÃO é cacheado:** o recebimento de eventos de sensor e a listagem de alertas em tempo real — dados desatualizados numa emergência seriam um risco. A autorização é verificada **antes** de chegar ao método cacheado, então o cache nunca vaza dados entre usuários.

---

## 9. Endpoints

Prefixo `/api`. Todos exigem `Authorization: Bearer <token>`, exceto cadastro e login. Listagens aceitam `page`, `size` e `sort`. Todos estão documentados no Swagger UI.

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/auth/cadastro` | Cria conta (cuidador ou responsável) | Público |
| POST | `/api/auth/login` | Retorna o JWT | Público |
| POST / GET | `/api/idosos` | Cadastra (201) / lista paginada com filtros | CUIDADOR, ADMIN |
| GET / PUT / DELETE | `/api/idosos/{id}` | Detalhe com links HATEOAS / atualiza / inativa (204) | Vinculados / CUIDADOR, ADMIN |
| GET | `/api/idosos/{id}/estado` | Último valor de cada sinal + alertas abertos | Vinculados e o próprio idoso |
| POST / DELETE | `/api/idosos/{id}/responsaveis/{respId}` | Vincula (parentesco, prioridade) / remove vínculo | CUIDADOR, ADMIN |
| GET | `/api/idosos/{id}/responsaveis` | Lista responsáveis vinculados | Vinculados |
| POST / DELETE | `/api/idosos/{id}/cuidadores/{cuidadorId}` | Vincula / remove cuidador | CUIDADOR vinculado, ADMIN |
| CRUD | `/api/cuidadores` · `/api/responsaveis` | Cadastro e manutenção | ADMIN / o próprio |
| POST / GET | `/api/idosos/{id}/lembretes` | Cria / lista lembretes (ativos do dia ou `ativos=false`) | CUIDADOR / Vinculados e idoso |
| PUT / DELETE | `/api/lembretes/{id}` | Altera / remove | CUIDADOR, ADMIN |
| PATCH | `/api/lembretes/{id}/confirmar` | Registra a tomada | IDOSO, CUIDADOR |
| POST | `/api/eventos-sensor` | Recebe leitura, classifica e gera alerta | Idoso / cuidador / admin |
| GET | `/api/idosos/{id}/eventos-sensor` | Histórico com filtro por tipo e período | Vinculados |
| POST | `/api/idosos/{id}/emergencia` | Botão de pânico (alerta CRÍTICO) | IDOSO |
| GET | `/api/alertas` | Lista com filtros (gravidade, status, idoso, período) | CUIDADOR, ADMIN, RESPONSAVEL (no escopo) |
| GET | `/api/alertas/painel-urgente` | Painel urgente do cuidador (cacheado) | CUIDADOR |
| GET | `/api/alertas/{id}` | Detalhe com links de ação | Vinculados |
| PATCH | `/api/alertas/{id}/status` | EM_ATENDIMENTO ou RESOLVIDO (exige observação) | CUIDADOR, ADMIN |
| GET | `/actuator/health` · `info` · `metrics` | Saúde e métricas | ADMIN (health público) |

**HATEOAS:** `Idoso` expõe `self`, `estado`, `lembretes`, `eventos-sensor`, `alertas` e `responsaveis`; `Alerta` expõe `self`, `idoso` e, conforme o status, as ações `assumir`/`resolver` (um alerta RESOLVIDO não exibe links de ação). Coleções paginadas usam `PagedModel` com `next`/`prev`.

---

## 10. Estrutura do projeto

```
cuidar-de-perto-api/
├── pom.xml
├── README.md
├── src/main/java/br/com/fiap/cuidardeperto/
│   ├── CuidarDePertoApplication.java
│   ├── config/      (SecurityConfig, CacheConfig, OpenApiConfig, CorsConfig)
│   ├── security/    (JwtService, JwtAuthFilter, UserDetailsServiceImpl, AcessoService, ...)
│   ├── domain/      (Idoso, Cuidador, Responsavel, LembreteRemedio, EventoSensor, Alerta, Usuario, enums)
│   ├── repository/  (interfaces + Specifications)
│   ├── dto/         (requests e responses)
│   ├── service/     (regras de negócio, ClassificadorSaude, NotificacaoService, agendador)
│   ├── controller/  (controllers REST + assemblers HATEOAS)
│   └── exception/   (exceções e @RestControllerAdvice)
├── src/main/resources/
│   ├── application.properties / application-example.properties
│   └── data.sql     (seed de usuários e dados de teste)
├── src/test/java/   (testes do classificador de saúde)
└── .gitignore
```

---

## 11. Declaração de uso de IA generativa

Ferramenta utilizada: **Gemini**, para apoio na geracao de roteiro para o pitch e apoio para a criacao do README E EDICAO DO VIDEO DO PITCH
