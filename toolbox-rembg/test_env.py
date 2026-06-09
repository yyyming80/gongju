import sys
print('Python version:', sys.version)

print('\nTesting imports...')
try:
    import flask
    print('✓ Flask imported')
except Exception as e:
    print('✗ Flask import error:', e)

try:
    import rembg
    print('✓ rembg imported')
except Exception as e:
    print('✗ rembg import error:', e)

try:
    from PIL import Image
    print('✓ Pillow imported')
except Exception as e:
    print('✗ Pillow import error:', e)

print('\nAll tests completed!')
