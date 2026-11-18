# Cairn architecture

Single Android application module, organized by feature and responsibility.

- `app/src/main/ai`: Firebase AI Logic integration, one router boundary, prompt/schema adapters.
- `app/src/main/java/com/cairn/emergency/domain`: profile, deterministic kit rules, safety decisions.
- `app/src/main/java/com/cairn/emergency/data`: Room persistence, guide index, queued requests.
- `app/src/main/java/com/cairn/emergency/feature`: household, kit, assistant, simulator, SOS and accessibility.
- `app/src/main/java/com/cairn/emergency/ui`: shared Compose design system.
- `prompts/`: versioned prompts. Remote Config selects only bundled prompt versions.
- `eval/`: safety, grounding and routing evaluation cases.

Hybrid inference is experimental. Firebase AI Logic provides PREFER_ON_DEVICE, ONLY_ON_DEVICE, PREFER_IN_CLOUD and ONLY_IN_CLOUD. The router must inspect the returned inference source and apply explicit timeouts and fallbacks. On-device inference requires Android API 26 and an eligible device/model. Never label static offline guidance as AI output.

The first slice adds the Firebase SDKs and App Check debug dependency. Runtime Firebase initialization and an actual model call require a project configuration and registered debug token; no AI success is simulated.
