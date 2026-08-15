-- Supabase Storage: bucket público para comprovantes de falta de água
-- Execute no SQL Editor: https://supabase.com/dashboard/project/_/sql

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'reclamacoes-agua',
  'reclamacoes-agua',
  true,
  52428800,
  array['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'video/mp4', 'video/quicktime', 'video/webm']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Leitura pública de comprovantes" on storage.objects;
drop policy if exists "Upload anônimo de comprovantes" on storage.objects;
drop policy if exists "Sem update de comprovantes" on storage.objects;
drop policy if exists "Sem delete de comprovantes" on storage.objects;

create policy "Leitura pública de comprovantes"
on storage.objects for select
using (bucket_id = 'reclamacoes-agua');

create policy "Upload anônimo de comprovantes"
on storage.objects for insert
with check (
  bucket_id = 'reclamacoes-agua'
  and (storage.foldername(name))[1] = 'reclamacoes'
);

create policy "Sem update de comprovantes"
on storage.objects for update
using (false);

create policy "Sem delete de comprovantes"
on storage.objects for delete
using (false);
