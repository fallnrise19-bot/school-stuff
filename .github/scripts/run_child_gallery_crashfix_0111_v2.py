from pathlib import Path

source = Path('.github/scripts/apply_child_gallery_crashfix_0111.py')
script = source.read_text()
old = "end_marker = '''\\n        }\\n    }\\n\\n    previewDocument?.let'''"
new = "end_marker = '''\\n    }\\n\\n    previewDocument?.let'''"
if old not in script:
    raise SystemExit('Expected original gallery end marker was not found')
script = script.replace(old, new, 1)
exec(compile(script, str(source), 'exec'), {})
