# Mais Náutico — API Server

API de notícias do Náutico Capibaribe para o app Android.
Agrega GE, NE10, Gazeta Esportiva, Folha PE e Google News em um único endpoint JSON.

## Endpoint

```
GET https://SEU-APP.onrender.com/noticias
GET https://SEU-APP.onrender.com/noticias?limit=30
GET https://SEU-APP.onrender.com/health
```

## Resposta JSON

```json
[
  {
    "title": "Náutico vence e se aproxima do G4",
    "link": "https://ge.globo.com/pe/futebol/times/nautico/noticia/...",
    "description": "Com gol de Danilo Boza no segundo tempo...",
    "image": "https://s2-ge.glbimg.com/...",
    "date": "2026-07-14T21:00:00.000Z",
    "source": "Globo Esporte",
    "color": "#C8102E"
  }
]
```

---

## Deploy no Render.com (passo a passo)

### Pré-requisitos
- Conta no [GitHub](https://github.com) (gratuita)
- Conta no [Render.com](https://render.com) (gratuita)

---

### Passo 1 — Subir o código no GitHub

1. Acesse [github.com](https://github.com) e faça login
2. Clique em **"New repository"** (botão verde no canto superior direito)
3. Nome: `maisfluminense-api`
4. Deixe **Public** marcado
5. Clique em **"Create repository"**
6. Na próxima tela, copie a URL do repositório (ex: `https://github.com/marcelaireismarcos/-maisfluminense-api.git`)

Agora, no seu computador, abra o terminal na pasta `server` deste projeto:

```bash
cd d:\Android\Noticias\MaisFluminense\server
git init
git config --global user.email "marcelaireismarcos@gmail.com"
git config --global user.name "marcelaireismarcos"
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/marcelaireismarcos/-maisfluminense-api.git
git push -u origin main
```

---

### Passo 2 — Criar o serviço no Render

1. Acesse [render.com](https://render.com) e faça login
2. Clique em **"New +"** → **"Web Service"**
3. Clique em **"Connect account"** para conectar com seu GitHub
4. Selecione o repositório `maisfluminense-api`
5. Configure:
   - **Name:** `maisfluminense-api`
   - **Region:** `Oregon (US West)` (ou o mais próximo do Brasil disponível no plano free)
   - **Branch:** `main`
   - **Build Command:** `npm install`
   - **Start Command:** `npm start`
   - **Plan:** `Free`
6. Clique em **"Create Web Service"**

O Render vai fazer o build automaticamente. Aguarde 2-3 minutos.

---

### Passo 3 — Pegar a URL do servidor

Após o deploy, o Render mostra a URL do seu serviço:
```
https://maisfluminense-api.onrender.com
```

Teste no browser:
```
https://maisfluminense-api.onrender.com/health
```

Deve retornar: `{"status":"ok","timestamp":"..."}`

---

### Passo 4 — Atualizar o app Android

No Android Studio, abra o `strings.xml` e adicione:

```xml
<string name="api_noticias_url">https://maisfluminense-api.onrender.com</string>
```

O app já está configurado para usar essa URL automaticamente.

---

## Atualizações futuras

Para atualizar o servidor (adicionar fontes, corrigir bugs):
1. Edite os arquivos localmente
2. `git add . && git commit -m "descrição" && git push`
3. O Render faz o redeploy automático em ~1 minuto

## Enquetes da Torcida — persistência no MySQL (multi-app)

As enquetes são armazenadas no **seu próprio banco MySQL** (Umbler), não no
filesystem do Render (que é efêmero no plano free e apagava os votos a cada
restart/redeploy). O histórico de enquetes encerradas também fica salvo no banco.

### Multi-app: um banco para vários apps

O mesmo banco serve **vários aplicativos**, cada um com suas enquetes isoladas:

- Tabela **`apps`**: um registro por app (`slug` único, ex.: `maisfluminense`).
- Coluna **`app_id`** em `enquetes`: isola as enquetes de cada app.

Para escolher o app em qualquer endpoint de enquete, use:

```
?app=meuapp           (query string)
X-App-Id: meuapp      (header HTTP)
```

Se o app não existir ainda, ele é **registrado automaticamente** no primeiro
uso. Sem parâmetro, assume `maisfluminense` — ou seja, o app Android atual
continua funcionando **sem nenhuma mudança**.

Variáveis de ambiente (valores padrão apontam para a conexão Umbler já usada
na pasta `php/`):

| Variável | Default | Descrição |
|---|---|---|
| `DB_HOST` | `mysql741.umbler.com` | Host do MySQL |
| `DB_PORT` | `3306` | Porta do MySQL |
| `DB_USER` | `vikkynsnorth` | Usuário |
| `DB_PASS` | `yUZu4Q*6.t` | Senha |
| `DB_NAME` | `vikkynsnorth` | Banco de dados |

Na primeira execução, as tabelas `apps`, `enquetes` e `enquetes_opcoes` são
criadas automaticamente, o app padrão `maisfluminense` é registrado e os dados
do `polls.json` antigo são migrados (se existirem).

### Opcional — criar as tabelas manualmente

Se preferir deixar o banco pronto antes do deploy (sem depender da criação
automática), rode o script **`php/enquetes_tabelas.sql`** no phpMyAdmin da
Umbler (aba SQL). Ele é idempotente (`IF NOT EXISTS` + migração da coluna
`app_id`), então pode ser executado quantas vezes quiser sem apagar dados.

### Endpoints de enquete

```
GET  /enquetes/ativa        — enquete ativa do app (usado pelo app)
POST /enquetes/votar        — registrar voto { pollId, optionId }
POST /enquetes/restaurar-voto — re-enviar voto após restart
GET  /enquetes/todas        — todas do app (ativas + encerradas) com resultados
GET  /enquetes/:id          — uma enquete específica do app com resultados
POST /enquetes/criar        — criar { question, options[], active? }
POST /enquetes/ativar       — ativar { pollId } (reinicia as 30h)
POST /enquetes/encerrar     — encerrar { pollId }
POST /enquetes/reset        — zerar votos da enquete ativa do app
```

Exemplo multi-app: `GET /enquetes/ativa?app=maisnautico` retorna a enquete
ativa do app "Mais Náutico", totalmente separada do "Mais Fluminense".

## Importante — Plano Gratuito do Render

O plano gratuito "hiberna" o servidor após 15 minutos sem uso.
A primeira requisição após hibernação demora ~30 segundos para "acordar".
As requisições seguintes são instantâneas.

Para evitar hibernação, você pode usar um serviço gratuito como
[UptimeRobot](https://uptimerobot.com) para pingar `/health` a cada 14 minutos.
