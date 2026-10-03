# Regras de Negócio e Domínio - HabitOS API

Este documento descreve as regras de negócio centrais, políticas de acesso e a estrutura de domínio do back-end do HabitOS.

## 1. Controle de Acesso (RBAC)
O sistema utiliza um controle de acesso baseado em papéis (Role-Based Access Control) através de tokens JWT.

- **ROLE_MORADOR**: 
  - Possui acesso exclusivo aos seus próprios dados (Perfil, Veículos, Pets).
  - Pode realizar requisições de Reservas de áreas comuns.
  - Pode votar em Assembleias em andamento (um voto por enquete).
  - Pode visualizar seus próprios boletos.
  
- **ROLE_PORTEIRO**:
  - Acesso ao módulo de Portaria.
  - Pode dar entrada e saída em Visitantes e Encomendas.
  - Pode realizar consultas rápidas de Veículos por placa para validar autorização de entrada.
  - **Restrição:** Não possui acesso a dados financeiros, ouvidoria privada e configurações de moradores.

- **ROLE_SINDICO** (e `ROLE_ADMIN`):
  - Possui privilégios totais de leitura do condomínio.
  - Pode aprovar ou rejeitar Reservas solicitadas pelos moradores.
  - Acesso ao painel Financeiro (Receitas, Despesas e Inadimplência).
  - Capacidade de criar e encerrar Assembleias Virtuais e emitir comunicados.

## 2. Regras de Domínio

### 2.1. Reservas de Áreas Comuns
- O morador inicia uma solicitação e ela entra no status `PENDENTE`.
- A data e horário solicitados ficam bloqueados provisoriamente para evitar choque de horários (dupla reserva).
- O Síndico analisa e altera o status para `APROVADA` ou `REJEITADA`.

### 2.2. Assembleia Virtual
- Apenas usuários com perfil de Síndico podem cadastrar enquetes e ditar a data de expiração.
- Um morador logado só pode registrar **um único voto** por opção/enquete ativa.
- **Regra Futura (Inadimplência):** Moradores com boletos atrasados perdem o direito a voto (conforme legislação condominial padrão).

### 2.3. Consulta de Veículos
- Os veículos cadastrados pelo morador são vinculados ao ID do seu apartamento/bloco.
- A consulta da portaria processa e limpa a string da placa (remove hifens e espaços) e realiza uma busca por aproximação (case-insensitive) para agilizar a entrada na cancela.

## 3. Arquitetura e Padrões
- **Clean Architecture / Hexagonal:** O sistema isola as regras de negócio em casos de uso (Use Cases), mantendo a camada de Domínio livre de dependências de Framework (Spring) sempre que possível.
- **Segurança (LGPD):** Consultas da portaria retornam apenas o necessário para a operação (Nome, Bloco, Apartamento). Dados sensíveis (como telefone e e-mail) não transitam na resposta do Porteiro.
