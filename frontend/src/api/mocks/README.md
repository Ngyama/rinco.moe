# Mock data for local frontend development when the Spring Boot API / PostgreSQL
# is unavailable. Served only via `apiGet` fallback (or force mode).
#
# Enable / disable: see `frontend/.env.development` → `VITE_USE_MOCK_DATA`
#   true   — try real API first; on failure return mocks (default in DEV)
#   force  — always use mocks (no network wait)
#   false  — never use mocks
#
# Entry: `src/api/mocks/index.ts` (path → payload)
# Catalog / builders: `catalog.ts`, `builders.ts`
