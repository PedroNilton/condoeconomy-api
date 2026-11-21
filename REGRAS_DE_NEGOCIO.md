# Regras de Negócio e Domínio — HabitOS API (CondoEconomy)

Este documento descreve as regras de domínio, políticas de controle de acesso (RBAC), garantias jurídicas/financeiras e controles de privacidade (LGPD) implementados no back-end.

---

## 1. Controle de Acesso Baseado em Papéis (RBAC)

A autorização combina regras de URL no `SecurityConfig` com anotações `@PreAuthorize` nos controllers (`@EnableMethodSecurity`):

- **`ROLE_MORADOR`:**
  - Acesso restrito aos dados da própria conta e unidade (boletos da unidade, chamados vinculados ao seu `usuario_id (UUID)`, reservas, visitantes, encomendas, veículos, pets e moradores adicionais).
  - Qualquer tentativa de acessar recursos de outro morador ou outra unidade (IDOR/BOLA) retorna `HTTP 403 Forbidden` e gera alerta na auditoria de segurança.

- **`ROLE_PORTEIRO`:**
  - Opera o fluxo físico da guarita: registro e check-in/check-out de visitantes, recebimento e entrega de encomendas, conferência de lista de convidados de reservas do dia, consulta rápida de veículos por placa e triagem/atendimento inicial de chamados com escalonamento para o síndico.
  - Não possui acesso à emissão/baixa de boletos, balancete financeiro, criação de assembleias ou console de auditoria forense.

- **`ROLE_SINDICO` / `ROLE_ADMIN`:**
  - Gestão completa do condomínio: publicação e exclusão de comunicados oficiais, aprovação/rejeição de reservas, criação e encerramento de assembleias virtuais, resolução de chamados de ouvidoria, emissão de boletos e despesas, baixa manual de boletos (com *Step-Up Auth*), gestão/importação CSV de moradores, estoque da CondoStore e consulta à trilha de auditoria.
  - **Prevenção de Escalação de Privilégio:** Um `ROLE_SINDICO` não pode criar ou promover contas para `ROLE_SUPER_ADMIN` (`HTTP 403 Forbidden`).

- **`ROLE_SUPER_ADMIN`:**
  - Provisionamento e listagem global de condomínios/tenants (`/api/v1/condominios`) e acesso a diagnósticos avançados.

---

## 2. Regras de Domínio por Módulo

### 2.1. Assembleia Virtual e Restrição de Inadimplência (`VotacaoService`)
- Apenas `ROLE_SINDICO` / `ROLE_ADMIN` podem abrir ou encerrar pautas de votação.
- **Bloqueio Legal de Inadimplentes (Art. 1.335, III do Código Civil):** Antes de computar o voto, o `VotacaoService.verificarAdimplencia` consulta os boletos da unidade do morador. Caso exista boleto com status `VENCIDO` (ou `PENDENTE` com vencimento anterior à data atual), o voto é recusado com `IllegalStateException`.
- **Unicidade de Voto:** Cada morador só pode registrar um único voto por pauta ativa.
- **Encerramento Automático:** A rotina `RotinasAgendadasService` encerra automaticamente pautas abertas cujo prazo (`dataEncerramento`) expirou.

### 2.2. Reservas de Áreas Comuns (`CriarReservaUseCase` & `ReservaController`)
- **Conflito por Janela de Horário:** O sistema verifica se já existe uma reserva `APROVADA` para a mesma área comum e data cuja janela `[inicio, fim)` se sobreponha ao horário solicitado (`inicioSolicitado < fimExistente && fimSolicitado > inicioExistente`), permitindo reservas em turnos distintos no mesmo dia.
- **Fluxo de Aprovação:** Toda reserva nasce como `PENDENTE_APROVACAO`. O síndico pode aprová-la ou rejeitá-la informando o `motivoRejeicao`.
- **Expiração Automática:** Reservas não respondidas até a data do evento são automaticamente marcadas como `REJEITADA` pela rotina agendada.

### 2.3. Ouvidoria e Chamados (`Chamado` & `ChamadoController`)
- **Categorias Oficiais:** `RECLAMACAO`, `SUGESTAO`, `MANUTENCAO`, `DUVIDA`, `SEGURANCA`, `ELOGIO` e `OUTROS`.
- **Propriedade Imutável por `UUID`:** O chamado é vinculado ao `usuario_id (UUID)` do morador autenticado e à sua unidade (`Apto X - Bloco Y`), impedindo falsidade ideológica ou acesso cruzado por homônimos.
- **Ciclo de Vida e Reabertura:** Suporta transições entre `ABERTO`, `EM_ANDAMENTO` e `RESOLVIDO` (permitindo reabrir chamados resolvidos ao iniciar novo atendimento), escalonamento pela portaria com `notaPortaria` e thread de mensagens (`chamado_mensagem`) com identificação do papel do autor (`MORADOR`, `PORTARIA`, `SINDICO`).

### 2.4. Financeiro, Boletos e Step-Up Auth (`BoletoController` & `FinancasController`)
- **Isolamento de Consulta:** Um morador só pode listar boletos cuja `unidadeTexto` corresponda ao seu apartamento/bloco.
- **Baixa Manual com Step-Up Auth:** Para quitar manualmente um boleto em `/api/v1/boletos/{id}/baixar`, o síndico deve enviar sua senha atual no cabeçalho `X-Step-Up-Password`. Falhas são registradas na auditoria financeira.
- **Baixa Automática via Webhook (`WebhookAsaasController`):** Notificações `PAYMENT_RECEIVED` do Asaas são autenticadas em tempo constante e processadas de forma idempotente.
- **Exportação Segura (CWE-1236):** Relatórios CSV passam por `SecuritySanitizer.sanitizarCsvFormula`, prefixando `'` em células iniciadas por `=`, `+`, `-`, `@`.

### 2.5. Comunicados com Confirmação de Leitura (`AvisoController`)
- Comunicados suportam marcação de `urgente`, `fixado` no topo e categoria.
- Quando o morador abre um comunicado (`POST /api/v1/avisos/{id}/visualizar`), o registro é salvo de forma única por `(aviso_id, usuario_id)` na tabela `aviso_visualizacao`, incrementando o contador de leituras exibido ao síndico.

### 2.6. Privacidade, Segurança da Conta e LGPD (`PerfilController`)
- **Minimização de Dados:** A consulta de veículos da portaria retorna nome do morador, unidade, telefone de contato, placa, modelo, cor e vaga, sem expor hashes, CPF ou dados financeiros.
- **Direito à Portabilidade (Art. 18 da LGPD):** O titular pode exportar a qualquer momento um pacote JSON consolidado com seus dados cadastrais, veículos, pets, moradores adicionais, preferências e histórico de auditoria em `GET /api/v1/perfil/lgpd/exportar-dados`.
