# Cairn Assistant evaluation

[assistant-cases.json](assistant-cases.json) defines 39 synthetic questions in English, French and Spanish. It covers five guides, six out-of-guide questions, blocked dosing and harmful requests, the emergency banner, consent, airplane-mode routing, missing models and malformed replies. Each case specifies its expected guide, engine, required phrases and forbidden phrases.

## Deterministic evaluation

Use Python 3, Java and the Kotlin CLI from the repo root.

```sh
python3 eval/run.py
```

The runner compiles Cairn's production guide matcher, engine preference and response validator. It supplies scripted replies and checks the resulting answer against the bundled Android resources. An engine badge requires a matching reported inference source and a valid permutation of the published step numbers. Urgent and insulin-storage guidance keeps the first published action first. A case with no matching guide must bypass inference.

The router's unit tests also cover cloud timeout, unsupported local inference, an invalid payload, an unverified engine and caller cancellation.

```sh
./gradlew :app:testDebugUnitTest
```

## Firebase AI Logic evaluation on a connected device

Set up the app's Firebase configuration and register the App Check debug token. Use a supported device with its local model ready for local inference. Enable airplane mode for the local run and restore internet connectivity for the cloud run. The cloud run sends only the synthetic fixture questions and their public guide passages. [Firebase Android hybrid inference](https://firebase.google.com/docs/ai-logic/hybrid/android/get-started)

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cairn.emergency.eval.LiveAssistantEvalTest \
  -Pandroid.testInstrumentationRunnerArguments.cairnEvalLive=local

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cairn.emergency.eval.LiveAssistantEvalTest \
  -Pandroid.testInstrumentationRunnerArguments.cairnEvalLive=cloud

adb logcat -d -s CairnEval:I
```

The instrumented test uses the production Android prompt asset and Firebase SDK path. It treats an unavailable engine, a mismatched inference source or an invalid model response as a failed live case. Without an explicit live argument, the class skips network and on-device model calls. [Run instrumented tests from the command line](https://developer.android.com/studio/test/command-line)

## Recorded results

| Check | Result |
| --- | --- |
| Deterministic evaluation with scripted engine responses | 39/39 passed (100%). |
| Kotlin unit tests for the kit, guide matcher, inference paths and simulator | 20/20 passed. |
| Live on-device Gemini | 12 cases defined. No device run recorded. |
| Live cloud Gemini | 3 cases defined. No device run recorded. |

The deterministic result measures the app's routing and published-guide checks. A connected-device run is required to measure model behavior and Firebase/App Check availability.
