import os
from PIL import Image, ImageDraw

os.makedirs('images', exist_ok=True)

img = Image.new('RGBA', (81, 81), (255, 255, 255, 0)
draw = ImageDraw.Draw(img)
draw.ellipse((15, 15, 66, 66), fill=(102, 126, 234, 255)
img.save('images/home.png')

img2 = Image.new('RGBA', (81, 81), (255, 255, 255, 0)
draw2 = ImageDraw.Draw(img2)
draw2.ellipse((15, 15, 66, 66), fill=(102, 126, 234, 255)
img2.save('images/profile.png')

print('Icons created!')
