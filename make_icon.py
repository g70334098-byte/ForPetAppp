from PIL import Image, ImageDraw, ImageFont
import os

width, height = 512, 512
image = Image.new('RGBA', (width, height), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)

# Draw rounded sky-blue rectangle background (#0284C7)
bg_color = (2, 132, 199, 255)
draw.rounded_rectangle([0, 0, width, height], radius=110, fill=bg_color)

# Draw main white paw print
center_x, center_y = 256, 220

# Main pad
draw.ellipse([center_x - 80, center_y - 30, center_x + 80, center_y + 90], fill=(255, 255, 255, 255))

# 4 Toe pads
toes = [
    (center_x - 90, center_y - 85, 32),
    (center_x - 35, center_y - 120, 34),
    (center_x + 35, center_y - 120, 34),
    (center_x + 90, center_y - 85, 32),
]

for tx, ty, r in toes:
    draw.ellipse([tx - r, ty - r, tx + r, ty + r], fill=(255, 255, 255, 255))

# Save image
output_path1 = r'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\포펫_앱아이콘.png'
output_path2 = r'C:\Users\g7033\AppData\Local\Google\AndroidStudio2026.2.1\projects\forpetapp2.65492385\.artifacts\931392ae-5d6f-4451-a46c-50ce266309ad\forpet_app_icon.png'

os.makedirs(os.path.dirname(output_path2), exist_ok=True)
image.save(output_path1)
image.save(output_path2)
print('Successfully saved app icon PNG to both locations!')
