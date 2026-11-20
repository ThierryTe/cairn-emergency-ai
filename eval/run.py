#!/usr/bin/env python3
"""Evaluate the app's guide selection and deterministic fallback with scripted model responses."""

import argparse
import base64
import json
from pathlib import Path
import subprocess
import tempfile
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1]
CASES = Path(__file__).with_name("assistant-cases.json")
SOURCE = ROOT / "app/src/main/java/com/cairn/emergency"
RES = ROOT / "app/src/main/res"
LOCALES = {"en": "values", "fr": "values-fr", "es": "values-es"}
GUIDES = {"Flood": "flood", "Power": "power", "Fire": "fire", "Earthquake": "quake", "InsulinStorage": "insulin"}


def encode(value):
    return base64.b64encode(value.encode()).decode("ascii")


def input_line(case):
    parts = [encode(case["question"]), str(case["cloudAllowed"]).lower(), str(case["online"]).lower(), "3"]
    for engine in ("LOCAL", "CLOUD"):
        answer = case["replies"].get(engine, {})
        parts.extend((answer.get("source") or "-", encode(answer.get("text", ""))))
    return "\t".join(parts)


def load_resources(locale):
    folder = RES / LOCALES[locale]
    strings = ElementTree.parse(folder / "strings.xml")
    arrays = ElementTree.parse(folder / "emergency_guides.xml")
    return (
        {item.attrib["name"]: item.text for item in strings.getroot()},
        {item.attrib["name"]: [step.text for step in item] for item in arrays.getroot()},
    )


def grade(case, actual, resources):
    topic, urgent_text, engine, order_text, routes_text = actual.split("\t")
    urgent = urgent_text == "true"
    order = [int(step) for step in order_text.split(",")] if order_text else []
    routes = routes_text.split(",") if routes_text else []
    strings, guides = resources
    expected_topic = case["topic"] or "-"
    issues = []

    if topic != expected_topic:
        issues.append(f"topic {topic}, expected {expected_topic}")
    if urgent != case["safetyBanner"]:
        issues.append(f"emergency banner {urgent}, expected {case['safetyBanner']}")
    if engine != case["engine"]:
        issues.append(f"engine {engine}, expected {case['engine']}")
    if "CLOUD" in routes and (
        not case["cloudAllowed"] or not case["online"] or case["safetyBanner"] or expected_topic in ("-", "InsulinStorage")
    ):
        issues.append("cloud route was offered for a restricted question")

    answer = []
    if urgent:
        answer.append(strings["assistant_safety"])
    if topic == "-":
        if routes or order:
            issues.append("sent a question with no guide to a model")
        answer.append(strings["assistant_no_guide"])
    elif topic in GUIDES:
        guide_id = GUIDES[topic]
        steps = guides[f"guide_{guide_id}_steps"]
        if sorted(order) != list(range(len(steps))):
            issues.append("the answer did not contain exactly the published guide steps")
        else:
            answer += [strings[f"guide_{guide_id}"], *[steps[index] for index in order], strings[f"guide_{guide_id}_source"]]
            if (urgent or topic == "InsulinStorage") and order[0] != 0:
                issues.append("changed the first published instruction in a safety-sensitive guide")

    visible = " ".join(answer).casefold()
    issues.extend(f"missing {text!r}" for text in case["mustInclude"] if text.casefold() not in visible)
    issues.extend(f"contains forbidden text {text!r}" for text in case["mustNotSay"] if text.casefold() in visible)
    return issues


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kotlinc", default="kotlinc")
    parser.add_argument("--java", default="java")
    parser.add_argument("--probe-classpath", help="Run an already compiled PolicyProbe with its Kotlin runtime classpath.")
    args = parser.parse_args()
    cases = json.loads(CASES.read_text(encoding="utf-8"))
    if len({case["id"] for case in cases}) != len(cases):
        parser.error("evaluation case IDs must be unique")

    resources = {locale: load_resources(locale) for locale in LOCALES}
    with tempfile.TemporaryDirectory(prefix="cairn-eval-") as temporary:
        classpath = args.probe_classpath
        if classpath is None:
            classpath = str(Path(temporary) / "probe.jar")
            try:
                subprocess.run(
                    [args.kotlinc, str(SOURCE / "ai/InferenceRouting.kt"), str(SOURCE / "feature/assistant/GuideMatching.kt"),
                     str(Path(__file__).with_name("PolicyProbe.kt")), "-include-runtime", "-d", classpath],
                    check=True,
                )
            except FileNotFoundError:
                parser.error("Kotlin CLI was not found. Set --kotlinc to its path or supply --probe-classpath.")
        result = subprocess.run(
            [args.java, "-cp", classpath, "com.cairn.eval.PolicyProbeKt"],
            input="\n".join(map(input_line, cases)) + "\n", encoding="utf-8", capture_output=True, check=True,
        )
    outputs = result.stdout.splitlines()
    if len(outputs) != len(cases):
        parser.error(f"probe returned {len(outputs)} results for {len(cases)} cases")
    passed = 0
    for case, actual in zip(cases, outputs):
        issues = grade(case, actual, resources[case["locale"]])
        if issues:
            print("FAIL", case["id"], " | ".join(issues))
        else:
            passed += 1
    print(f"Deterministic policy and grounding: {passed}/{len(cases)} ({100 * passed / len(cases):.1f}%)")
    return 0 if passed == len(cases) else 1


if __name__ == "__main__":
    raise SystemExit(main())
