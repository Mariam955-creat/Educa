# docs/assets — diagrammes exportés

Diagrammes du projet **educa**, sous forme de sources Mermaid (`.mmd`) et d'images générées
(`.svg` vectoriel pour le mémoire, `.png` pour un aperçu rapide / insertion Word).

| Sujet | Source | Images | Doc de référence |
|---|---|---|---|
| Architecture générale | `architecture.mmd` | `architecture.svg`, `architecture.png` | `../02-conception.md` §1 |
| Modèle de données (MCD, périmètre MVP implémenté) | `mcd.mmd` | `mcd.svg`, `mcd.png` | `../02-conception.md` §2–§3 |

> Le `mcd.mmd` reflète les **15 tables du MVP effectivement migrées** (Flyway `V1`/`V2`).
> Les entités de traduction de contenu (`*_translation`, `language`) et `chat_message` restent
> au stade conception (*Should have*, voir `../02-conception.md` §2) et ne sont pas dans le schéma.

## Régénérer les images

Prérequis : Node.js + un navigateur Chrome/Chromium installé.

```bash
# fichier de config pointant sur le Chrome du poste (une fois)
cat > puppeteer.json <<'JSON'
{ "executablePath": "C:/Program Files/Google/Chrome/Application/chrome.exe", "args": ["--no-sandbox"] }
JSON

export PUPPETEER_SKIP_DOWNLOAD=1
MMDC="npx -y @mermaid-js/mermaid-cli@11 -p puppeteer.json -t neutral"

$MMDC -i architecture.mmd -o architecture.svg -b transparent
$MMDC -i architecture.mmd -o architecture.png -b white -s 3
$MMDC -i mcd.mmd          -o mcd.svg          -b transparent
$MMDC -i mcd.mmd          -o mcd.png          -b white -s 3
```

Les sources `.mmd` se visualisent aussi directement sur GitHub, dans VS Code
(extension *Markdown Preview Mermaid*) ou sur <https://mermaid.live>.
