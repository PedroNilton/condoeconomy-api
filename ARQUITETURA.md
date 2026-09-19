# Arquitetura do Sistema - CondoEconomy API

Este documento detalha a arquitetura de software adotada para o Back-end da plataforma CondoEconomy. O projeto foi desenhado focando em escalabilidade, manutenibilidade e segurança, características essenciais para um produto B2B/SaaS voltado para Administradoras de Condomínios.

## 1. Padrão Arquitetural

A aplicação adota os princípios da **Clean Architecture (Arquitetura Limpa)** e **Ports and Adapters (Arquitetura Hexagonal)**. O objetivo principal é manter as regras de negócio isoladas de frameworks, bancos de dados e detalhes de entrega web (HTTP).

### Estrutura de Pacotes (`com.condoeconomy.api`)

- **`/domain`**: O coração do software. Contém as entidades de negócio puras (ex: `Usuario`, `Reserva`, `Boleto`) e interfaces de regras de negócio. Não possui dependências do Spring.
- **`/application`**: Casos de uso (Use Cases) da aplicação. Orquestra o fluxo de dados entre as portas de entrada e o domínio. (Ex: `AutorizarVisitanteUseCase`).
- **`/presentation` (Controllers)**: A porta de entrada HTTP. Contém os `RestControllers` e `DTOs`. Recebe requisições REST, traduz para objetos de domínio e chama os casos de uso.
- **`/infrastructure`**: Implementações técnicas. Contém adaptadores de banco de dados (`SpringDataRepository`, Entidades JPA), configurações do Spring Security, JWT, e integrações com APIs externas (ex: Gateways de pagamento).

## 2. Stack Tecnológica
- **Java 17+** com **Spring Boot 3.x**
- **Banco de Dados:** PostgreSQL (Relacional)
- **Migrações:** Flyway (Versionamento de banco de dados e criação de tabelas automáticas no start).
- **Segurança:** Spring Security com autenticação Stateless (JWT - JSON Web Tokens).
- **Lombok:** Redução de boilerplate de código.

## 3. Fluxo de Autenticação (Segurança)
1. O Front-end envia credenciais para `/api/v1/auth/login`.
2. A infraestrutura valida no banco e, caso correto, gera um token JWT assinado criptograficamente contendo a `ROLE` (Morador, Síndico, Porteiro) no Payload.
3. Nas requisições subsequentes, o Spring Security (via Filter) intercepta o token no header `Authorization: Bearer <token>`, valida a assinatura e aplica regras de `@PreAuthorize("hasRole(...)")` diretamente nos controllers.

## 4. Integrações Futuras Previstas na Infraestrutura
Os adaptadores (Adapters) de infraestrutura já estão organizados para plugar facilmente:
- Emissão de Boletos (Asaas, Cora, Inter).
- Disparo de Notificações via WhatsApp.
- Reconhecimento Facial (Hardware de Portaria).
