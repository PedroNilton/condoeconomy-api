# HabitOS API (CondoEconomy)

API RESTful e servidor WebSocket/STOMP da plataforma **HabitOS / CondoEconomy**, construída em **Java 17** com **Spring Boot 3.2.3**, **Clean Architecture**, **Multi-Tenancy por schema** e **defesa em profundidade (AppSec & LGPD)**.

---

## Stack Tecnológica

- **Java 17**
- **Spring Boot 3.2.3** (`Web`, `Validation`, `Data JPA`, `Security`, `WebSocket`, `Mail`, `Actuator`)
- **PostgreSQL 15** com **Flyway** (`34` migrations versionadas: `V1` a `V34`)
- **Hibernate Envers** + tabela dedicada `auditoria_seguranca` para trilha forense
- **Auth0 Java-JWT (`4.4.0`)** com assinatura `HMAC-SHA256`
- **SpringDoc OpenAPI 3 (`2.3.0`)** para documentação interativa (Swagger UI)
- **Notificações Push:** Web Push (`nl.martijndwars:web-push` + `BouncyCastle` VAPID) e **Firebase Admin SDK (`9.2.0`)** com fallback local seguro
- **Gateway Financeiro:** Integração nativa com **Asaas API v3** + contingência automática em modo local
- **JUnit 5 & Mockito** (`46` testes automatizados cobrindo segurança, domínio e fluxos operacionais)

---

## Arquitetura de Segurança (Cybersecurity & AppSec)

A API implementa múltiplas barreiras de proteção validadas por testes automatizados:

1. **Autenticação Híbrida & Token Kill Switch (`tokenVersion`):**
   - Suporte a `Authorization: Bearer <jwt>` e cookie `HttpOnly` (`HABITOS_SESSION`, `SameSite=Lax`).
   - Cada usuário possui um contador `tokenVersion`. Eventos críticos (troca de senha, redefinição via OTP, botão *"Encerrar outras sessões"*, bloqueio administrativo de conta ou `POST /api/v1/auth/logout`) incrementam o `tokenVersion` no banco e revogam instantaneamente todos os JWTs emitidos anteriormente.
   - Trava de produção (`TokenService`): impede a inicialização no profile `prod` caso `JWT_SECRET` contenha a chave padrão de desenvolvimento.

2. **Isolamento Multi-Tenant com Anti-Spoofing (`TenantFilter` & `SecurityFilter`):**
   - Resolução dinâmica de schema PostgreSQL via `X-Tenant-ID` validado por regex estrita (`^[a-zA-Z0-9_]{1,63}$`).
   - O `SecurityFilter` cruza a claim `tenantId` assinada no JWT com o header `X-Tenant-ID` e bloqueia (`HTTP 403 Forbidden` + evento forense `CRITICO`) qualquer tentativa de acesso cruzado entre condomínios.

3. **Step-Up Authentication:**
   - Operações sensíveis — como **baixa manual de boletos** (`PUT /api/v1/boletos/{id}/baixar` via header `X-Step-Up-Password`) e **troca de senha** (`PUT /api/v1/perfil/seguranca/alterar-senha`) — exigem revalidação da senha atual do operador via `BCrypt`.

4. **Proteção contra Força Bruta e Ataques Distribuídos (`LoginAttemptService` & `PasswordRecoveryService`):**
   - Bloqueio temporário (15 min) após **5 falhas por `e-mail + IP`** e após **10 falhas globais na mesma conta** (proteção contra botnets com rotação de IPs), com cache limitado (`10.000` entradas) contra exaustão de memória.
   - Recuperação de senha com código OTP de 6 dígitos gerado via `SecureRandom`, persistindo apenas o hash `SHA-256(email:codigo)`, com **revogação automática do OTP após 5 tentativas incorretas** e resposta uniforme anti-enumeração.

5. **WebSocket/STOMP Autenticado (`WebSocketConfig`):**
   - Origens restritas a `app.cors.allowed-origins`.
   - `ChannelInterceptor` valida o JWT e o `tokenVersion` nos frames `CONNECT` e bloqueia `SUBSCRIBE` de sessões não autenticadas.

6. **Anti-IDOR, Sanitização e Conformidade LGPD:**
   - Vínculo imutável por `UUID` (`usuario_id`) em chamados de ouvidoria, notificações e dados de perfil.
   - `SecuritySanitizer`: bloqueio de payloads XSS/SQLi/Null-Byte, limite de `2 MB` para imagens Base64 (`data:image/jpeg|png|webp`), neutralização de **CSV Formula Injection (CWE-1236)** e mascaramento de CPF/RG e e-mail.
   - Endpoint de portabilidade de dados pessoais (`GET /api/v1/perfil/lgpd/exportar-dados` — Art. 18 da LGPD).

7. **Segurança de Webhooks Financeiros (`WebhookAsaasController`):**
   - Validação do header `asaas-access-token` em **tempo constante** (`MessageDigest.isEqual`) contra *timing attacks* e controle de **idempotência** contra *replay* de eventos `PAYMENT_RECEIVED`.

---

## Principais Módulos e Endpoints

- **Autenticação (`/api/v1/auth`):** `/login`, `/logout`, `/recuperar-senha`, `/redefinir-senha`.
- **Comunicados (`/api/v1/avisos`):** Listagem, publicação com flag `urgente` e `fixado`, exclusão e confirmação de leitura (`POST /api/v1/avisos/{id}/visualizar`).
- **Notificações (`/api/v1/notificacoes`):** Listagem por usuário, contagem de não lidas (`/nao-lidas/count`), leitura individual e em lote (`/todas-lidas`).
- **Boletos e Finanças (`/api/v1/boletos`, `/api/v1/financas`):** Boletos do morador, baixa manual com Step-Up Auth, emissão de boletos, gestão de despesas, balancete mensal consolidado e exportação CSV.
- **Reservas (`/api/v1/reservas`):** Áreas comuns, solicitação com verificação de conflito por janela de horário (`inicio`–`fim`), aprovação/rejeição com motivo e check-in de convidados na portaria.
- **Ouvidoria e Chamados (`/api/v1/chamados`):** Abertura com foto anexa, atualização de status, escalonamento com `notaPortaria` e mensagens (`GET/POST /api/v1/chamados/{id}/mensagens`).
- **Visitantes e Encomendas (`/api/v1/visitantes`, `/api/v1/encomendas`):** Pré-autorização, Auto Check-in público com rate limit, entrada/saída na portaria e fluxo de retirada de pacotes.
- **Consulta de Veículos (`/api/v1/portaria/veiculos`):** Busca por placa ou listagem geral com vaga, modelo, cor e telefone do morador.
- **Assembleia Virtual (`/api/v1/votacoes`):** Criação/encerramento de pautas, validação de adimplência (Art. 1.335, III do Código Civil), registro de voto único e apuração.
- **Perfil, Segurança e LGPD (`/api/v1/perfil`):** Dados pessoais, veículos, pets, moradores adicionais (com `email` e `acessoApp`), preferências de notificação, troca de senha, Kill Switch de sessões, histórico de segurança e exportação LGPD.
- **Administração (`/api/v1/admin`):** Gestão de moradores, importação em lote via CSV (`/usuarios/importar-csv`), gestão de estoque da CondoStore e trilha forense de auditoria (`/auditoria`).

---

## Como Executar o Projeto

### Pré-requisitos
- **Java JDK 17+**
- **Maven 3.9+**
- **PostgreSQL 15+** (via Docker Compose ou instalação local na porta `5433`)

### Passo a Passo

1. Suba o banco de dados PostgreSQL via Docker Compose:
```bash
docker compose up -d db
```

2. Execute a aplicação com o profile `local`:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
*(O Flyway aplicará automaticamente as migrations `V1` a `V34` na inicialização.)*

3. Execute a suíte completa de testes automatizados (`46` testes):
```bash
mvn test
```

### URLs Úteis (Ambiente Local)
- **API Base:** `http://localhost:8080`
- **Swagger UI (OpenAPI 3):** `http://localhost:8080/swagger-ui/index.html`
- **Health Check (Actuator):** `http://localhost:8080/actuator/health`
