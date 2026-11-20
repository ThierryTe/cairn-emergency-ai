# Voice SOS

Start at **Emergency SOS → Talk to Cairn**. The microphone beside Emergency SOS on Home opens the same workflow. Dictation fills an editable field. **Review and continue** interprets the text and opens the next step, such as a Rescue Card, a guide, a location preview or an action confirmation.

For an emergency description, review the incident, number of people present, urgent needs and short description. The fields contain only details extracted from the current text. Saved counts and needs from My Household are offered separately, since current attendance and needs can differ. GPS is requested automatically only if permission already exists. Use **Add device location** to request permission. **Use this location in the draft** attaches a fresh fix with coordinates, timestamp and accuracy when available. Fixes expire after two minutes.

The Rescue Card and editable SMS use the same reviewed fields. Cairn reads the proposed action when an offline system voice is available and **Read Aloud** is enabled. Tap the confirmation button to open the system phone keypad or Messages. Enter the local number in the dialer or choose a recipient in Messages. Use a consenting test contact and stop before dialing or sending during acceptance testing.

## Xiaomi Redmi 12 acceptance

1. Open **Emergency SOS → Talk to Cairn**. Say “Cairn, there is flooding in my house. We are three people and my mother cannot walk.”
2. Verify that the transcription is editable. Tap **Review and continue**. Check that the card shows flooding, three people and the stated mobility need. Leave the physical location blank until a fix is available or you supply a location.
3. Grant location permission using **Add device location**. Check the coordinates, timestamp and accuracy if present, then tap **Use this location in the draft**. Check that **Remove device location** updates the draft.
4. Edit the SMS. Tap **Open Messages** and listen to or read the prompt. Tap **Cancel** once to verify the app stays in Cairn. Open it again and confirm. Check that Messages receives the reviewed draft. Leave the draft unsent.
5. Return to Voice SOS and say “Call emergency services.” Review the text, tap **Review and continue**, and verify the phone keypad opens only after the confirmation button. Leave the call unplaced.
6. Repeat in airplane mode. Also deny microphone and location permissions in separate runs. Verify the editable text and manually entered location alternatives.

## Airplane mode

| Capability | Expected behavior | Redmi 12 test |
| --- | --- | --- |
| Dictation | Requires an Android on-device recognizer and an installed model for the selected language. If unavailable, type. The optional phone speech service may require internet and prompts before use. | Pending |
| Commands, card, drafts and guides | Available locally, independent of Firebase and connectivity. | Pending |
| Read aloud | Requires an installed offline Android voice for the selected language and the app's Read Aloud setting. The action text stays visible. | Pending |
| Location | An enabled provider can supply a fresh fix, including GPS if enabled and supported. The app excludes stale fixes and accepts a manually entered location. | Pending |
| Dialer and Messages | Installed system apps can be opened for review. Call placement and SMS delivery depend on the device, carrier and network state. | Pending |

Android services used by Voice SOS are documented in the [speech recognition reference](https://developer.android.com/reference/android/speech/SpeechRecognizer), [location reference](https://developer.android.com/reference/android/location/LocationManager) and [common intents guide](https://developer.android.com/guide/components/intents-common).
