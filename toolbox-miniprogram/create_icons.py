from PIL import Image, ImageDraw, ImageFont
import os

# 创建images目录
os.makedirs('d:/gongju/toolbox-miniprogram/images', exist_ok=True)

def create_icon(filename, size=81):
    img = Image.new('RGBA', (size, size), (255, 255, 255, 0)
    draw = ImageDraw.Draw(img)
    
    # 画一个简单的图标
    # 圆圈
    draw.ellipse([10, 10, size-10, size-10], fill=(102, 126, 234, 255)
    
    # 保存
    img.save(f'd:/gongju/toolbox-miniprogram/images/{filename}', 'PNG')

# 创建图标
create_icon('home.png')
create_icon('home-active.png')
create_icon('profile.png')
create_icon('profile-active.png')

print('图标创建完成！')
