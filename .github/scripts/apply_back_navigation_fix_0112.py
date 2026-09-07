from pathlib import Path

app = Path('app/src/main/java/ca/creativepixels/schoolstuff/SchoolStuffApp.kt')
text = app.read_text()

# Compose's internal child/add screens are not Activities. Without BackHandler,
# Android's edge-back gesture finishes MainActivity instead of navigating within
# School Stuff. Intercept it whenever an internal page is open.
import_anchor = 'import androidx.activity.compose.rememberLauncherForActivityResult\n'
if 'import androidx.activity.compose.BackHandler\n' not in text:
    if import_anchor not in text:
        raise SystemExit('Missing activity compose import anchor')
    text = text.replace(
        import_anchor,
        'import androidx.activity.compose.BackHandler\n' + import_anchor,
        1,
    )

state_anchor = '''        var tab by remember { mutableStateOf(MainTab.HOME) }\n        var childPage by remember { mutableStateOf<String?>(null) }\n        var addingThing by remember { mutableStateOf(false) }\n'''
if state_anchor not in text:
    raise SystemExit('Missing SchoolStuffApp navigation state anchor')

back_handler = state_anchor + '''\n        BackHandler(enabled = addingThing || childPage != null) {\n            when {\n                addingThing -> addingThing = false\n                childPage != null -> childPage = null\n            }\n        }\n'''
if 'BackHandler(enabled = addingThing || childPage != null)' not in text:
    text = text.replace(state_anchor, back_handler, 1)

app.write_text(text)

gradle = Path('app/build.gradle.kts')
g = gradle.read_text()
if 'versionCode = 12' not in g or 'versionName = "0.1.11"' not in g:
    raise SystemExit('Unexpected current version; refusing blind bump')
g = g.replace('versionCode = 12', 'versionCode = 13', 1)
g = g.replace('versionName = "0.1.11"', 'versionName = "0.1.12"', 1)
gradle.write_text(g)

print('System back navigation fixed; version 0.1.12 build 13.')
