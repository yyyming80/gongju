import os
from PIL import Image, ImageDraw

def create_icon(filename, size=81):
    img = Image.new('RGBA', (size, size), (255, 255, 255, 0)
    draw = ImageDraw.Draw(img)
    padding = 15
    draw.ellipse((padding, padding, size-padding, size-padding), fill=(102, 126, 234, 255))
    img.save('images/' + filename, 'PNG')
    print('Created: images/' + filename)

os.makedirs('images', exist_ok=True)
create_icon('home.png')
create_icon('home-active.png')
create_icon('profile.png')
create_icon('profile-active.png')
print('All icons created!')
