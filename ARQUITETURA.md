# Arquitetura do Sistema — HabitOS API (CondoEconomy)

Este documento detalha a arquitetura de software, o modelo Multi-Tenant e as integrações da API Back-end (`com.condoeconomy.api`).

---

## 1. Padrão Arquitetural (Clean Architecture & Hexagonal)

A aplicação organiza suas responsabilidades em quatro camadas concêntricas dentro do pacote raiz `com.condoeconomy.api`:

```text
com.condoeconomy.api
 ├── core/                  # Segurança transversal, Multi-Tenancy e configurações globais
 │    ├── config/           # MultiTenantJpaConfiguration, OpenApiConfig, WebSocketConfig
 │    ├── security/         # SecurityConfig, SecurityFilter, TokenService, LoginAttemptService,
 │    │                     # PasswordRecoveryService, SecuritySanitizer, SecurityAuditService
 │    └── tenant/           # TenantFilter, TenantContext, TenantConnectionProvider, TenantIdentifierResolver
 ├── domain/                # Entidades de domínio puras, enums e regras de negócio invariantes
 │    └── entity/           # Chamado, Reserva, Visitante, Encomenda, Boleto, Votacao, Produto, Aviso...
 ├── application/           # Casos de Uso (Use Cases), Gateways (Portas) e Serviços de Aplicação
 │    ├── gateway/          # Contratos de repositório desacoplados do JPA
 │    ├── service/          # VotacaoService, RotinasAgendadasService, AsaasGatewayPagamentoServiceImpl,
 │    │                     # FirebasePushNotificationServiceImpl, WebPushNotificationServiceImpl
 │    └── usecase/          # Orquestração de fluxos de negócio (CriarReservaUseCase, EscalarChamadoUseCase...)
 ├── infrastructure/        # Adaptadores técnicos (Spring Data JPA, Entidades JPA, PostgreSQL)
 │    └── persistence/      # Entidades *JpaEntity e repositórios SpringData*Repository
 └── presentation/          # Porta de entrada HTTP/REST
      ├── controller/       # RestControllers organizados por contexto e GlobalExceptionHandler
      └── dto/              # Records imutáveis com Bean Validation (@Valid)
```

---

## 2. Isolamento Multi-Tenant (Schema-per-Tenant)

O HabitOS suporta múltiplos condomínios isolados no mesmo cluster PostgreSQL:

1. **`TenantFilter`:** Intercepta a requisição HTTP e lê o cabeçalho `X-Tenant-ID`, validando-o contra a expressão regular `^[a-zA-Z0-9_]{1,63}$` (prevenindo *Schema Tampering* e *SQL Injection*).
2. **`SecurityFilter` (Tenant Sandbox):** Para usuários autenticados (exceto `ROLE_SUPER_ADMIN`), compara o `X-Tenant-ID` enviado na requisição com a claim `tenantId` assinada dentro do JWT. Qualquer divergência é bloqueada imediatamente com `HTTP 403 Forbidden` e registrada na trilha forense como `TENTATIVA_SPOOFING_TENANT` (`CRITICO`).
3. **`TenantConnectionProvider`:** Ajusta o `search_path` da conexão JDBC para o schema do condomínio antes de executar as queries do Hibernate.

---

## 3. Fluxo de Autenticação, Sessão e WebSocket

1. **Login (`POST /api/v1/auth/login`):**
   - Verifica bloqueio por força bruta (`LoginAttemptService`: limite por `email + IP` e limite global por conta).
   - Autentica via `AuthenticationManager` + `BCryptPasswordEncoder`.
   - Emite um JWT (`HMAC-SHA256`, validade de 2 horas) contendo `sub` (e-mail), `papel`, `tokenVersion` e `tenantId`, entregando-o também em cookie `HttpOnly` (`HABITOS_SESSION`).
2. **Validação por Requisição (`SecurityFilter`):**
   - Verifica assinatura do JWT, status ativo do usuário (`isEnabled()`) e igualdade estrita entre a claim `tokenVersion` do token e o `tokenVersion` atual no banco (*Token Kill Switch*).
3. **Tempo Real (`WebSocketConfig`):**
   - Conexões em `/ws` passam por `HandshakeInterceptor` e `ChannelInterceptor`, exigindo JWT válido e não revogado nos frames STOMP `CONNECT` e `SUBSCRIBE`.

---

## 4. Integrações e Rotinas Automatizadas Implementadas

- **Gateway de Pagamento Asaas (`AsaasGatewayPagamentoServiceImpl` & `WebhookAsaasController`):**
  - Emite cobranças via API v3 do Asaas quando `ASAAS_API_KEY` está configurada e opera automaticamente em **contingência local resiliente** (gerando linha digitável compatível) em ambiente de desenvolvimento.
  - Recebe notificações de pagamento em `POST /api/v1/webhooks/asaas` com validação de token em tempo constante (`MessageDigest.isEqual`) e proteção de idempotência.
- **Notificações Push (`WebPushNotificationServiceImpl` & `FirebasePushNotificationServiceImpl`):**
  - Suporte a Web Push (VAPID) para navegadores/PWA e Firebase Cloud Messaging (FCM) com degradação graciosa em modo local.
- **Rotinas Agendadas (`RotinasAgendadasService`):**
  - Atualização automática de boletos `PENDENTE` vencidos para `VENCIDO`.
  - Encerramento automático de assembleias/votações após o prazo final (`dataEncerramento`).
  - Cancelamento automático de solicitações de reserva `PENDENTE_APROVACAO` cuja data do evento já passou.
