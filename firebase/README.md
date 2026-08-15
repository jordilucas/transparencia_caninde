# Firebase — dados das reclamações

Projeto: **reclamacao-agua-caninde**  
Console: https://console.firebase.google.com/project/reclamacao-agua-caninde/overview

Guarda **endereço, setor, dias sem água** e a **URL do comprovante** (hospedado no Supabase).

## Status

| Serviço | Status |
|---------|--------|
| Firestore | ✅ Ativo |
| Auth anônimo | ✅ Ativo |
| Storage Firebase | ❌ Não usado (migrado para Supabase) |

## Mídia (fotos/vídeos)

Use **Supabase Storage** — ver [supabase/README.md](../supabase/README.md).

## Publicar regras Firestore

```bash
npx -y firebase-tools@latest deploy --only firestore:rules,auth --project reclamacao-agua-caninde
```

## Coleção

`reclamacoes/{id}` — campos: `endereco`, `setor`, `diasSemAgua`, `criadoEmMillis`, `mediaUrl`, `mediaType`
