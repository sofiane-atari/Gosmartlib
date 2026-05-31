# GoSmartLib

GoSmartLib is een modern bibliotheeksysteem voor scholen. Het platform biedt een volledige boekenbeheer- en uitleenworkflow voor leerlingen, leerkrachten, bibliothecarissen en beheerders. De applicatie bestaat uit een **Angular** frontend en een **Spring Boot** backend, en maakt gebruik van **MySQL** als database. GoSmartLib integreert met **Smartschool** via OAuth2 en synchroniseert gebruikers en klassen via de **OneRoster API**. Daarnaast ondersteunt het gamification, boekreviews, automatische herinneringsmails en een volledig rollen- en rechtenbeheer.

## Technologieën

| Laag | Technologie |
|------|-------------|
| Frontend | Angular 18 |
| Backend | Spring Boot 3.5 (Java 21) |
| Database | MySQL 8.0 |
| Reverse Proxy | Traefik |
| Containerisatie | Docker & Docker Compose |

---

## Deployen op een Linux-server (stappenplan)

### Vereisten

- Een Linux-server (bijv. Ubuntu 22.04 LTS)
- Docker en Docker Compose geïnstalleerd
- Een domeinnaam met DNS-records die naar je server verwijzen
- Traefik draait in een aparte Docker-netwerk `proxy` (aangemaakt via `docker network create proxy`)
- SSL-certificaten worden automatisch beheerd door Traefik

### Stap 1: Repository klonen

```bash
cd /opt
git clone <repository-url> gosmartlib
cd gosmartlib
```

### Stap 2: Environment-variabelen configureren

Kopieer het bestand `.env` en pas het aan:

```bash
cp .env .env.local
nano .env.local
```

Vul alle verplichte variabelen in (zie tabel hieronder).

### Stap 3: Docker-netwerk controleren

Zorg dat het `proxy`-netwerk bestaat voor Traefik:

```bash
docker network create proxy 2>/dev/null || true
```

### Stap 4: Containers opstarten

```bash
docker compose up -d
```

Dit bouwt en start de volgende services:
- **frontend**: Angular SPA geserveerd door Nginx
- **backend**: Spring Boot applicatie
- **mysql**: MySQL 8.0 database

### Stap 5: Controleer of alles draait

```bash
docker compose ps
docker compose logs -f backend
```

De backend is bereikbaar op `https://<jouw-domein>/api` en de frontend op `https://<jouw-domein>/`.

### Stap 6: Eerste school aanmaken (seed)

Bij de eerste start maakt de backend automatisch een school aan als de `SCHOOL_SEED_*` variabelen zijn ingevuld. Na de start kan je inloggen via Smartschool OAuth2.

---

## Environment-variabelen

Hieronder vind je een overzicht van alle environment-variabelen die je moet configureren in het `.env`-bestand.

> **Legenda:** `R` = Required (verplicht), `O` = Optional (optioneel), `D` = Default waarde.

### Basis & Domein

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `DOMAIN` | Het publieke domein waarop de app bereikbaar is (bv. `gosmartlib.be`). Wordt door Traefik gebruikt voor routing. | **R** | — | — |
| `BASE_URL` | De volledige basis-URL van de backend inclusief protocol (bv. `https://gosmartlib.be`). | **R** | — | — |
| `FRONTEND_URL` | De URL van de frontend (bv. `https://gosmartlib.be` of `http://localhost:4200`). | **R** | — | — |
| `FRONTEND_REDIRECT_URL` | De URL waar gebruikers naartoe worden doorverwezen na OAuth2-login (bv. `https://gosmartlib.be/oauth2/callback/smartschool`). | **R** | — | Afhankelijk van `FRONTEND_URL` |
| `SPRING_PORT` | De interne poort waarop de Spring Boot-app draait. | O | `8080` | — |
| `APP_CORS_ALLOWED_ORIGINS` | Komma-gescheiden lijst van toegestane CORS-origins. | **R** | — | Afhankelijk van `FRONTEND_URL` |

### Database

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `DB_HOST` | Hostnaam of container-naam van de MySQL-database. | **R** | — | — |
| `DB_PORT` | Poort van de MySQL-database. | O | `3306` | — |
| `DB_NAME` | Naam van de database. | **R** | — | — |
| `DB_USER` | Gebruikersnaam voor de database. | **R** | — | — |
| `DB_USER_PASS` | Wachtwoord voor de databasegebruiker. | **R** | — | — |
| `DB_ROOT_PASS` | Root-wachtwoord voor MySQL. | **R** | — | — |

### Beveiliging & JWT

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `JWT_SECRET` | Geheime sleutel voor het ondertekenen van JWT-tokens. Minimaal 256 bits. | **R** | — | — |
| `JASYPT_ENCRYPTOR_PASSWORD` | Wachtwoord voor Jasypt-encryptie (indien versleutelde waarden worden gebruikt). | **R** | — | — |

### Smartschool OAuth2

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `SMART_SCHOOL_CLIENT_ID` | Client ID van de Smartschool OAuth2-app. | **R** | — | — |
| `SMART_SCHOOL_CLIENT_SECRET` | Client Secret van de Smartschool OAuth2-app. | **R** | — | — |

### OneRoster Synchronisatie

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `ONE_ROSTER_SYNC_CRON` | Cron-expressie voor het synchronisatieschema. | O | `0 0 3 * * *` | — |
| `ONE_ROSTER_SYNC_ZONE` | Tijdzone voor de synchronisatie. | O | `Europe/Brussels` | — |
| `ONE_ROSTER_SYNC_INTERVAL_DAYS` | Aantal dagen tussen synchronisaties. | O | `7` | — |

### School Seed (optioneel)

Deze variabelen worden gebruikt om bij de eerste start automatisch één school met een campus aan te maken. Als ze leeg blijven, moet je de school handmatig configureren.

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `SCHOOL_SEED_DOMAIN` | Smartschool-domein van de school. | O | *(leeg)* | — |
| `SCHOOL_SEED_NAME` | Naam van de school. | O | *(leeg)* | — |
| `SCHOOL_SEED_CAMPUS_NAME` | Naam van de campus. | O | *(leeg)* | — |
| `SCHOOL_SEED_CAMPUS_ADDRESS` | Adres van de campus. | O | *(leeg)* | — |
| `SCHOOL_SEED_CAMPUS_POSTAL_CODE` | Postcode van de campus. | O | `0` | — |
| `SCHOOL_SEED_CAMPUS_CONTACT_PHONE` | Telefoonnummer van de campus. | O | *(leeg)* | — |
| `SCHOOL_SEED_CAMPUS_CONTACT_EMAIL` | E-mailadres van de campus. | O | *(leeg)* | — |
| `SCHOOL_SEED_ONEROSTER_CLIENT_ID` | OneRoster Client ID voor deze school. | O | *(leeg)* | Afhankelijk van `SCHOOL_SEED_DOMAIN` |
| `SCHOOL_SEED_ONEROSTER_CLIENT_SECRET` | OneRoster Client Secret voor deze school. | O | *(leeg)* | Afhankelijk van `SCHOOL_SEED_ONEROSTER_CLIENT_ID` |

### Integratie & API

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `BOOKS_API_KEY` | API-sleutel voor de Google Books API. | **R** | — | — |

### Uploadlimieten

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `MAX_UPLOAD_FILE_SIZE` | Maximale bestandsgrootte voor uploads. | O | `50MB` | — |
| `MAX_UPLOAD_REQUEST_SIZE` | Maximale totale request-grootte bij uploads. | O | `50MB` | — |

### Ontwikkeling & Beheer

> **Waarschuwing:** de volgende opties zijn bedoeld voor ontwikkeling of beheer. Zet `DEV_MODE_ENABLED` en `MOCK_MODE_ENABLED` op `false` in productie, tenzij je weet wat je doet.

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `DEV_MODE_ENABLED` | Schakelt ontwikkelingsfuncties in (bv. admin override, purge). | O | `false` | — |
| `MOCK_MODE_ENABLED` | Schakelt mock- endpoints en mock-schools in voor testen. | O | `false` | — |
| `MOCK_BASE_URL` | Basis-URL van de mock Smartschool-server (alleen nodig als `MOCK_MODE_ENABLED=true`). | O | *(leeg)* | Vereist als `MOCK_MODE_ENABLED=true` |
| `REAL_SCHOOL_DOMAINS` | Komma-gescheiden lijst van echte Smartschool-domeinen die in mock-mode naar de echte OAuth worden doorverwezen. | O | *(leeg)* | Alleen relevant als `MOCK_MODE_ENABLED=true` |
| `ADMIN_USERNAMES` | Komma-gescheiden lijst van admin-gebruikersnamen (1:1 mapping met `ADMIN_SCHOOL_DOMAINS`). | O | *(leeg)* | Vereist `ADMIN_SCHOOL_DOMAINS` |
| `ADMIN_SCHOOL_DOMAINS` | Komma-gescheiden lijst van Smartschool-domeinen die overeenkomen met `ADMIN_USERNAMES`. | O | *(leeg)* | Vereist `ADMIN_USERNAMES` |

### Soft Deletion (Scholen)

| Variabele | Beschrijving | R/O | Standaard | Afhankelijkheid |
|-----------|--------------|-----|-----------|-----------------|
| `SCHOOL_DELETION_GRACE_PERIOD_DAYS` | Aantal dagen voordat een zacht verwijderde school permanent wordt verwijderd. | O | `30` | — |
| `SCHOOL_DELETION_CHECK_CRON` | Cron-expressie voor het controleren van te verwijderen scholen. | O | `0 0 2 * * *` | — |

---

## Overige informatie

- Alle services communiceren via het Docker-netwerk `proxy`.
- De backend draait met het Spring-profiel `docker` wanneer gestart via Docker Compose.
- De Angular frontend wordt gecompileerd naar statische bestanden en geserveerd door Nginx op poort `8080`.
- MySQL-data wordt bewaard in een Docker-volume `mysql-data`.

