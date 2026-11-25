# Política de Segurança — HabitOS API (CondoEconomy)

A segurança da informação, o isolamento estrito entre condomínios (**Multi-Tenancy**) e a conformidade com a **LGPD** são pilares centrais da **HabitOS API (`condoeconomy-api`)**. Este documento define as versões suportadas, o canal de divulgação responsável de vulnerabilidades, os controles de defesa em profundidade ativos e o checklist de endurecimento (*hardening*) para produção.

---

## Versões Suportadas

As atualizações e correções de segurança são mantidas na branch principal (`main`):

| Versão / Branch | Stack Base | Suporte a Correções de Segurança |
| :--- | :--- | :---: |
| `main` (atual) | Java 17 + Spring Boot 3.2.3 + PostgreSQL 15 | Sim |
| Commits anteriores | — | Não |

---

## Como Reportar uma Vulnerabilidade

Se você identificar qualquer vulnerabilidade de segurança (ex.: bypass de autenticação/RBAC, vazamento entre schemas Multi-Tenant, IDOR, XSS/SQLi, falha de validação de webhook ou exposição de dados pessoais):

1. **Não abra uma Issue pública** descrevendo como explorar a falha.
2. Envie o relato de forma privada via **GitHub Security Advisories** (*Aba `Security` → `Advisories` → `Report a vulnerability`*) ou diretamente ao mantenedor do repositório.
3. Inclua no relato:
   - Endpoint, filtro ou componente afetado (ex.: `SecurityFilter`, `TenantFilter`, `WebhookAsaasController`, `WebSocketConfig`);
   - Impacto estimado e pré-condições (papel/perfil necessário);
   - Passos claros para reprodução;
   - Sugestão de correção, se disponível.

### Prazos de Referência
- **Confirmação de recebimento:** até 48 horas úteis.
- **Triagem e validação técnica:** até 5 dias úteis.
- **Correção com testes automatizados de regressão:** priorizada de acordo com a severidade.

---

## Controles de Defesa em Profundidade (AppSec & LGPD)

1. **Autenticação JWT & Token Kill Switch (`TokenService`, `SecurityFilter`, `AuthController`):**
   - Tokens `HMAC-SHA256` (`Auth0 Java-JWT`) com expiração de 2 horas, entregues via Bearer Token e cookie `HttpOnly` (`HABITOS_SESSION`, `SameSite=Lax`).
   - Validação de entropia mínima da chave (`>= 32 bytes`) e trava de inicialização (`Prod Guard`) que impede boot no profile `prod` caso `JWT_SECRET` contenha a chave padrão de desenvolvimento.
   - Incremento do `tokenVersion` em logout, troca/redefinição de senha, encerramento de sessões e bloqueio administrativo de conta, revogando instantaneamente JWTs previamente emitidos.

2. **Isolamento Multi-Tenant e Anti-Spoofing (`TenantFilter`, `SecurityFilter`):**
   - `TenantFilter` restringe o cabeçalho `X-Tenant-ID` à expressão regular `^[a-zA-Z0-9_]{1,63}$`, recaindo para o schema padrão (`public`) em qualquer entrada malformada e garantindo limpeza do `TenantContext` em bloco `finally`.
   - `SecurityFilter` compara a claim `tenantId` assinada no JWT com `X-Tenant-ID` e bloqueia (`HTTP 403 Forbidden` + evento forense `TENTATIVA_SPOOFING_TENANT` de severidade `CRITICO`) qualquer tentativa de acesso cruzado entre condomínios.

3. **Autorização RBAC e Step-Up Authentication (`SecurityConfig`, `BoletoController`, `PerfilController`):**
   - `@EnableMethodSecurity` ativo somado ao mapeamento explícito por papel (`MORADOR`, `PORTEIRO`, `SINDICO`, `ADMIN`, `SUPER_ADMIN`).
   - Operações financeiras e sensíveis (baixa manual de boletos via `X-Step-Up-Password` e alteração de senha) exigem revalidação da senha atual em `BCrypt`.

4. **Anti-Força Bruta, Rate Limit e Recuperação via OTP (`LoginAttemptService`, `PasswordRecoveryService`):**
   - Bloqueio temporário (15 min) após 5 falhas por `e-mail + IP` e após 10 falhas globais na conta (proteção contra rotação de IPs), com cache limitado (`10.000` entradas).
   - Rate limit por IP em rotas públicas (auto check-in, recuperação e redefinição de senha).
   - Códigos OTP de 6 dígitos gerados com `SecureRandom`, armazenados apenas como hash `SHA-256(email:codigo)`, expiração de 15 minutos, resposta uniforme anti-enumeração e revogação automática após 5 erros consecutivos.

5. **WebSocket/STOMP Autenticado (`WebSocketConfig`):**
   - Origens restritas a `app.cors.allowed-origins`.
   - `ChannelInterceptor` valida JWT, status ativo da conta e `tokenVersion` nos frames `CONNECT` e bloqueia `SUBSCRIBE` em sessões não autenticadas.

6. **Sanitização, Anti-IDOR e LGPD (`SecuritySanitizer`, `AuditoriaAdminController`, `PerfilController`):**
   - Bloqueio de assinaturas XSS, SQLi e bytes nulos/caracteres de controle; validação de URLs/Data URIs de imagem (`http/https` ou `data:image/jpeg|png|webp` com limite máximo de ~2 MB).
   - Neutralização de **CSV Formula Injection (CWE-1236)** em exportações.
   - Mascaramento de CPF/RG e e-mail em conformidade com a minimização de dados e portabilidade via `GET /api/v1/perfil/lgpd/exportar-dados` (Art. 18 da LGPD).
   - Trilha forense com Hibernate Envers e tabela `auditoria_seguranca`.

7. **Webhooks Financeiros (`WebhookAsaasController`):**
   - Verificação do header `asaas-access-token` em **tempo constante** (`MessageDigest.isEqual`) contra *timing attacks* e controle de **idempotência** em memória limitada (`5.000` eventos) contra *replay* de `PAYMENT_RECEIVED`.

---

## Checklist de Segurança para Produção

- [ ] Definir `SPRING_PROFILES_ACTIVE=prod` e uma chave aleatória forte em `JWT_SECRET` (mínimo 32 bytes / 256 bits).
- [ ] Configurar `DB_USERNAME` e `DB_PASSWORD` exclusivos, sem reutilizar valores de desenvolvimento do `application.properties`.
- [ ] Configurar `ASAAS_WEBHOOK_TOKEN` com segredo forte e exclusivo em produção (nunca utilizar fallback local).
- [ ] Restringir `CORS_ALLOWED_ORIGINS` exclusivamente aos domínios HTTPS confiáveis do front-end.
- [ ] Substituir chaves VAPID (`vapid.public.key` / `vapid.private.key`) e credenciais SMTP (`MAIL_USERNAME` / `MAIL_PASSWORD`) por segredos gerenciados via cofre ou variáveis de ambiente.
- [ ] Manter `management.endpoint.health.show-details=when-authorized` e nunca expor `/actuator/**` publicamente além de `/actuator/health` e `/actuator/info`.
- [ ] Garantir que a suíte de testes de segurança (`mvn test`) passe integralmente antes de cada deploy.

---

## Repositório Relacionado

Para diretrizes de segurança do front-end React/PWA, consulte **[`PedroNilton/condoeconomy-web`](https://github.com/PedroNilton/condoeconomy-web)**.
