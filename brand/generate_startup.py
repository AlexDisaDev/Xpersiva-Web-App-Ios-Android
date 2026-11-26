"""A quiet, layered startup animation for Xpersiva Portal."""

from __future__ import annotations

import subprocess
from pathlib import Path

import imageio_ffmpeg
from PIL import Image, ImageDraw

from generate import ease, font
from generate_v2 import INK, VIOLET, CYAN, LILAC, WHITE, MUTED, centered_text, portal, x_mark


OUT = Path(__file__).resolve().parent / 'v2-portal'
WIDTH, HEIGHT, FPS, SECONDS = 1080, 1920, 24, 3.4


def reveal(t, start, duration):
    return ease((t-start)/duration)


def put(base, layer, opacity):
    if opacity <= 0:
        return
    if opacity < 1:
        layer.putalpha(layer.getchannel('A').point(lambda a: int(a*opacity)))
    base.alpha_composite(layer)


def frame(t):
    base=Image.new('RGBA',(WIDTH,HEIGHT),(*INK,255))
    cx,cy=WIDTH/2,650
    glow=reveal(t,.15,1.1)
    if glow:
        stage=Image.new('RGBA',(WIDTH,HEIGHT),(0,0,0,0))
        d=ImageDraw.Draw(stage)
        for r in range(500,180,-18):
            a=round(3+6*(500-r)/320)
            d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=(*VIOLET,a))
        put(base,stage,glow)

    symbol_in=reveal(t,.28,.85)
    if symbol_in:
        render_scale=3
        side=760*render_scale
        square=Image.new('RGBA',(side,side),(0,0,0,0))
        d=ImageDraw.Draw(square)
        portal(d,side/2,side/2,290*render_scale*(.90+.10*symbol_in),17*render_scale,phase=-24)
        x_mark(d,side/2,side/2,650*render_scale*(.92+.08*symbol_in))
        spin=ease((t-1.02)/1.38)
        square=square.rotate(-720*spin,resample=Image.Resampling.BICUBIC)
        square=square.resize((760,760),Image.Resampling.LANCZOS)
        layer=Image.new('RGBA',(WIDTH,HEIGHT),(0,0,0,0))
        layer.alpha_composite(square,(round(cx-380),round(cy-380)))
        put(base,layer,symbol_in)

    name_in=reveal(t,.95,.85)
    if name_in:
        layer=Image.new('RGBA',(WIDTH,HEIGHT),(0,0,0,0))
        d=ImageDraw.Draw(layer)
        centered_text(d,1075,'xpersiva',font(152,True),WHITE,WIDTH)
        put(base,layer,name_in)

    line_in=reveal(t,1.5,.7)
    if line_in:
        layer=Image.new('RGBA',(WIDTH,HEIGHT),(0,0,0,0))
        d=ImageDraw.Draw(layer)
        d.rounded_rectangle((cx-65,1253,cx+65,1260),radius=3,fill=CYAN)
        centered_text(d,1315,'VIVE EL EVENTO',font(55,True),LILAC,WIDTH,tracking=3)
        centered_text(d,1394,'DESDE DENTRO',font(55,True),LILAC,WIDTH,tracking=3)
        put(base,layer,line_in)

    copy_in=reveal(t,1.95,.55)
    if copy_in:
        layer=Image.new('RGBA',(WIDTH,HEIGHT),(0,0,0,0))
        d=ImageDraw.Draw(layer)
        centered_text(d,1580,'Escanea  ·  Entra  ·  Juega',font(45),MUTED,WIDTH)
        put(base,layer,copy_in)
    if t>=2.55:
        clean_background=Image.new('RGB',(WIDTH,HEIGHT),INK)
        return Image.blend(base.convert('RGB'),clean_background,reveal(t,2.55,.55))
    return base.convert('RGB')


def render():
    OUT.mkdir(exist_ok=True)
    output=OUT/'xpersiva-intro-para-app-suave.mp4'
    ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
    cmd=[ffmpeg,'-y','-f','rawvideo','-vcodec','rawvideo','-pix_fmt','rgb24',
         '-s',f'{WIDTH}x{HEIGHT}','-r',str(FPS),'-i','-','-an','-c:v','libx264',
         '-preset','medium','-crf','16','-pix_fmt','yuv420p','-movflags','+faststart',str(output)]
    proc=subprocess.Popen(cmd,stdin=subprocess.PIPE,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    for i in range(round(FPS*SECONDS)):
        t=i/FPS
        pic=frame(t)
        if i in [0,12,24,36,48,60,72]:
            pic.save(OUT/f'giro-preview-{i//12:02d}.png',optimize=True)
        proc.stdin.write(pic.tobytes())
    proc.stdin.close();err=proc.stderr.read().decode(errors='replace')
    if proc.wait()!=0: raise RuntimeError(err[-3000:])
    print(output)


if __name__=='__main__':
    render()
