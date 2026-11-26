"""Generate the Xpersiva brand assets and a short vertical launch teaser."""

from __future__ import annotations

import math
import subprocess
import wave
from pathlib import Path

import imageio_ffmpeg
import numpy as np
from PIL import Image, ImageDraw, ImageFont


OUT = Path(__file__).resolve().parent
PURPLE = (109, 74, 255)
PURPLE_LIGHT = (162, 139, 255)
PURPLE_DEEP = (76, 49, 204)
NAVY = (13, 15, 32)
WHITE = (255, 255, 255)
MUTED = (190, 188, 214)
FONT_BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")
FONT_REG = Path(r"C:\Windows\Fonts\segoeui.ttf")


def font(size: int, bold: bool = False):
    return ImageFont.truetype(str(FONT_BOLD if bold else FONT_REG), size)


def mark(draw: ImageDraw.ImageDraw, box, colors, center_color):
    x, y, size = box
    p = [
        [(90, 90), (170, 90), (256, 210), (210, 256), (90, 170)],
        [(342, 90), (422, 90), (422, 170), (302, 256), (256, 210)],
        [(90, 342), (210, 256), (256, 302), (170, 422), (90, 422)],
        [(302, 256), (422, 342), (422, 422), (342, 422), (256, 302)],
    ]
    for poly, color in zip(p, colors):
        draw.polygon([(x + a * size / 512, y + b * size / 512) for a, b in poly], fill=color)
    # The central gap is the visual "portal" of the symbol.
    draw.polygon(
        [(x + a * size / 512, y + b * size / 512) for a, b in [(256, 210), (302, 256), (256, 302), (210, 256)]],
        fill=center_color,
    )


def text_center(draw, xy, value, fnt, fill, spacing=0):
    x, y = xy
    if spacing == 0:
        box = draw.textbbox((0, 0), value, font=fnt)
        draw.text((x - (box[2] - box[0]) / 2, y), value, font=fnt, fill=fill)
        return
    widths = [draw.textlength(ch, font=fnt) for ch in value]
    total = sum(widths) + spacing * (len(value) - 1)
    pos = x - total / 2
    for ch, width in zip(value, widths):
        draw.text((pos, y), ch, font=fnt, fill=fill)
        pos += width + spacing


def export_static():
    icon = Image.new("RGB", (1024, 1024), PURPLE)
    d = ImageDraw.Draw(icon)
    mark(d, (0, 0, 1024), [WHITE] * 4, PURPLE)
    icon.save(OUT / "xpersiva-app-icon.png", optimize=True)

    android_fg = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    d = ImageDraw.Draw(android_fg)
    mark(d, (137, 137, 750), [WHITE] * 4, (0, 0, 0, 0))
    android_fg.save(OUT / "xpersiva-android-foreground.png", optimize=True)
    android_fg.save(OUT / "xpersiva-android-monochrome.png", optimize=True)
    Image.new("RGB", (1024, 1024), PURPLE).save(OUT / "xpersiva-android-background.png", optimize=True)

    splash = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    d = ImageDraw.Draw(splash)
    mark(d, (0, 0, 1024), [PURPLE, PURPLE_DEEP, PURPLE_DEEP, PURPLE], (0, 0, 0, 0))
    splash.save(OUT / "xpersiva-splash-icon.png", optimize=True)

    transparent = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    d = ImageDraw.Draw(transparent)
    mark(d, (0, 0, 1024), [PURPLE, PURPLE_DEEP, PURPLE_DEEP, PURPLE] , (0, 0, 0, 0))
    transparent.save(OUT / "xpersiva-symbol.png", optimize=True)

    word = Image.new("RGBA", (2000, 520), (0, 0, 0, 0))
    d = ImageDraw.Draw(word)
    mark(d, (0, 15, 490), [PURPLE, PURPLE_DEEP, PURPLE_DEEP, PURPLE], (0, 0, 0, 0))
    d.text((485, 105), "xpersiva", font=font(247, True), fill=NAVY)
    word.save(OUT / "xpersiva-wordmark.png", optimize=True)

    svg = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" role="img" aria-label="Símbolo Xpersiva">
<path fill="#6D4AFF" d="M90 90h80l86 120-46 46L90 170zM302 256l120 86v80h-80l-86-120z"/>
<path fill="#4C31CC" d="M342 90h80v80L302 256l-46-46zM90 342l120-86 46 46-86 120H90z"/>
</svg>'''
    (OUT / "xpersiva-symbol.svg").write_text(svg, encoding="utf-8")
    word_svg = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1500 400" role="img" aria-label="Xpersiva">
<g transform="translate(0 -56) scale(.9)"><path fill="#6D4AFF" d="M90 90h80l86 120-46 46L90 170zM302 256l120 86v80h-80l-86-120z"/><path fill="#4C31CC" d="M342 90h80v80L302 256l-46-46zM90 342l120-86 46 46-86 120H90z"/></g>
<text x="425" y="294" fill="#0D0F20" font-family="Segoe UI, Arial, sans-serif" font-size="235" font-weight="700" letter-spacing="-5">xpersiva</text>
</svg>'''
    (OUT / "xpersiva-wordmark.svg").write_text(word_svg, encoding="utf-8")


def ease(v):
    v = max(0.0, min(1.0, v))
    return 1 - (1 - v) ** 3


def alpha_text(base, xy, value, fnt, color, alpha, centered=True, spacing=0):
    if alpha <= 0:
        return
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    fill = (*color, int(255 * min(1, alpha)))
    if centered:
        text_center(d, xy, value, fnt, fill, spacing)
    else:
        d.text(xy, value, font=fnt, fill=fill)
    base.alpha_composite(overlay)


def frame(t, w=1080, h=1920):
    im = Image.new("RGBA", (w, h), (*NAVY, 255))
    d = ImageDraw.Draw(im)
    cx = w / 2
    # Restrained ambient glow behind the main action.
    for radius in range(500, 120, -16):
        blend = .025 + .045 * (500 - radius) / 380
        glow = tuple(round(NAVY[k] * (1-blend) + PURPLE[k] * blend) for k in range(3))
        d.ellipse((cx-radius, 735-radius, cx+radius, 735+radius), fill=(*glow, 255))

    if t < 2.65:
        a = ease(t / .55)
        pulse = 1 + .025 * math.sin(t * 5)
        s = 510 * pulse
        left, top, right, bottom = cx-s/2, 670-s/2, cx+s/2, 670+s/2
        line = int(45 * ease(t / .6))
        c = PURPLE_LIGHT
        for x0, y0, x1, y1 in [
            (left,top,left+130,top),(left,top,left,top+130),
            (right-130,top,right,top),(right,top,right,top+130),
            (left,bottom-130,left,bottom),(left,bottom,left+130,bottom),
            (right,bottom-130,right,bottom),(right,bottom,right-130,bottom),
        ]:
            d.line((x0,y0,x1,y1), fill=c, width=14)
        # QR blocks are a graphic cue, not a scannable code.
        for x0, y0, ss in [(cx-115,555,80),(cx+25,555,80),(cx-115,695,80),(cx+30,700,55),(cx+110,770,28)]:
            d.rounded_rectangle((x0,y0,x0+ss,y0+ss), radius=8, fill=WHITE)
        scan_y = 430 + (t % 1.3) / 1.3 * 480
        d.line((cx-255,scan_y,cx+255,scan_y), fill=PURPLE_LIGHT, width=7)
        alpha_text(im,(cx,1020),"ESCANEA",font(96,True),WHITE,a,spacing=7)
        alpha_text(im,(cx,1158),"el QR del evento",font(45),MUTED,a)

    elif t < 5.4:
        u = t - 2.65
        a = ease(u / .55)
        # A single phone acts as the controller, following the actual product description.
        d.rounded_rectangle((cx-260,350,cx+260,1030), radius=70, fill=(39,39,63), outline=PURPLE_LIGHT, width=12)
        d.rounded_rectangle((cx-229,397,cx+229,965), radius=35, fill=(22,23,46))
        d.rounded_rectangle((cx-66,374,cx+66,393), radius=10, fill=NAVY)
        rr = 130 + 7*math.sin(u*5)
        d.ellipse((cx-rr,670-rr,cx+rr,670+rr), outline=PURPLE_LIGHT, width=13)
        d.ellipse((cx-80,590,cx+80,750), fill=PURPLE)
        d.polygon([(cx-28,622),(cx-28,717),(cx+57,670)],fill=WHITE)
        for bx,by in [(cx-144,854),(cx+144,854)]:
            d.ellipse((bx-35,by-35,bx+35,by+35), fill=PURPLE_DEEP)
        alpha_text(im,(cx,1110),"ENTRA EN EL JUEGO",font(75,True),WHITE,a,spacing=2)
        alpha_text(im,(cx,1225),"Tu móvil es el mando",font(45),MUTED,a)
    else:
        u = t - 5.4
        a = ease(u / .65)
        size = 700 * (0.91 + .09*ease(u/.8))
        x0, y0 = cx-size/2, 565-size/2
        mark(d,(x0,y0,size),[PURPLE_LIGHT,PURPLE,PURPLE,PURPLE_LIGHT],NAVY)
        alpha_text(im,(cx,1005),"xpersiva",font(150,True),WHITE,a)
        alpha_text(im,(cx,1202),"EXPERIENCIAS INMERSIVAS",font(49,True),PURPLE_LIGHT,a,spacing=3)
        alpha_text(im,(cx,1288),"EN EVENTOS",font(49,True),PURPLE_LIGHT,a,spacing=3)
        alpha_text(im,(cx,1490),"Escanea. Entra. Juega.",font(48),MUTED,a)

    d = ImageDraw.Draw(im)
    d.rounded_rectangle((80,h-145,w-80,h-135),radius=5,fill=(52,48,80))
    d.rounded_rectangle((80,h-145,80+(w-160)*min(t/8.7,1),h-135),radius=5,fill=PURPLE)
    return im.convert("RGB")


def make_audio(seconds=8.7, sr=44100):
    n = int(seconds*sr)
    y = np.zeros(n, dtype=np.float32)
    # Soft synthetic pulses, intentionally unobtrusive under a voiceover.
    for start, freq, length, amp in [(0.15,220,.42,.12),(2.7,330,.5,.13),(5.5,440,.75,.16),(6.0,660,.7,.10)]:
        i = int(start*sr)
        m = min(int(length*sr), n-i)
        tt = np.arange(m)/sr
        env = np.sin(np.pi*np.arange(m)/m)**2
        y[i:i+m] += amp*np.sin(2*np.pi*freq*tt)*env
    pcm = np.clip(y*32767,-32768,32767).astype('<i2')
    with wave.open(str(OUT/"xpersiva-intro-audio.wav"),"wb") as wav:
        wav.setnchannels(1); wav.setsampwidth(2); wav.setframerate(sr); wav.writeframes(pcm.tobytes())


def export_video():
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    raw_path = OUT/"xpersiva-intro-silent.mp4"
    fps, seconds, w, h = 24, 8.7, 1080, 1920
    command = [ffmpeg,"-y","-f","rawvideo","-vcodec","rawvideo","-pix_fmt","rgb24",
               "-s",f"{w}x{h}","-r",str(fps),"-i","-","-an","-c:v","libx264",
               "-preset","veryfast","-crf","21","-pix_fmt","yuv420p","-movflags","+faststart",str(raw_path)]
    proc = subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    for i in range(round(fps*seconds)):
        pic = frame(i/fps,w,h)
        if i in [0,48,96,144,192]:
            pic.save(OUT/f"preview-{i//fps:02d}s.png")
        proc.stdin.write(pic.tobytes())
    proc.stdin.close()
    err = proc.stderr.read().decode(errors="replace")
    if proc.wait() != 0:
        raise RuntimeError(err[-3000:])
    make_audio(seconds)
    output = OUT/"xpersiva-intro-vertical.mp4"
    cmd = [ffmpeg,"-y","-i",str(raw_path),"-i",str(OUT/"xpersiva-intro-audio.wav"),
           "-c:v","copy","-c:a","aac","-b:a","128k","-shortest","-movflags","+faststart",str(output)]
    subprocess.run(cmd,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    raw_path.unlink()
    (OUT/"xpersiva-intro-audio.wav").unlink()
    frame(7.8).save(OUT / "xpersiva-video-cover.png", optimize=True)


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    export_static()
    export_video()
    print("Generated Xpersiva brand assets in", OUT)
