# Supabase Storage — comprovantes (fotos/vídeos)

Projeto: **jimdiiwbybfhazxeyusk**  
Dashboard: https://supabase.com/dashboard/project/jimdiiwbybfhazxeyusk

Storage **gratuito** (plano Free, sem cartão) para anexos das reclamações de falta de água.

Os **dados textuais** continuam no Firebase Firestore. Apenas mídia vai para o Supabase.

## Status

| Item | Status |
|------|--------|
| `SupabaseConfig.kt` | ✅ Preenchido |
| Bucket + políticas SQL | ⚠️ Executar `storage-setup.sql` uma vez |

## Criar bucket (passo único)

1. Abra o SQL Editor: https://supabase.com/dashboard/project/jimdiiwbybfhazxeyusk/sql/new
2. Cole o conteúdo de `supabase/storage-setup.sql`
3. Clique em **Run**

## Testar upload

```bash
curl -X POST "https://jimdiiwbybfhazxeyusk.supabase.co/storage/v1/object/reclamacoes-agua/reclamacoes/teste/foto.jpg" \
  -H "Authorization: Bearer SUA_CHAVE" \
  -H "apikey: SUA_CHAVE" \
  -H "Content-Type: image/jpeg" \
  --data-binary @foto.jpg
```

URL pública:
`https://jimdiiwbybfhazxeyusk.supabase.co/storage/v1/object/public/reclamacoes-agua/reclamacoes/teste/foto.jpg`

## Limites do plano Free

| Recurso | Limite |
|---------|--------|
| Storage total | 1 GB |
| Tamanho máximo por arquivo | 50 MB |
| Banda | 5 GB/mês |

## Arquitetura

```
App KMP
  ├─ Supabase Storage  → foto/vídeo (URL pública)
  └─ Firebase Firestore  → endereço, setor, dias, mediaUrl
```
