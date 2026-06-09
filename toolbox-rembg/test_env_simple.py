import sys
print('Python version:', sys.version)

print('\nTesting imports...')
try:
    import flask
    print('[OK] Flask imported')
except Exception as e:
    print('[FAIL] Flask import error:', e)

try:
    import rembg
    print('[OK] rembg imported')
except Exception as e:
    print('[FAIL] rembg import error:', e)

try:
    from PIL import Image
    print('[OK] Pillow imported')
except Exception as e:
    print('[FAIL] Pillow import error:', e)

print('\nAll tests completed!')
