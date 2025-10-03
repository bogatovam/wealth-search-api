#!/usr/bin/env python3
"""
Populate the Wealth Search API with synthetic clients and documents.

The script creates a configurable number of clients. For each client it
attaches 1..N documents filled with pseudo-random content to simulate a
realistic dataset for local testing.

Example:
    python scripts/populate_fake_data.py         --host http://localhost:8080         --token demo-token         --clients 10000         --min-docs 1         --max-docs 7

The script requires the API to be running locally and accessible without TLS.
"""
from __future__ import annotations

import argparse
import json
import logging
import os
import random
import re
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

try:
    import requests
except ImportError as exc:  # pragma: no cover - handled at runtime only
    raise SystemExit(
        "The 'requests' library is required. Install it with 'pip install requests'."
    ) from exc

try:  # pragma: no cover - optional dependency
    from faker import Faker
except ImportError:  # pragma: no cover - optional dependency
    Faker = None


LOG = logging.getLogger("populate_fake_data")
LOG_PATH = Path(__file__).resolve().with_suffix(".log")


def setup_logging(level: int = logging.INFO) -> None:
    if LOG.handlers:
        return

    LOG.setLevel(logging.DEBUG)

    file_handler = logging.FileHandler(LOG_PATH, encoding="utf-8")
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(
        logging.Formatter(
            fmt="%(asctime)s [%(levelname)s] %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
    )

    console_handler = logging.StreamHandler()
    console_handler.setLevel(level)
    console_handler.setFormatter(logging.Formatter("%(message)s"))

    LOG.addHandler(file_handler)
    LOG.addHandler(console_handler)

    LOG.info("Logging initialised. File: %s", LOG_PATH)


@dataclass
class ClientPayload:
    first_name: str
    last_name: str
    email: str
    country_of_residence: str

    def to_json(self) -> dict:
        return {
            "firstName": self.first_name,
            "lastName": self.last_name,
            "email": self.email,
            "countryOfResidence": self.country_of_residence,
        }


@dataclass
class DocumentPayload:
    title: str
    content: str

    def to_json(self) -> dict:
        return {
            "title": self.title,
            "content": self.content,
        }


class OllamaDocumentGenerator:
    """Lightweight client for requesting document drafts from an Ollama model."""

    def __init__(self, base_url: str, model: str, timeout: int = 60) -> None:
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.timeout = timeout
        self.session = requests.Session()
        self._disabled = False

    def generate_document(self, context: dict) -> Optional[tuple[str, str]]:
        if self._disabled:
            return None
        prompt = self._build_prompt(context)
        try:
            response = self.session.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": prompt,
                    "format": "json",
                    "stream": False,
                    "options": {"temperature": 0.7},
                },
                timeout=self.timeout,
            )
            response.raise_for_status()
        except requests.RequestException as exc:  # pragma: no cover - runtime only
            LOG.warning("Ollama request failed: %s", exc)
#             self._disabled = True
            return None

        try:
            payload = response.json()
        except ValueError as exc:  # pragma: no cover - runtime only
            LOG.warning("Ollama invalid JSON response: %s", exc)
#             self._disabled = True
            return None

        text = (payload.get("response") or payload.get("text") or "").strip()
        if not text:
            LOG.warning("Ollama returned an empty response")
#             self._disabled = True
            return None

        match = re.search(r"\{[\s\S]*\}", text)
        if match:
            text = match.group(0)

        try:
            data = json.loads(text)
        except json.JSONDecodeError as exc:  # pragma: no cover - runtime only
            LOG.warning("Ollama JSON parsing failed: %s | payload=%s", exc, text)
#             self._disabled = True
            return None

        title = (data.get("title") or "").strip()
        content = (data.get("content") or "").strip()
        if not title or not content:
            LOG.warning("Ollama response missing title/content: %s", data)
#             self._disabled = True
            return None

        return title, content

    def _build_prompt(self, ctx: dict) -> str:
        actions = "; ".join(ctx["actions_list"])
        country = ctx.get("client_country") or "N/A"
        residency_year = ctx.get("residency_year") or ctx.get("adoption_rate")
        return (
            f"Generate a banking document summary with:\n"
            f"- Client: {ctx['client_name']} from {country}, resident since {residency_year}\n"
            f"- Document: {ctx['doc_type']} for {ctx['theme']}\n"
            f"- Period: {ctx['quarter']} {ctx['year']}\n"
            f"- Regulation: {ctx['regulation']}\n"
            f"- Financial: {ctx['kpi_label']} {ctx['kpi_value']:.2f}, income {ctx['income_amount']:.2f} {ctx['income_currency']}\n"
            f"- Actions: {actions}\n\n"
            f"Create JSON with two fields:\n"
            f"1. title: Short document title (max 80 chars)\n"
            f"2. content: Three paragraphs separated by \\n\\n:\n"
            f"   - Paragraph 1: Document description, identity fields, residency info\n"
            f"   - Paragraph 2: Compliance checks and regulatory requirements\n"
            f"   - Paragraph 3: Next steps and follow-up actions"
        )


class FakeDataFactory:
    """Generate pseudo-random but consistent-looking client and document data."""

    FIRST_NAMES = [
        "Ivan", "Mira", "Sofia", "Noah", "Lucas", "Elena", "Caleb", "Iris",
        "Mateo", "Lina", "Jonas", "Priya", "Levi", "Nina", "Ravi", "Sage",
    ]
    LAST_NAMES = [
        "Ivanov", "Nguyen", "Hernandez", "Patel", "Okafor", "Silva",
        "Kowalski", "Ibrahim", "Williams", "Chen", "Miller", "Garcia",
        "D'Souza", "Novak", "Iversen", "Yamamoto",
    ]
    COUNTRIES = [
        "US", "UK", "CA", "DE", "FR", "CH", "SG", "AE", "AU", "BR", "ZA",
    ]
    PERSONAL_DOMAINS = [
        "gmail.com",
        "outlook.com",
        "yahoo.com",
        "icloud.com",
        "mail.ru",
        "yandex.ru",
        "gmx.com",
        "proton.me",
        "zoho.com",
    ]
    COMPANY_DOMAINS = [
        "hsbc.com",
        "citi.com",
        "bankofamerica.com",
        "revolut.com",
        "monzo.com",
        "stripe.com",
        "wise.com",
        "klarna.com",
        "paypal.com",
        "squareup.com",
        "visa.com",
        "mastercard.com",
        "neviswealth.com",
    ]
    DOCUMENT_TYPES = [
        "Passport Scan",
        "Residence Permit",
        "Income Statement",
        "Tax Declaration",
        "Bank Statement",
        "Employment Contract",
        "Proof of Address",
        "Utility Bill Receipt",
        "Investment Portfolio Snapshot",
        "Salary Slip",
    ]
    DOCUMENT_TOPICS = [
        "identity verification",
        "residency confirmation",
        "source of funds",
        "wealth assessment",
        "AML screening",
        "transaction monitoring",
        "credit underwriting",
        "loan servicing",
        "KYC refresh",
        "PEP clearance",
    ]
    SUPPORTING_ITEMS = [
        "passport number",
        "national ID",
        "residence permit number",
        "utility account reference",
        "IBAN",
        "SWIFT BIC",
        "tax identification number",
        "employer reference",
        "rental contract",
        "health insurance certificate",
    ]
    REGULATIONS = [
        "EU AMLD6",
        "FATF Travel Rule",
        "GDPR",
        "FCA KYC Handbook",
        "MAS Notice 626",
        "FinCEN CDD Rule",
        "BaFin GwG",
        "HKMA AML Guideline",
        "ASIC RG 97",
        "FINTRAC PCMLTFA",
    ]
    FINANCIAL_METRICS = [
        "monthly net income",
        "average balance",
        "cash inflow ratio",
        "card spending",
        "savings rate",
        "loan exposure",
        "mortgage balance",
        "investment contributions",
        "remittance volume",
        "FX turnover",
    ]
    ACTION_ITEMS = [
        "archive notarised copy in secure vault",
        "schedule in-person verification",
        "update customer risk profile",
        "notify compliance review queue",
        "refresh sanctions screening",
        "recalculate affordability metrics",
        "share summary with relationship manager",
        "request additional supporting invoices",
        "trigger electronic signature workflow",
        "confirm address change with postal service",
    ]

    def __init__(self, seed: Optional[int] = None, ollama: Optional[OllamaDocumentGenerator] = None) -> None:
        self.random = random.Random(seed)
        self.ollama = ollama
        if Faker is not None:
            self.faker: Optional[Faker] = Faker()
            if seed is not None:
                self.faker.seed_instance(seed)
        else:
            self.faker = None

    def client(self, index: int) -> ClientPayload:
        first_name = self._first_name()
        last_name = self._last_name()
        domain = self._email_domain(index)
        email = f"{first_name}.{last_name}{index}@{domain}".lower()
        country = self._country()
        return ClientPayload(
            first_name=first_name,
            last_name=last_name,
            email=email,
            country_of_residence=country,
        )

    def document(self, client: ClientPayload, client_index: int, doc_index: int) -> DocumentPayload:
        context = self._build_fintech_context(client, client_index, doc_index)

        if self.ollama:
            generated = self.ollama.generate_document(context)
            if generated:
                title, content = generated
                LOG.debug(
                    "Ollama generated document for %s (ref=%s)",
                    client.email,
                    context["reference_id"],
                )
                return DocumentPayload(title=title, content=content)
            LOG.debug(
                "Falling back to template document for %s (ref=%s)",
                client.email,
                context["reference_id"],
            )

        return DocumentPayload(
            title=context["fallback_title"],
            content=context["fallback_content"],
        )

    def _first_name(self) -> str:
        if self.faker:
            return self.faker.first_name()
        return self.random.choice(self.FIRST_NAMES)

    def _last_name(self) -> str:
        if self.faker:
            return self.faker.last_name()
        return self.random.choice(self.LAST_NAMES)

    def _country(self) -> str:
        if self.faker:
            return self.faker.country_code(representation="alpha-2")
        return self.random.choice(self.COUNTRIES)

    def _email_domain(self, index: int) -> str:
        use_personal = self.random.random() < 0.65
        if use_personal:
            return self.random.choice(self.PERSONAL_DOMAINS)

        domain = self.random.choice(self.COMPANY_DOMAINS)
        if self.faker:
            return domain

        # Introduce minor variation to keep addresses realistic but unique.
        if self.random.random() < 0.3:
            name, _, suffix = domain.partition(".")
            return f"{name}{index % 100}.{suffix}"
        return domain

    def _build_fintech_context(
        self,
        client: ClientPayload,
        client_index: int,
        doc_index: int,
    ) -> dict:
        client_name = f"{client.first_name} {client.last_name}"
        doc_type = self.random.choice(self.DOCUMENT_TYPES)
        topic = self.random.choice(self.DOCUMENT_TOPICS)
        regulation = self.random.choice(self.REGULATIONS)
        kpi_label = self.random.choice(self.FINANCIAL_METRICS)
        kpi_value = round(self.random.uniform(1.5, 95.0), 2)
        residency_year = self.random.randint(2010, int(time.strftime("%Y")))
        actions_list = self.random.sample(self.ACTION_ITEMS, 3)
        actions_sentence = ", ".join(actions_list)
        supporting = ", ".join(self.random.sample(self.SUPPORTING_ITEMS, 2))
        quarter = self.random.choice(["Q1", "Q2", "Q3", "Q4"])
        year = time.strftime("%Y")
        income_currency = self.random.choice(["USD", "EUR", "GBP", "CHF", "SGD", "AED"])
        income_amount = round(self.random.uniform(4500, 27500), 2)

        title = f"{doc_type} – {client_name}"
        paragraphs = [
            (
                f"{doc_type.lower()} uploaded for {client_name} as part of the {topic} review. "
                f"Residence registered since {residency_year} with supporting evidence ({supporting})."
            ),
            (
                f"Compliance context: controls assessed against {regulation}. "
                f"Declared {kpi_label} measured at {kpi_value:.2f} with monthly income {income_amount:,.2f} {income_currency}. "
                "All personal identifiers validated against passport and national registry data."
            ),
            (
                f"Next steps: {actions_sentence}. "
                "Client will be notified once verification and archival steps are complete."
            ),
            (
                f"Reference ID {client_index}-{doc_index} | Generated {time.strftime('%Y-%m-%d %H:%M:%S')}"
            ),
        ]
        content = "\n\n".join(paragraphs)
        return {
            "client_name": client_name,
            "client_country": client.country_of_residence,
            "theme": topic,
            "doc_type": doc_type,
            "regulation": regulation,
            "kpi_label": kpi_label,
            "kpi_value": kpi_value,
            "adoption_rate": residency_year,
            "residency_year": residency_year,
            "quarter": quarter,
            "year": year,
            "actions_list": actions_list,
            "reference_id": f"{client_index}-{doc_index}",
            "income_amount": income_amount,
            "income_currency": income_currency,
            "fallback_title": title,
            "fallback_content": content,
        }


def populate(
    api_host: str,
    token: str,
    client_count: int,
    documents_min: int,
    documents_max: int,
    dry_run: bool,
    seed: Optional[int],
    use_ollama: bool,
    ollama_base_url: str,
    ollama_model: str,
) -> None:
    if documents_min < 0 or documents_max < 0:
        raise ValueError("Document counts must be non-negative")
    if documents_min > documents_max:
        raise ValueError("--min-docs cannot exceed --max-docs")

    ollama_generator: Optional[OllamaDocumentGenerator] = None
    LOG.info(
        "Starting population run: clients=%s min_docs=%s max_docs=%s dry_run=%s use_ollama=%s",
        client_count,
        documents_min,
        documents_max,
        dry_run,
        use_ollama,
    )

    if use_ollama and ollama_base_url:
        try:
            ollama_generator = OllamaDocumentGenerator(
                base_url=ollama_base_url,
                model=ollama_model,
            )
        except Exception as exc:  # pragma: no cover - runtime convenience
            LOG.warning(
                "Failed to initialise Ollama client (%s). Falling back to template-based documents.",
                exc,
            )

    generator = FakeDataFactory(seed=seed, ollama=ollama_generator)
    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    })

    created_clients = 0
    created_documents = 0
    start = time.time()

    for idx in range(1, client_count + 1):
        client_payload = generator.client(idx)
        docs_to_create = generator.random.randint(documents_min, documents_max) if documents_max > 0 else 0
        LOG.debug(
            "Prepared client payload idx=%s email=%s documents=%s",
            idx,
            client_payload.email,
            docs_to_create,
        )

        if dry_run:
            created_clients += 1
            created_documents += docs_to_create
            continue

        client_response = session.post(
            f"{api_host}/clients",
            json=client_payload.to_json(),
            timeout=30,
        )
        if client_response.status_code >= 400:
            raise RuntimeError(
                f"Client creation failed (status {client_response.status_code}): {client_response.text}"
            )
        client_body = client_response.json()
        client_id = client_body.get("id") or client_body.get("client_id")
        if not client_id:
            raise RuntimeError(
                f"Client creation succeeded but response lacks 'id': {json.dumps(client_body)}"
            )

        created_clients += 1
        LOG.info("Created client idx=%s id=%s", idx, client_id)

        for doc_idx in range(1, docs_to_create + 1):
            document_payload = generator.document(client_payload, idx, doc_idx)
            document_body = document_payload.to_json()
            document_body["clientId"] = client_id

            document_response = session.post(
                f"{api_host}/clients/{client_id}/documents",
                json=document_body,
                timeout=30,
            )
            if document_response.status_code >= 400:
                raise RuntimeError(
                    f"Document creation failed for client {client_id} "
                    f"(status {document_response.status_code}): {document_response.text}"
                )
            created_documents += 1
            LOG.debug(
                "Created document idx=%s/%s for client_id=%s title='%s'",
                doc_idx,
                docs_to_create,
                client_id,
                document_payload.title,
            )

        if idx % 100 == 0:
            elapsed = time.time() - start
            rate = created_clients / elapsed if elapsed else 0
            LOG.info(
                "Progress: %s clients, %s documents (%.1f clients/sec)",
                created_clients,
                created_documents,
                rate,
            )

    elapsed = time.time() - start if not dry_run else 0
    if dry_run:
        LOG.info(
            "Dry run complete. Would create %s clients and %s documents.",
            created_clients,
            created_documents,
        )
    else:
        LOG.info(
            "Data load complete in %.1fs. Created %s clients and %s documents.",
            elapsed,
            created_clients,
            created_documents,
        )


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Populate the Wealth Search API with pseudo data for local testing.",
    )
    parser.add_argument(
        "--host",
        default="http://localhost:8080",
        help="Base host where the API is reachable (default: %(default)s)",
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("WS_API_TOKEN", "demo-token"),
        help="Bearer token used for authentication (default: env WS_API_TOKEN or demo-token)",
    )
    parser.add_argument(
        "--clients",
        type=int,
        default=10_000,
        help="Number of clients to create (default: %(default)s)",
    )
    parser.add_argument(
        "--min-docs",
        type=int,
        default=1,
        help="Minimum number of documents per client (default: %(default)s)",
    )
    parser.add_argument(
        "--max-docs",
        type=int,
        default=7,
        help="Maximum number of documents per client (default: %(default)s)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="Optional random seed for reproducible data.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="If set, do not call the API. Only report the intended load.",
    )
    parser.add_argument(
        "--ollama-base-url",
        default=os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434"),
        help="Base URL for the local Ollama service (default: %(default)s)",
    )
    parser.add_argument(
        "--ollama-model",
        default=os.environ.get("OLLAMA_MODEL", "gemma2:2b"),
        help="Ollama model identifier to use when drafting documents (default: %(default)s)",
    )
    parser.add_argument(
        "--no-ollama",
        action="store_true",
        help="Disable Ollama-generated documents and fall back to templates.",
    )
    return parser.parse_args(argv)


def main() -> None:
    setup_logging()
    args = parse_args(sys.argv[1:])
    try:
        populate(
            api_host=args.host.rstrip("/"),
            token=args.token,
            client_count=args.clients,
            documents_min=args.min_docs,
            documents_max=args.max_docs,
            dry_run=args.dry_run,
            seed=args.seed,
            use_ollama=not args.no_ollama,
            ollama_base_url=args.ollama_base_url,
            ollama_model=args.ollama_model,
        )
    except KeyboardInterrupt:  # pragma: no cover - runtime convenience
        print("Interrupted by user", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:  # pragma: no cover - runtime convenience
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":  # pragma: no cover - script entry point
    main()
