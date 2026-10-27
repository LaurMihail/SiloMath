"""Validate FD-74 document/refreeze consistency; never certify unimplemented runtime behavior."""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
CR = "CR-COUNTRY-CATALOG-CONTRACT-001"
MATERIAL = {"01", "02", "03", "06", "09", "10", "11", "12", "16", "18", "19", "20"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--write-evidence", action="store_true")
    args = parser.parse_args()
    checks = []

    def check(name, condition):
        checks.append({"name": name, "passed": bool(condition)})

    def load(path):
        return json.loads((ROOT / path).read_text(encoding="utf-8-sig"))

    contract = load(f"engineering/baseline-change/{CR}.json")
    expected = {
        "changeRequestId": CR, "decisionId": "FD-74", "status": "APPROVED",
        "catalog": "ISO 3166-1", "catalogCoverage": "COMPLETE",
        "canonicalEncoding": "ISO 3166-1 alpha-2", "canonicalCase": "UPPERCASE",
        "freeTextNewValuesAllowed": False, "newValuesMustBelongToCatalog": True,
        "newValueValidation": "FAIL_CLOSED", "optionsAvailableLocallyOrCached": True,
        "automaticHistoricalNormalizationAllowed": False,
        "historicalNonconformingValues": "PRESERVE_UNCHANGED; SEPARATE_AUDIT_BEFORE_ANY_MIGRATION",
        "implementationStatus": "NOT CERTIFIED BY DOCUMENTATION APPROVAL",
    }
    for name, value in expected.items():
        check("approved_contract:" + name,
              type(contract.get(name)) is type(value) and contract.get(name) == value)
    check("shared_organization_and_location_scope",
          contract["appliesTo"] == ["Organization.country", "Location.country"])
    check("approved_examples", contract["examples"] == {"Romania": "RO", "Germany": "DE", "United States": "US"})
    check("labels_not_canonical_values", "not the canonical persisted/API value" in contract["presentationLabel"])
    check("no_invented_catalogue_version", contract["catalogVersionIdentifier"] == "NOT ASSIGNED BY THIS CLARIFICATION")

    changes = load("engineering/baseline-change-register.json")
    records = [r for r in changes["records"] if r["id"] == CR]
    check("approved_unique_change_record", len(records) == 1 and records[0]["status"] == "APPROVED"
          and records[0]["changesFrozenBaseline"] is True)
    check("existing_cr_namespace", re.fullmatch(changes["changeRequestNamespace"]["pattern"], CR) is not None)

    registry = load("engineering/baseline-registry.json")
    manifest = load(registry["manifest"])
    files = {v["file"]: v for v in manifest["files"]}
    actual = {p.name for p in (ROOT / "baseline").glob("[0-9][0-9]_*.md")}
    check("exact_21_official_documents", len(actual) == 21 and actual == set(files)
          and len(registry["documents"]) == 21)
    check("frozen_registry", registry["status"] == "APPROVED_FROZEN")
    check("manifest_country_contract", manifest["countryContract"] == contract)
    check("approval_retains_calculation_contract", "CR-CALC-VERSION-CONTRACT-001" in manifest["approvedChanges"])
    previous_bytes = (ROOT / manifest["previousManifest"]).read_bytes()
    previous = json.loads(previous_bytes)
    old_files = {v["file"]: v for v in previous["files"]}
    check("previous_manifest_not_overwritten", hashlib.sha256(previous_bytes).hexdigest() == manifest["previousManifestSha256"])
    check("math_hash_preserved", manifest["mathematicalSemanticsSha256"] == previous["mathematicalSemanticsSha256"])
    check("math_source_commit_preserved", manifest["sourceMathBaselineCommit"] == previous["sourceMathBaselineCommit"])

    for doc in registry["documents"]:
        path = ROOT / doc["file"]
        data = path.read_bytes()
        text = data.decode("utf-8")
        metadata = files[path.name]
        check(path.name + ":frozen", "APPROVED / FROZEN" in text)
        check(path.name + ":current_revision",
              f"Baseline revision: {registry['refreezeDate']} - {registry['refreezeType']}" in text.replace("*", ""))
        check(path.name + ":utf8_lf_no_bom", not data.startswith(b"\xef\xbb\xbf") and b"\r" not in data)
        check(path.name + ":registry_hash", hashlib.sha256(data).hexdigest() == doc["sha256"])
        check(path.name + ":manifest_hash", hashlib.sha256(data).hexdigest() == metadata["sha256"])
        check(path.name + ":bytes_lines", len(data) == metadata["bytes"] == doc["bytes"]
              and len(text.splitlines()) == metadata["lines"] == doc["lines"])
        if doc["id"] in MATERIAL:
            check(path.name + ":country_trace", CR in text and "FD-74" in text and "ISO 3166-1" in text)
        elif manifest["approvedChange"] == CR:
            restored = re.sub(r"(?m)^(\*\*Baseline revision:\*\*|Baseline revision:) .+$",
                              lambda m: m.group(1) + " " + manifest["previousRevisionText"], text)
            check(path.name + ":metadata_only_delta",
                  hashlib.sha256(restored.encode("utf-8")).hexdigest() == old_files[path.name]["sha256"])

    decisions = (ROOT / "baseline/02_DECISION_REGISTER.md").read_text(encoding="utf-8")
    check("fd74_registered", "| FD-74 | Canonical Country catalogue and encoding" in decisions)
    check("fd_range_updated", "FD-01–FD-74" in decisions)
    ui = (ROOT / "baseline/06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION.md").read_text(encoding="utf-8")
    for field in ["ORG02-05", "P1SH01-05"]:
        row = next(line for line in ui.splitlines() if line.startswith("| " + field + " |"))
        check(field + ":no_free_text", "Selector (no free text)" in row)
        check(field + ":complete_iso_and_code", "Complete ISO 3166-1 catalogue" in row and "alpha-2" in row)
        check(field + ":local_label_and_fail_closed", "human-readable labels" in row and "locally/cached" in row
              and "fail-closed" in row and "Uppercase" in row)
    model = (ROOT / "baseline/09_DATA_MODEL.md").read_text(encoding="utf-8")
    country_rows = [line for line in model.splitlines() if line.startswith("| country |")]
    check("both_country_data_fields", len(country_rows) == 2 and all(
        "ISO 3166-1 alpha-2 code" in row and "human-readable name is presentation only" in row
        and "separate audit before migration" in row for row in country_rows))
    check("no_implicit_snapshot_schema_assignment", model.count("| snapshotSchemaVersion | Version |") == 2)
    check("business_rule_registered", "**BR-MD-014**" in (ROOT / "baseline/03_BUSINESS_RULES.md").read_text(encoding="utf-8"))
    check("acceptance_registered", "**TA-D233**" in (ROOT / "baseline/18_TEST_STRATEGY_ACCEPTANCE.md").read_text(encoding="utf-8"))
    failed = [c["name"] for c in checks if not c["passed"]]
    report = {"validator": "engineering/validate-country-contract.py", "baselineId": "SILOMATH_V6",
              "approvedChange": CR, "decisionId": "FD-74", "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
              "scope": "DOCUMENT/REGISTRY/HASH CONSISTENCY ONLY; runtime implementation not certified",
              "passed": len(checks) - len(failed), "failed": len(failed), "errors": failed, "checks": checks}
    if args.write_evidence:
        output = registry.get("contractValidationEvidence", {}).get(CR, registry["validationEvidence"])
        (ROOT / output).write_bytes((json.dumps(report, indent=2) + "\n").encode("utf-8"))
    print(f"COUNTRY BASELINE VALIDATION: {'FAIL' if failed else 'PASS'} ({report['passed']} checks; {report['failed']} failures)")
    for name in failed:
        print(" - " + name)
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
