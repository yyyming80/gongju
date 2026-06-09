from flask import Flask, request, jsonify, send_file
from rembg import remove, new_session
from PIL import Image, ImageFilter
import io
import base64
import numpy as np

app = Flask(__name__)

# 使用高质量的 BiRefNet 模型
# 可选模型: 'u2net', 'u2netp', 'u2net_human_seg', 'u2net_cloth_seg', 'silueta', 'isnet-general-use', 'isnet-anime', 'birefnet-general', 'birefnet-human', 'birefnet-portrait'
# 'birefnet-portrait' 最适合证件照人像
session = new_session('birefnet-portrait')

def alpha_feather(img, feather_amount=3):
    """
    Alpha通道边缘羽化处理，让边缘更自然
    """
    if img.mode != 'RGBA':
        return img
    
    alpha = img.split()[-1]
    # 对Alpha通道进行高斯模糊，实现羽化效果
    alpha_blurred = alpha.filter(ImageFilter.GaussianBlur(radius=feather_amount))
    
    result = img.copy()
    result.putalpha(alpha_blurred)
    return result

def defringe_alpha(img, threshold=200, defringe_size=2):
    """
    去除边缘白边处理 (Defringe)
    """
    if img.mode != 'RGBA':
        return img
    
    data = np.array(img)
    alpha_channel = data[:, :, 3]
    
    # 找到半透明区域，去除白边
    edge_mask = (alpha_channel > 0) & (alpha_channel < 255)
    
    # 对边缘区域应用去白边处理
    for y in range(img.height):
        for x in range(img.width):
            if edge_mask[y, x]:
                # 获取这个像素周围的颜色
                alpha = alpha_channel[y, x]
                if alpha < 255:
                    # 调整RGB值，避免白色边缘
                    r, g, b, a = data[y, x]
                    # 按透明度比例降低RGB值，减少白边
                    factor = alpha / 255.0
                    data[y, x] = [
                        int(r * factor),
                        int(g * factor),
                        int(b * factor),
                        a
                    ]
    
    return Image.fromarray(data, 'RGBA')

def smooth_edges(img, iterations=2):
    """
    多次平滑处理，让边缘更自然
    """
    if img.mode != 'RGBA':
        return img
    
    result = img.copy()
    for _ in range(iterations):
        result = alpha_feather(result, 1)
    
    return result

@app.route('/health', methods=['GET'])
def health():
    print('Health check called')
    return jsonify({'status': 'ok', 'service': 'rembg-api - BiRefNet High Quality'})

@app.route('/api/remove-background', methods=['POST'])
def remove_background():
    try:
        print('Remove background called - High Quality Mode')
        if 'image' not in request.files:
            return jsonify({'error': 'No image file provided'}), 400
        
        file = request.files['image']
        if file.filename == '':
            return jsonify({'error': 'No file selected'}), 400
        
        print('Processing image with BiRefNet model...')
        input_image = Image.open(file.stream)
        
        # 使用高质量模型和alpha_matting抠图
        output_image = remove(
            input_image,
            session=session,
            alpha_matting=True,           # 启用Alpha Matting，提高边缘质量
            alpha_matting_foreground_threshold=240,  # 前景阈值（0-255）
            alpha_matting_background_threshold=10,   # 背景阈值（0-255）
            alpha_matting_erode_size=10              # 腐蚀大小，让边缘更紧凑
        )
        
        print('Applying post-processing...')
        
        # 1. 边缘羽化处理
        output_image = alpha_feather(output_image, feather_amount=2)
        
        # 2. 去除白边处理
        output_image = defringe_alpha(output_image)
        
        # 3. 再次平滑边缘
        output_image = smooth_edges(output_image, iterations=2)
        
        print('Image processed successfully with high quality!')
        
        img_byte_arr = io.BytesIO()
        output_image.save(img_byte_arr, format='PNG')
        img_byte_arr.seek(0)
        
        return send_file(img_byte_arr, mimetype='image/png')
    
    except Exception as e:
        print('Error:', str(e))
        import traceback
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print('='*50)
    print('Starting Rembg AI Background Removal Service')
    print('High Quality Mode - BiRefNet Portrait Model')
    print('='*50)
    print('Health check: http://localhost:5000/health')
    print('API endpoint: http://localhost:5000/api/remove-background')
    print('Features:')
    print('  - BiRefNet Portrait Model (Best for ID Photos)')
    print('  - Alpha Matting Enabled')
    print('  - Edge Feathering')
    print('  - Defringe (White Edge Removal)')
    print('  - Hair Detail Preservation')
    print('='*50)
    app.run(host='0.0.0.0', port=5000, debug=True)
