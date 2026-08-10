import { useState } from "react";
import { api, ApiError } from "./api";
import type {
  CouponCreateRequest,
  CouponRedemptionRequest,
  CouponRedemptionResponse,
  CouponResponse
} from "./apiTypes";

type Mode = "check" | "redeem" | "create";
type SubmitAction = Mode | null;

type ResultState =
  | {
      kind: "coupon";
      title: string;
      coupon: CouponResponse;
    }
  | {
      kind: "redemption";
      title: string;
      redemption: CouponRedemptionResponse;
    };

const modeOptions: Array<{ id: Mode; label: string; menuLabel: string }> = [
  { id: "check", label: "Sprawdź", menuLabel: "sprawdź status i limit" },
  { id: "redeem", label: "Zrealizuj", menuLabel: "zrealizuj kupon" },
  { id: "create", label: "Utwórz", menuLabel: "utwórz nowy kupon" }
];

const modeCopy: Record<Mode, { title: string; description: string; button: string; busy: string }> = {
  check: {
    title: "Saldo i status kuponu rabatowego",
    description:
      "Jeśli posiadasz kod rabatowy, możesz sprawdzić jego limit wykorzystania i kraj obowiązywania.",
    button: "Sprawdź",
    busy: "Sprawdzanie..."
  },
  redeem: {
    title: "Realizacja kuponu rabatowego",
    description: "Podaj kod kuponu i identyfikator użytkownika, aby zarejestrować użycie.",
    button: "Zrealizuj",
    busy: "Realizacja..."
  },
  create: {
    title: "Nowy kupon rabatowy",
    description: "Podaj kod, limit wykorzystania i kraj, w którym kupon może zostać użyty.",
    button: "Utwórz",
    busy: "Tworzenie..."
  }
};

function messageFromError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "Wystąpił nieoczekiwany błąd interfejsu.";
}

function normalizeCode(value: string): string {
  return value.trim().toUpperCase();
}

function normalizeCountryCode(value: string): string {
  return value.replace(/[^a-z]/gi, "").slice(0, 2).toUpperCase();
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("pl-PL", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function usagePercent(currentUses: number, maxUses: number): number {
  if (maxUses <= 0) {
    return 0;
  }

  return Math.min(100, Math.round((currentUses / maxUses) * 100));
}

function DetailItem({ label, value }: Readonly<{ label: string; value: string | number }>) {
  return (
    <div className="detail-item">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function CouponUsage({
  currentUses,
  maxUses
}: Readonly<{ currentUses: number; maxUses: number }>) {
  const percent = usagePercent(currentUses, maxUses);
  const remainingUses = Math.max(maxUses - currentUses, 0);

  return (
    <div className="usage-block">
      <div className="d-flex justify-content-between align-items-baseline gap-3">
        <span className="text-secondary small">Wykorzystanie</span>
        <span className="fw-semibold">
          {currentUses}/{maxUses}
        </span>
      </div>
      <div
        className="progress usage-progress"
        aria-label={`Wykorzystano ${percent} procent limitu kuponu`}
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div className="progress-bar" style={{ width: `${percent}%` }} />
      </div>
      <div className="text-secondary small">Pozostało: {remainingUses}</div>
    </div>
  );
}

function ResultPanel({ result }: Readonly<{ result: ResultState | null }>) {
  if (!result) {
    return null;
  }

  const coupon =
    result.kind === "coupon"
      ? result.coupon
      : {
          code: result.redemption.code,
          currentUses: result.redemption.currentUses,
          maxUses: result.redemption.maxUses,
          countryCode: result.redemption.countryCode
        };

  return (
    <section className="result-panel" aria-live="polite">
      <h3>{result.title}</h3>
      <div className="result-summary">
        <div className="result-code-row">
          <span className="result-code">{coupon.code}</span>
          <span className="badge rounded-pill text-bg-success">aktywny</span>
        </div>

        <CouponUsage currentUses={coupon.currentUses} maxUses={coupon.maxUses} />

        <dl className="detail-grid">
          <DetailItem label="Kraj" value={coupon.countryCode} />
          {result.kind === "coupon" ? (
            <DetailItem label="Utworzono" value={formatDateTime(result.coupon.createdAt)} />
          ) : (
            <>
              <DetailItem label="Użytkownik" value={result.redemption.userId} />
              <DetailItem label="Użyto" value={formatDateTime(result.redemption.usedAt)} />
            </>
          )}
        </dl>
      </div>
    </section>
  );
}

export default function App() {
  const [mode, setMode] = useState<Mode>("check");
  const [checkCode, setCheckCode] = useState("");
  const [redeemCode, setRedeemCode] = useState("");
  const [userId, setUserId] = useState("");
  const [createCode, setCreateCode] = useState("");
  const [maxUses, setMaxUses] = useState("10");
  const [countryCode, setCountryCode] = useState("PL");
  const [submitAction, setSubmitAction] = useState<SubmitAction>(null);
  const [showColdStartHint, setShowColdStartHint] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [result, setResult] = useState<ResultState | null>(null);

  const isBusy = submitAction !== null;
  const isRenderBackend = api.baseUrl.includes("onrender.com");
  const currentCopy = modeCopy[mode];

  async function runBackendAction(action: Mode, callback: () => Promise<void>) {
    setSubmitAction(action);
    setShowColdStartHint(false);
    setErrorMessage(null);
    setSuccessMessage(null);

    const coldStartTimer = globalThis.setTimeout(() => {
      setShowColdStartHint(true);
    }, 7000);

    try {
      await callback();
    } catch (error) {
      setErrorMessage(messageFromError(error));
    } finally {
      globalThis.clearTimeout(coldStartTimer);
      setSubmitAction(null);
    }
  }

  async function handleCheck() {
    const code = normalizeCode(checkCode);

    if (!code) {
      setErrorMessage("Podaj kod kuponu.");
      return;
    }

    await runBackendAction("check", async () => {
      const coupon = await api.getCoupon(code);
      setCheckCode(coupon.code);
      setResult({ kind: "coupon", title: "Kupon znaleziony", coupon });
      setSuccessMessage(`Kupon ${coupon.code} jest dostępny.`);
    });
  }

  async function handleRedeem() {
    const code = normalizeCode(redeemCode);
    const trimmedUserId = userId.trim();

    if (!code || !trimmedUserId) {
      setErrorMessage("Podaj kod kuponu i identyfikator użytkownika.");
      return;
    }

    const command: CouponRedemptionRequest = {
      code,
      userId: trimmedUserId
    };

    await runBackendAction("redeem", async () => {
      const redemption = await api.redeemCoupon(command);
      setRedeemCode(redemption.code);
      setResult({ kind: "redemption", title: "Kupon zrealizowany", redemption });
      setSuccessMessage(`Kupon ${redemption.code} został zrealizowany.`);
    });
  }

  async function handleCreate() {
    const code = normalizeCode(createCode);
    const country = normalizeCountryCode(countryCode);
    const maxUsesValue = Number(maxUses);

    if (!code) {
      setErrorMessage("Podaj kod kuponu.");
      return;
    }

    if (!Number.isInteger(maxUsesValue) || maxUsesValue < 1) {
      setErrorMessage("Limit użyć musi być liczbą całkowitą większą od zera.");
      return;
    }

    if (country.length !== 2) {
      setErrorMessage("Kod kraju musi mieć dokładnie dwa znaki.");
      return;
    }

    const command: CouponCreateRequest = {
      code,
      maxUses: maxUsesValue,
      countryCode: country
    };

    await runBackendAction("create", async () => {
      const coupon = await api.createCoupon(command);
      setCreateCode(coupon.code);
      setCountryCode(coupon.countryCode);
      setCheckCode(coupon.code);
      setRedeemCode(coupon.code);
      setResult({ kind: "coupon", title: "Kupon utworzony", coupon });
      setSuccessMessage(`Kupon ${coupon.code} został utworzony.`);
    });
  }

  function renderActiveForm() {
    if (mode === "check") {
      return (
        <form
          className="form-stack"
          onSubmit={(event) => {
            event.preventDefault();
            void handleCheck();
          }}
        >
          <label className="visually-hidden" htmlFor="check-code">
            Kod kuponu
          </label>
          <input
            id="check-code"
            className="form-control coupon-input"
            type="text"
            value={checkCode}
            maxLength={64}
            placeholder="Kod kuponu"
            autoComplete="off"
            onChange={(event) => setCheckCode(event.target.value)}
          />
          <button className="btn primary-action" type="submit" disabled={isBusy}>
            {submitAction === "check" ? currentCopy.busy : currentCopy.button}
          </button>
        </form>
      );
    }

    if (mode === "redeem") {
      return (
        <form
          className="form-stack"
          onSubmit={(event) => {
            event.preventDefault();
            void handleRedeem();
          }}
        >
          <label className="visually-hidden" htmlFor="redeem-code">
            Kod kuponu
          </label>
          <input
            id="redeem-code"
            className="form-control coupon-input"
            type="text"
            value={redeemCode}
            maxLength={64}
            placeholder="Kod kuponu"
            autoComplete="off"
            onChange={(event) => setRedeemCode(event.target.value)}
          />
          <label className="visually-hidden" htmlFor="redeem-user">
            Identyfikator użytkownika
          </label>
          <input
            id="redeem-user"
            className="form-control coupon-input"
            type="text"
            value={userId}
            maxLength={128}
            placeholder="Identyfikator użytkownika"
            autoComplete="off"
            onChange={(event) => setUserId(event.target.value)}
          />
          <button className="btn primary-action" type="submit" disabled={isBusy}>
            {submitAction === "redeem" ? currentCopy.busy : currentCopy.button}
          </button>
        </form>
      );
    }

    return (
      <form
        className="form-stack"
        onSubmit={(event) => {
          event.preventDefault();
          void handleCreate();
        }}
      >
        <label className="visually-hidden" htmlFor="create-code">
          Kod kuponu
        </label>
        <input
          id="create-code"
          className="form-control coupon-input"
          type="text"
          value={createCode}
          maxLength={64}
          placeholder="Kod kuponu"
          autoComplete="off"
          onChange={(event) => setCreateCode(event.target.value)}
        />
        <label className="visually-hidden" htmlFor="create-max-uses">
          Limit użyć
        </label>
        <input
          id="create-max-uses"
          className="form-control coupon-input"
          type="number"
          min={1}
          step={1}
          value={maxUses}
          placeholder="Limit użyć"
          onChange={(event) => setMaxUses(event.target.value)}
        />
        <label className="visually-hidden" htmlFor="create-country">
          Kod kraju
        </label>
        <input
          id="create-country"
          className="form-control coupon-input"
          type="text"
          value={countryCode}
          maxLength={2}
          placeholder="Kod kraju"
          autoComplete="off"
          onChange={(event) => setCountryCode(normalizeCountryCode(event.target.value))}
        />
        <button className="btn primary-action" type="submit" disabled={isBusy}>
          {submitAction === "create" ? currentCopy.busy : currentCopy.button}
        </button>
      </form>
    );
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <nav className="header-main" aria-label="Główna nawigacja">
          <div className="container page-container">
            <a className="brand-link" href={import.meta.env.BASE_URL} aria-label="Kupony rabatowe">
              <img src={`${import.meta.env.BASE_URL}coupon-mark.svg`} alt="" className="brand-icon" />
              <span>kupony</span>
            </a>
            <div className="header-actions">
              <a href={`${api.baseUrl}/swagger-ui/index.html`} target="_blank" rel="noreferrer">
                Swagger UI
              </a>
            </div>
          </div>
        </nav>
        <div className="header-subnav">
          <div className="container page-container">
            <a className="subnav-link active" href={import.meta.env.BASE_URL}>
              Promocje
            </a>
            <span className="subnav-link">Kupony rabatowe</span>
            <span className="subnav-link">Kody dla klientów</span>
            <span className="subnav-link ms-lg-auto">Panel demo</span>
          </div>
        </div>
      </header>

      <main>
        <div className="page-title-row container page-container">
          <h1>Kupony rabatowe</h1>
        </div>

        <div className="container page-container coupon-layout">
          <aside className="coupon-sidebar" aria-label="Nawigacja kuponów">
            <h2>Kupony rabatowe</h2>
            {modeOptions.map((option) => (
              <button
                key={option.id}
                className={`sidebar-link ${mode === option.id ? "active" : ""}`}
                type="button"
                onClick={() => setMode(option.id)}
              >
                {option.menuLabel}
              </button>
            ))}
            <a className="sidebar-link" href={`${api.baseUrl}/v3/api-docs`} target="_blank" rel="noreferrer">
              kontrakt OpenAPI
            </a>
          </aside>

          <section className="coupon-main" aria-labelledby="coupon-action-title">
            <div className="mode-tabs" role="tablist" aria-label="Akcja kuponu">
              {modeOptions.map((option) => (
                <button
                  key={option.id}
                  className={`mode-tab ${mode === option.id ? "active" : ""}`}
                  type="button"
                  role="tab"
                  aria-selected={mode === option.id}
                  onClick={() => setMode(option.id)}
                >
                  {option.label}
                </button>
              ))}
            </div>

            <div className="react-box-container">
              <h2 id="coupon-action-title">{currentCopy.title}</h2>
              <p>{currentCopy.description}</p>

              {showColdStartHint && isRenderBackend && (
                <div className="alert alert-warning status-alert" role="status">
                  Render wybudza usługę po przerwie. Pierwsza odpowiedź może potrwać około minuty.
                </div>
              )}

              {errorMessage && (
                <div className="alert alert-danger status-alert" role="alert">
                  {errorMessage}
                </div>
              )}

              {successMessage && (
                <div className="alert alert-success status-alert" role="status">
                  {successMessage}
                </div>
              )}

              {renderActiveForm()}

              <ResultPanel result={result} />
            </div>
          </section>
        </div>
      </main>

      <footer className="api-footer">
        <div className="container page-container">
          <span className="api-badge">{isRenderBackend ? "Render" : "Local"}</span>
          <span>{api.baseUrl}</span>
        </div>
      </footer>
    </div>
  );
}
