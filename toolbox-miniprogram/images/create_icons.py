import os
from PIL import Image, ImageDraw

# 创建images目录
os.makedirs('d:/gongju/toolbox-miniprogram/images', exist_ok=True)

def create_circular_icon(filename, size=81):
    # 创建透明背景的圆形图标
    img = Image.new('RGBA', (size, size), (255, 255, 255, 0)
    draw = ImageDraw.Draw(img)
    
    # 画一个简单的圆形图标
    padding = 15
    draw.ellipse([padding, padding, size-padding, size-padding], fill=(102, 126, 234, 255))
    img.save('d:/toolbox-miniprogram/images/' + filename, 'PNG')

# 创建4个图标
create_circular_icon('home.png')
create_circular_icon('home-active.png')
create_circular_icon('profile.png')
create_circular_icon('profile-active.png')

print('Icons created!')
