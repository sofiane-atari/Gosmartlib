# Didactic components overview

Korte uitleg per component binnen de didactic feature.

---

## `didactic-material-overview`

**Main functie:** overzichtspagina van alle didactische materialen.

**Verantwoordelijkheden:**

- lijst van didactische materialen ophalen
- materialen tonen in cards
- lege/loading states tonen
- navigeren naar detail of create

---

## `didactic-material-card`

**Main functie:** 1 didactisch materiaal visueel voorstellen in de overview.

**Verantwoordelijkheden:**

- titel, beschrijving en metadata tonen
- gekoppelde boeken previewen
- auteur/rol info tonen
- actie "bekijken" triggeren

---

## `didactic-material-detail`

**Main functie:** detailpagina van 1 didactisch materiaal.

**Verantwoordelijkheden:**

- volledig materiaal tonen
- metadata, trefwoorden en gekoppelde boeken tonen
- creator info tonen
- bewerken/verwijderen mogelijk maken indien toegestaan
- attachments panel tonen

---

## `didactic-material-form-page`

**Main functie:** pagina-orchestratie voor create en edit.

**Verantwoordelijkheden:**

- bepalen of pagina in create of edit mode zit
- bestaand materiaal laden bij edit
- save/update flow afhandelen
- linked books state koppelen aan save request
- attachment queue en attachment panel samenbrengen

---

## `didactic-material-form`

**Main functie:** formulier met de basisgegevens van een didactisch materiaal.

**Verantwoordelijkheden:**

- titel, beschrijving, type, vak, niveau en trefwoorden beheren
- validatie van formvelden
- formdata uitsturen naar parent component

---

## `didactic-material-attachments-panel`

**Main functie:** beheer van opgeslagen bijlagen van een bestaand didactisch materiaal.

**Verantwoordelijkheden:**

- bestaande attachments ophalen
- attachments tonen in lijst
- uploaden van nieuwe bestanden
- downloaden van bestanden
- verwijderen van bestanden indien toegestaan

---

## `didactic-material-attachment-queue`

**Main functie:** tijdelijke wachtrij voor bestanden tijdens create flow.

**Verantwoordelijkheden:**

- bestanden selecteren via shared uploader
- bestanden lokaal in wachtrij houden
- bestanden verwijderen uit wachtrij
- queued files doorgeven aan parent component

---

## `didactic-material-book-selection-panel`

**Main functie:** UI voor zoeken en selecteren van gekoppelde boeken.

**Verantwoordelijkheden:**

- zoekvelden tonen
- zoekresultaten tonen
- geselecteerde status tonen
- selectie-event uitsturen naar parent

---

## `shared/file-upload`

**Main functie:** herbruikbare file uploader voor meerdere pagina’s.

**Verantwoordelijkheden:**

- drag & drop en klassieke file select
- filetype en grootte valideren
- optionele progressbar tonen
- geselecteerd bestand uitsturen

---

# Architectuur in het kort

## Page components

Deze components sturen de flow aan:

- `didactic-material-overview`
- `didactic-material-detail`
- `didactic-material-form-page`

## Presentational components

Deze components tonen vooral UI en krijgen data via inputs:

- `didactic-material-card`
- `didactic-material-form`
- `didactic-material-book-selection-panel`
- `didactic-material-attachment-queue`
- `didactic-material-attachments-panel`

## Shared component

- `shared/file-upload`

---

# Belangrijke flow

## Create

- `didactic-material-form-page`
- `didactic-material-form`
- `didactic-material-book-selection-panel`
- `didactic-material-attachment-queue`

## Edit

- `didactic-material-form-page`
- `didactic-material-form`
- `didactic-material-book-selection-panel`
- `didactic-material-attachments-panel`

## Detail

- `didactic-material-detail`
- `didactic-material-attachments-panel`
