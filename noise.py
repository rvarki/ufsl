#A script for creating similar images with noise
x, y = 10, 25
shade = 20

from PIL import Image
im = Image.open("/Users/sean/CS/EC504/ufsl/src/test/java/org/example/testImage1.png")
pix = im.load()

if im.mode == '1':
    value = int(shade >= 127) # Black-and-white (1-bit)
elif im.mode == 'L':
    value = shade # Grayscale (Luminosity)
elif im.mode == 'RGB':
    value = (shade, shade, shade)
elif im.mode == 'RGBA':
    value = (shade, shade, shade, 255)
elif im.mode == 'P':
    raise NotImplementedError("TODO: Look up nearest color in palette")
else:
    raise ValueError("Unexpected mode for PNG image: %s" % im.mode)

for x in range(0,600):
    for y in range(0,600):
        if x*y%500 == 0:
            pix[x, y] = value

im.save("testImage1Noisier.png")
