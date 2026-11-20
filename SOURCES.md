# Sources

## 72-hour kit

The kit uses original English, French and Spanish summaries of [French Civil Protection's 72-hour emergency kit guidance](https://www.securite-civile.interieur.gouv.fr/reagir/comment-se-preparer-face-aux-risques/kit-durgence). The example in the mockup uses this baseline of six liters of drinking water per person for 72 hours. The baseline applies to the selected kit guidance regardless of the app language. Local recommendations can differ. The quantity is for household preparedness and does not set an infant's intake or a medication dose.

Reviewed against the source on 2026-11-20. Release review of safety-critical translations is still pending.

## Offline SOS guides

The app bundles concise summaries for four incidents and insulin storage, with English, French and Spanish text. The recommendations were checked against the following primary sources on 2026-11-20. Independent review of the translations is still pending.

- Flood: [French Civil Protection flood guidance](https://www.securite-civile.interieur.gouv.fr/reagir/risques-majeurs/inondations), [FEMA flood annex](https://www.ready.gov/sites/default/files/2019.CERT_.HazardAnnexes.IG_.FINAL_.508c.pdf), and [FEMA flood guidance in Spanish](https://www.ready.gov/sites/default/files/2024-07/ready.gov_flood_info-sheet_spanish.pdf).
- Power outage: [CDC guidance for power outages](https://www.cdc.gov/natural-disasters/response/what-to-do-protect-yourself-during-a-power-outage.html) and [the Spanish edition](https://cdc.gov/natural-disasters/es/response/que-hacer-para-protegerse-durante-un-apagon.html).
- Home fire: [French Civil Protection home fire guidance](https://www.securite-civile.interieur.gouv.fr/reagir/risques-de-vie-courante/incendie-domestique) and [U.S. Fire Administration escape planning](https://www.usfa.fema.gov/gallery/pictographs/escape-planning.html).
- Earthquake: [French Civil Protection earthquake guidance](https://www.securite-civile.interieur.gouv.fr/reagir/risques-majeurs/seisme) and [FEMA earthquake guidance in Spanish](https://www.ready.gov/sites/default/files/2024-07/ready.gov_earthquake_info-sheet_spanish.pdf).
- Insulin storage: [FDA emergency insulin storage guidance](https://www.fda.gov/drugs/emergency-preparedness-drugs/information-regarding-insulin-storage-and-switching-between-products-emergency) and [FDA medicine safety after a natural disaster](https://www.fda.gov/drugs/emergency-preparedness-drugs/safe-drug-use-after-natural-disaster). The guide covers storage and refers users to a pharmacist or care team for product-specific advice.

## Crisis simulator

The flood and power-outage exercises use the flood and power-outage sources above. They use original situations with fixed decisions and feedback in English, French and Spanish. Independent review of the translations is still pending.

### Simulator artwork

The built-in ImageGen tool generated these two original scene illustrations for the card layout.

- [Flood banner](app/src/main/res/drawable-nodpi/sim_flood.jpg). Prompt: “Use case: photorealistic-natural. Generate a landscape banner asset for the Cairn Android emergency-preparedness simulator, matching the user's design. Small rural neighborhood, a wooden house at right with water at the foundation, calm shallow floodwater across the center foreground reflecting blue-grey cloud breaks and a row of leafy trees on the left. Clear separation of water, house, and trees; realistic disaster preparedness editorial photo, natural daylight, blue-green and muted warm details. Ideal center strip must read well when cropped to a wide 3:1 card. No people, no logos, no typography, no labels, no borders, no UI.”
- [Power outage banner](app/src/main/res/drawable-nodpi/sim_power.jpg). Prompt: “Use case: photorealistic-natural. Generate a landscape banner asset for the Cairn Android emergency-preparedness simulator. Quiet small neighborhood in a power outage at dusk. Dark house rooftops and a few unlit residential buildings silhouetted under a warm orange sunset horizon with indigo blue clouds above. Utility poles and lines clearly visible on the right. Real photographic textures, low angle streetscape, soft sunset with deep blue shade, clear safe editorial mood. Ideal center strip must read when cropped to a wide 3:1 card. No people, no logos, no typography, no labels, no borders, no UI.”
