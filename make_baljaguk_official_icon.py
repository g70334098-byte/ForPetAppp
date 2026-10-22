import os
import math
from PIL import Image, ImageDraw, ImageFont

width, height = 512, 512
image = Image.new('RGBA', (width, height), (250, 249, 246, 255))
draw = ImageDraw.Draw(image)

# 1. Circle Borders (Orange top arch & Blue bottom arch)
orange_color = (249, 115, 22, 255) # #F97316
blue_color = (56, 189, 248, 255)   # #38BDF8
dark_text_color = (30, 41, 59, 255) # #1E293B
sub_text_color = (100, 116, 139, 255) # #64748B

# Outer Circle Arcs
margin = 35
line_width = 12
bbox = [margin, margin, width - margin, height - margin]

# Top-left to Top-right Orange Arc
draw.arc(bbox, start=190, end=350, fill=orange_color, width=line_width)

# Bottom-left to Bottom-right Blue Arc
draw.arc(bbox, start=20, end=170, fill=blue_color, width=line_width)

# 2. Orange Paw Main Pad & Heart Outline
center_x, center_y = 256, 190

# Main Paw Pad
draw.ellipse([center_x - 55, center_y - 15, center_x + 55, center_y + 45], fill=orange_color)

# 4 Toe Pads
toes = [
    (center_x - 60, center_y - 40, 16, 20),
    (center_x - 22, center_y - 60, 17, 22),
    (center_x + 22, center_y - 60, 17, 22),
    (center_x + 60, center_y - 40, 16, 20),
]

for tx, ty, rx, ry in toes:
    draw.ellipse([tx - rx, ty - ry, tx + rx, ty + ry], fill=orange_color)

# 3. Blue Footprints & Small Paw Prints
# Shoe footprint 1 & 2
draw.ellipse([center_x - 70, center_y + 70, center_x - 20, center_y + 95], fill=blue_color)
draw.ellipse([center_x - 30, center_y + 85, center_x + 20, center_y + 110], fill=blue_color)

# Small blue paw steps
draw.ellipse([center_x + 50, center_y + 50, center_x + 68, center_y + 64], fill=blue_color)
draw.ellipse([center_x + 75, center_y + 68, center_x + 93, center_y + 82], fill=blue_color)
draw.ellipse([center_x + 55, center_y + 82, center_x + 73, center_y + 96], fill=blue_color)

# 4. Text (Title: 발자국동행, Sub: 반려동물과 함께 걷는 따뜻한 길)
try:
    font_title = ImageFont.truetype("malgun.ttf", 46)
    font_sub = ImageFont.truetype("malgun.ttf", 20)
except Exception:
    font_title = ImageFont.load_default()
    font_sub = ImageFont.load_default()

draw.text((256, 355), "발자국동행", fill=dark_text_color, font=font_title, anchor="mm")
draw.text((256, 415), "반려동물과 함께 걷는 따뜻한 길", fill=sub_text_color, font=font_sub, anchor="mm")

# Save
output_path1 = r'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\baljaguk_app_icon.png'
output_path2 = r'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\발자국동행_앱아이콘.png'

image.save(output_path1)
image.save(output_path2)
print("Baljaguk Donghaeng Official Emblem Generated Successfully!")
