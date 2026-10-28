"""Validate the approved calculation refreeze; optionally emit actual executed-check evidence."""
from pathlib import Path
import argparse
import hashlib
import json
import re
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
CR = "CR-CALC-VERSION-CONTRACT-001"
MATERIAL = {"01", "02", "07", "09", "10", "11", "12", "13", "16", "18", "19", "20"}
FORMULAS = ["CYL_TRUNC_CONE_V1", "CYL_FLAT_V1", "SQUARE_FLAT_V1", "RECT_FLAT_V1", "SQUARE_PYRAMID_HOPPER_V1", "RECT_PYRAMID_HOPPER_V1", "REG_HEX_FLAT_V1", "STAR_VERIFIED_AREA_V1", "POCKET_VERIFIED_AREA_V1", "PERIMETER_VERIFIED_AREA_V1"]

def mathematical_sections(text):
    return text.split("## 3. Canonical calculation pipeline", 1)[1].split("## 20.", 1)[0] + text.split("## 26. Golden vectors", 1)[1].split("## 29.", 1)[0]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--write-evidence", action="store_true")
    args = parser.parse_args()
    checks = []
    def check(name, passed):
        checks.append({"name": name, "passed": bool(passed)})
    contract = json.loads((ROOT / "engineering/baseline-change" / (CR + ".json")).read_text(encoding="utf-8"))
    expected = {"engineVersion": "2.0", "formulaVersion": 1, "inputSchemaVersion": 1, "normalizationVersion": 1, "surfaceModelVersion": "LEVEL_BASED_V1", "aggregationMethod": "MEAN_ULLAGE", "aggregationRuleVersion": 1, "combinedSemanticIdentifier": "MEAN_ULLAGE_V1"}
    check("approval", contract["status"] == "APPROVED" and contract["changeRequestId"] == CR)
    for key, value in expected.items():
        check("typed_tuple:" + key, type(contract[key]) is type(value) and contract[key] == value)
    check("unchanged_formula_codes", contract["formulaCodes"] == {f"F{i:02}": value for i, value in enumerate(FORMULAS, 1)})
    registry = json.loads((ROOT / "engineering/baseline-registry.json").read_text(encoding="utf-8"))
    manifest = json.loads((ROOT / registry["manifest"]).read_text(encoding="utf-8"))
    entries = {v["file"]: v for v in manifest["files"]}
    actual_files = {p.name for p in (ROOT / "baseline").glob("[0-9][0-9]_*.md")}
    check("exact_21_documents", len(actual_files) == 21 and actual_files == set(entries) and len(registry["documents"]) == 21)
    for entry in registry["documents"]:
        path = ROOT / entry["file"]
        data = path.read_bytes()
        text = data.decode("utf-8")
        check(path.name + ":frozen", "APPROVED / FROZEN" in text)
        check(path.name + ":revision", f"Baseline revision: {registry['refreezeDate']} - {registry['refreezeType']}" in text.replace("*", ""))
        check(path.name + ":utf8_no_bom_lf", not data.startswith(b"\xef\xbb\xbf") and b"\r" not in data)
        check(path.name + ":registry_hash", hashlib.sha256(data).hexdigest() == entry["sha256"])
        check(path.name + ":manifest_hash", hashlib.sha256(data).hexdigest() == entries[path.name]["sha256"])
        check(path.name + ":aggregation_field", re.search(r"aggregationMethod\s*=\s*`?MEAN_ULLAGE_V1", text) is None)
        if entry["id"] in MATERIAL:
            check(path.name + ":material_reference", CR in text and 'engineVersion="2.0"' in text and "inputSchemaVersion=1" in text and "normalizationVersion=1" in text and "formulaVersion=1" in text)
    doc13 = (ROOT / "baseline/13_CALCULATION_ENGINE_SPECIFICATION.md").read_text(encoding="utf-8")
    section20 = doc13.split("## 20.", 1)[1].split("## 21.", 1)[0]
    check("doc13_section20", "aggregationMethod=`MEAN_ULLAGE`" in section20 and "aggregationRuleVersion=`1`" in section20)
    for i, formula in enumerate(FORMULAS, 1):
        check("formula_mapping:" + formula, f"| F{i:02} | {formula} | 1 |" in doc13)
    check("unchanged_mathematical_semantics_and_golden_vectors", hashlib.sha256(mathematical_sections(doc13).encode("utf-8")).hexdigest() == manifest["mathematicalSemanticsSha256"])
    check("decision_registered", "| FD-73 | Approved Calculation Version Contract" in (ROOT / "baseline/02_DECISION_REGISTER.md").read_text(encoding="utf-8"))
    changes = json.loads((ROOT / "engineering/baseline-change-register.json").read_text(encoding="utf-8"))
    check("approved_change_registered", any(v["id"] == CR and v["status"] == "APPROVED" and v["changesFrozenBaseline"] is True for v in changes["records"]))
    failed = [v["name"] for v in checks if not v["passed"]]
    report = {"validator": "engineering/validate-calculation-version-contract.py", "baselineId": "SILOMATH_V6", "approvedChange": CR, "generatedAtUtc": datetime.now(timezone.utc).isoformat(), "passed": len(checks)-len(failed), "failed": len(failed), "errors": failed, "checks": checks}
    if args.write_evidence:
        (ROOT / registry.get("contractValidationEvidence", {}).get(CR, registry["validationEvidence"])).write_bytes((json.dumps(report, indent=2) + "\n").encode("utf-8"))
    print(f"CALCULATION VERSION BASELINE VALIDATION: {'FAIL' if failed else 'PASS'} ({report['passed']} checks; {report['failed']} failures)")
    for name in failed:
        print(" - " + name)
    return 1 if failed else 0

if __name__ == "__main__":
    raise SystemExit(main())
