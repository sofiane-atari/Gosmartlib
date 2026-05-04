# Lesson Tip components overview

Korte uitleg per component binnen de lesson tip feature.

---

## `lesson-tip-overview`

**Main functie:** overzichtspagina van alle lestips.

**Verantwoordelijkheden:**

- lijst van lestips ophalen
- lestips tonen in cards
- lege/loading states tonen
- navigeren naar detail of create

---

## `lesson-tip-card`

**Main functie:** 1 lestip visueel voorstellen in de overview.

**Verantwoordelijkheden:**

- titel, beschrijving en metadata tonen
- gekoppelde boeken previewen
- auteur/rol info tonen
- actie "bekijken" triggeren

---

## `lesson-tip-detail`

**Main functie:** detailpagina van 1 lestip.

**Verantwoordelijkheden:**

- volledige lestip tonen
- metadata, trefwoorden en gekoppelde boeken tonen
- creator info tonen
- bewerken/verwijderen mogelijk maken indien toegestaan
- attachments panel tonen

---

## `lesson-tip-form-page`

**Main functie:** pagina-orchestratie voor create en edit.

**Verantwoordelijkheden:**

- bepalen of pagina in create of edit mode zit
- bestaand materiaal laden bij edit
- save/update flow afhandelen
- linked books state koppelen aan save request
- attachment queue en attachment panel samenbrengen

---

## `lesson-tip-form`

**Main functie:** formulier met de basisgegevens van een lestip.

**Verantwoordelijkheden:**

- titel, beschrijving, type, vak, niveau en trefwoorden beheren
- validatie van formvelden
- formdata uitsturen naar parent component

---

## `lesson-tip-attachments-panel`

**Main functie:** beheer van opgeslagen bijlagen van een bestaande lestip.

**Verantwoordelijkheden:**

- bestaande attachments ophalen
- attachments tonen in lijst
- uploaden van nieuwe bestanden
- downloaden van bestanden
- verwijderen van bestanden indien toegestaan

---

## `lesson-tip-attachment-queue`

**Main functie:** tijdelijke wachtrij voor bestanden tijdens create flow.

**Verantwoordelijkheden:**

- bestanden selecteren via shared uploader
- bestanden lokaal in wachtrij houden
- bestanden verwijderen uit wachtrij
- queued files doorgeven aan parent component

---

## `lesson-tip-book-selection-panel`

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

- `lesson-tip-overview`
- `lesson-tip-detail`
- `lesson-tip-form-page`

## Presentational components

Deze components tonen vooral UI en krijgen data via inputs:

- `lesson-tip-card`
- `lesson-tip-form`
- `lesson-tip-book-selection-panel`
- `lesson-tip-attachment-queue`
- `lesson-tip-attachments-panel`

## Shared component

- `shared/file-upload`

---

# Belangrijke flow

## Create

- `lesson-tip-form-page`
- `lesson-tip-form`
- `lesson-tip-book-selection-panel`
- `lesson-tip-attachment-queue`

## Edit

- `lesson-tip-form-page`
- `lesson-tip-form`
- `lesson-tip-book-selection-panel`
- `lesson-tip-attachments-panel`

## Detail

- `lesson-tip-detail`
- `lesson-tip-attachments-panel`
