#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parent.parent
template = root / 'gateway' / 'kong' / 'kong.template.yml'
output = root / 'gateway' / 'kong' / 'kong.yml'
public_key = root / 'platform' / 'certs' / 'iam-public.pem'
ca_cert = root / 'platform' / 'certs' / 'ca.crt'

for p in (template, public_key, ca_cert):
    if not p.exists():
        raise SystemExit(f'Falta {p}')

text = template.read_text(encoding='utf-8')
pub_lines = public_key.read_text(encoding='utf-8').splitlines()
ca_lines = ca_cert.read_text(encoding='utf-8').splitlines()
if '__IAM_PUBLIC_KEY_PEM__' not in text or '__INTERNAL_CA_PEM__' not in text:
    raise SystemExit('Faltan placeholders en kong.template.yml')
if '-----BEGIN PUBLIC KEY-----' not in pub_lines:
    raise SystemExit('iam-public.pem invalido')
if '-----BEGIN CERTIFICATE-----' not in ca_lines:
    raise SystemExit('ca.crt invalido')

text = text.replace('__IAM_PUBLIC_KEY_PEM__', '\n'.join('      ' + line for line in pub_lines))
text = text.replace('__INTERNAL_CA_PEM__', '\n'.join('    ' + line for line in ca_lines))
output.write_text(text, encoding='utf-8')
print('Kong declarative config rendered: JWT RSA + trusted internal CA for HTTPS upstreams.')
