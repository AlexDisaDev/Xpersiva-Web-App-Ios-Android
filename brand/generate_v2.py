"""Xpersiva Portal: a bolder app identity and launch teaser."""

from __future__ import annotations

import math
import subprocess
import wave
from pathlib import Path

import imageio_ffmpeg
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from generate import frame as original_frame, font, ease


OUT = Path(__file__).resolve().parent / "v2-portal"
INK = (9, 11, 30)
VIOLET = (109, 74, 255)
LILAC = (169, 147, 255)
CYAN = (66, 227, 242)
WHITE = (255, 255, 255)
MUTED = (192, 197, 220)

ARMS = [
    [(90, 90), (170, 90), (256, 210), (210, 256), (90, 170)],
    [(342, 90), (422, 90), (422, 170), (302, 256), (256, 210)],
    [(90, 342), (210, 256), (256, 302), (170, 422), (90, 422)],
    [(302, 256), (422, 342), (422, 422), (342, 422), (256, 302)],
]


def centered_text(draw, y, text, fnt, fill, width, tracking=0):
    spans = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(spans) + max(0, len(text)-1) * tracking
    x = (width-total)/2
    for ch, span in zip(text, spans):
        draw.text((x,y),ch,font=fnt,fill=fill)
        x += span + tracking


def x_mark(draw, cx, cy, size, colors=(LILAC, VIOLET, VIOLET, CYAN)):
    for poly, color in zip(ARMS, colors):
        draw.polygon([(cx+(px-256)*size/512,cy+(py-256)*size/512) for px,py in poly],fill=color)


def portal(draw, cx, cy, radius, width, phase=0, intensity=1):
    # Four short arcs keep the silhouette clear when the icon is reduced.
    base = (83, 73, 150)
    draw.arc((cx-radius,cy-radius,cx+radius,cy+radius),0,360,fill=base,width=max(2,int(width*.32)))
    for offset, color in [(0,CYAN),(90,VIOLET),(180,CYAN),(270,VIOLET)]:
        start = offset + phase
        draw.arc((cx-radius,cy-radius,cx+radius,cy+radius),start,start+47,fill=color,width=width)


def icon(size=1024):
    yy,xx=np.mgrid[0:size,0:size]
    dist=np.sqrt(((xx-size*.5)/(size*.74))**2+((yy-size*.47)/(size*.74))**2)
    glow=np.maximum(0,1-dist)**2
    base=np.zeros((size,size,3),dtype=np.uint8)
    for k in range(3):
        base[:,:,k]=np.clip(INK[k]+glow*(VIOLET[k]-INK[k])*.34,0,255)
    im=Image.fromarray(base,'RGB')
    d=ImageDraw.Draw(im)
    cx=cy=size/2
    portal(d,cx,cy,size*.315,int(size*.019),phase=-24)
    x_mark(d,cx,cy,size*.78)
    return im


def static_assets():
    OUT.mkdir(exist_ok=True)
    icon().save(OUT/'xpersiva-portal-app-icon.png',optimize=True)
    Image.new('RGB',(1024,1024),INK).save(OUT/'xpersiva-portal-android-background.png',optimize=True)
    startup=Image.new('RGBA',(1024,1024),(0,0,0,0))
    sd=ImageDraw.Draw(startup)
    portal(sd,512,512,322,18,phase=-24)
    x_mark(sd,512,512,800)
    startup.save(OUT/'xpersiva-portal-startup-symbol.png',optimize=True)

    # Android adaptive foreground is intentionally smaller to remain in its safe circle.
    fg=Image.new('RGBA',(1024,1024),(0,0,0,0))
    d=ImageDraw.Draw(fg)
    portal(d,512,512,275,17,phase=-24)
    x_mark(d,512,512,690,(WHITE,WHITE,WHITE,WHITE))
    fg.save(OUT/'xpersiva-portal-android-foreground.png',optimize=True)
    mono=Image.new('RGBA',(1024,1024),(0,0,0,0))
    d=ImageDraw.Draw(mono)
    x_mark(d,512,512,690,(WHITE,WHITE,WHITE,WHITE))
    mono.save(OUT/'xpersiva-portal-android-monochrome.png',optimize=True)

    lockup=Image.new('RGBA',(1800,530),(0,0,0,0))
    d=ImageDraw.Draw(lockup)
    portal(d,250,265,192,12,phase=-24)
    x_mark(d,250,265,470)
    d.text((490,112),'xpersiva',font=font(245,True),fill=INK)
    lockup.save(OUT/'xpersiva-portal-wordmark-light.png',optimize=True)
    dark=Image.new('RGB',(1800,530),INK)
    d=ImageDraw.Draw(dark)
    portal(d,250,265,192,12,phase=-24)
    x_mark(d,250,265,470)
    d.text((490,112),'xpersiva',font=font(245,True),fill=WHITE)
    dark.save(OUT/'xpersiva-portal-wordmark-dark.png',optimize=True)

    svg='''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" role="img" aria-label="Xpersiva Portal">
<defs><linearGradient id="xp" x1="0" x2="1" y1="0" y2="1"><stop stop-color="#A993FF"/><stop offset=".55" stop-color="#6D4AFF"/><stop offset="1" stop-color="#42E3F2"/></linearGradient></defs>
<circle cx="256" cy="256" r="161" fill="none" stroke="#534996" stroke-width="5"/>
<g fill="none" stroke-width="10" stroke-linecap="round"><path stroke="#42E3F2" d="M406 196a161 161 0 0 1-16 116"/><path stroke="#6D4AFF" d="M316 406a161 161 0 0 1-116-16"/><path stroke="#42E3F2" d="M106 316a161 161 0 0 1 16-116"/><path stroke="#6D4AFF" d="M196 106a161 161 0 0 1 116 16"/></g>
<g transform="translate(56 56) scale(.78)"><path fill="#A993FF" d="M90 90h80l86 120-46 46L90 170z"/><path fill="#6D4AFF" d="M342 90h80v80L302 256l-46-46zM90 342l120-86 46 46-86 120H90z"/><path fill="#42E3F2" d="M302 256l120 86v80h-80l-86-120z"/></g>
</svg>'''
    (OUT/'xpersiva-portal-symbol.svg').write_text(svg,encoding='utf-8')
    app_concept()


def app_concept():
    """A design preview showing the mark in a plausible mobile home screen."""
    w,h=1080,1920
    im=Image.new('RGB',(w,h),(247,248,252))
    d=ImageDraw.Draw(im)
    d.rectangle((0,0,w,620),fill=INK)
    for radius in range(200,70,-10):
        blend=.025+.045*(200-radius)/130
        col=tuple(round(INK[k]*(1-blend)+VIOLET[k]*blend) for k in range(3))
        d.ellipse((750-radius,270-radius,750+radius,270+radius),fill=col)
    small=icon(130)
    im.paste(small,(65,105))
    d.text((225,125),'xpersiva',font=font(67,True),fill=WHITE)
    d.text((72,300),'EXPERIENCIAS INMERSIVAS',font=font(31,True),fill=CYAN)
    d.text((72,356),'El evento empieza',font=font(75,True),fill=WHITE)
    d.text((72,449),'contigo.',font=font(75,True),fill=WHITE)
    d.rounded_rectangle((55,575,1025,695),radius=30,fill=WHITE)
    d.ellipse((95,610,142,657),outline=(112,107,156),width=6)
    d.line((135,651,157,673),fill=(112,107,156),width=6)
    d.text((190,603),'Busca una experiencia o ciudad',font=font(38),fill=(106,111,130))
    d.text((72,758),'Explora experiencias',font=font(58,True),fill=INK)
    d.rounded_rectangle((72,850,365,923),radius=36,fill=VIOLET)
    d.text((122,864),'Cerca de mí',font=font(34,True),fill=WHITE)
    d.rounded_rectangle((385,850,655,923),radius=36,fill=(234,230,255))
    d.text((438,864),'En directo',font=font(34,True),fill=VIOLET)
    # Card one: visual language for event discovery, not a real listed event.
    d.rounded_rectangle((72,975,1008,1375),radius=32,fill=WHITE)
    d.rounded_rectangle((72,975,1008,1195),radius=32,fill=(39,30,91))
    for rr in range(102,35,-8):
        blend=.035+.095*(102-rr)/67
        col=tuple(round(39*(1-blend)+VIOLET[k]*blend) for k in range(3))
        d.ellipse((805-rr,1080-rr,805+rr,1080+rr),fill=col)
    portal(d,805,1080,90,7,phase=-24)
    x_mark(d,805,1080,205)
    d.text((112,1017),'TU PRÓXIMA',font=font(34,True),fill=CYAN)
    d.text((112,1060),'EXPERIENCIA',font=font(57,True),fill=WHITE)
    d.text((112,1225),'Descubre lo que pasa cerca',font=font(45,True),fill=INK)
    d.text((112,1292),'Eventos, juegos y mundos por explorar',font=font(32),fill=(108,112,132))
    d.rounded_rectangle((72,1412,1008,1680),radius=32,fill=WHITE)
    d.ellipse((110,1468,260,1618),fill=(235,230,255))
    x_mark(d,185,1543,175)
    d.text((295,1470),'Escanea y entra',font=font(47,True),fill=INK)
    d.text((295,1530),'Tu móvil se convierte en el mando',font=font(32),fill=(108,112,132))
    d.rounded_rectangle((72,1740,1008,1850),radius=31,fill=VIOLET)
    centered_text(d,1757,'ESCANEAR QR',font(52,True),WHITE,w,tracking=2)
    im.save(OUT/'xpersiva-app-concept.png',optimize=True)


def finale(t,w=1080,h=1920):
    im=Image.new('RGB',(w,h),INK)
    d=ImageDraw.Draw(im)
    u=t-5.35
    a=ease(u/.8)
    cx=w/2; cy=650
    # Layered stage lighting, with motion restrained around the identity.
    for r in range(550,180,-20):
        blend=.018+.055*(550-r)/370
        color=tuple(round(INK[k]*(1-blend)+VIOLET[k]*blend) for k in range(3))
        d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=color)
    # Subtle particles create depth without weakening the logo.
    for i in range(26):
        angle=i*2.399+u*.18
        rr=310+(i%5)*64
        px=cx+rr*math.cos(angle)
        py=cy+rr*.72*math.sin(angle)
        rad=2+(i%4)
        col=CYAN if i%4==0 else (83,71,151)
        d.ellipse((px-rad,py-rad,px+rad,py+rad),fill=col)
    radius=315*(.78+.22*a)
    portal(d,cx,cy,radius,18,phase=-24+u*11)
    x_mark(d,cx,cy,700*(.84+.16*a))
    # Brand typography echoes the existing finale, now with a more useful message.
    def fade(color, amount):
        return tuple(round(INK[k]*(1-amount)+color[k]*amount) for k in range(3))
    name_in=ease((u-.34)/.55)
    line_in=ease((u-.7)/.55)
    copy_in=ease((u-1.0)/.6)
    centered_text(d,1050,'xpersiva',font(152,True),fade(WHITE,name_in),w)
    d.rounded_rectangle((cx-67,1230,cx+67,1237),radius=3,fill=fade(CYAN,line_in))
    centered_text(d,1290,'VIVE EL EVENTO',font(55,True),fade(LILAC,line_in),w,tracking=3)
    centered_text(d,1370,'DESDE DENTRO',font(55,True),fade(LILAC,line_in),w,tracking=3)
    centered_text(d,1560,'Escanea  ·  Entra  ·  Juega',font(45),fade(MUTED,copy_in),w)
    return im


def audio(seconds=8.7,sr=44100):
    n=round(seconds*sr); y=np.zeros(n,dtype=np.float32)
    for start,freq,length,amp in [(0.2,220,.5,.09),(2.68,330,.55,.10),(5.38,220,1.4,.13),(5.55,440,1.3,.10),(5.72,660,1.1,.07)]:
        i=round(start*sr); m=min(round(length*sr),n-i)
        tt=np.arange(m)/sr; env=np.sin(np.pi*np.arange(m)/m)**2
        y[i:i+m]+=amp*np.sin(2*np.pi*freq*tt)*env
    pcm=np.clip(y*32767,-32768,32767).astype('<i2')
    wav=OUT/'xpersiva-portal-audio.wav'
    with wave.open(str(wav),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(sr);f.writeframes(pcm.tobytes())
    return wav


def video():
    ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
    silent=OUT/'xpersiva-portal-silent.mp4'
    w,h,fps,seconds=1080,1920,24,8.7
    cmd=[ffmpeg,'-y','-f','rawvideo','-vcodec','rawvideo','-pix_fmt','rgb24','-s',f'{w}x{h}',
         '-r',str(fps),'-i','-','-an','-c:v','libx264','-preset','veryfast','-crf','19',
         '-pix_fmt','yuv420p','-movflags','+faststart',str(silent)]
    proc=subprocess.Popen(cmd,stdin=subprocess.PIPE,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    for i in range(round(fps*seconds)):
        t=i/fps
        if t<5.35:
            pic=original_frame(t,w,h)
        elif t<5.8:
            pic=Image.blend(original_frame(5.25,w,h),finale(t,w,h),ease((t-5.35)/.45))
        else:
            pic=finale(t,w,h)
        if i in [96,144,168,192]:
            pic.save(OUT/f'preview-{i//fps:02d}s.png',optimize=True)
        proc.stdin.write(pic.tobytes())
    proc.stdin.close();err=proc.stderr.read().decode(errors='replace')
    if proc.wait()!=0: raise RuntimeError(err[-3000:])
    wav=audio(seconds)
    result=OUT/'xpersiva-portal-intro-vertical.mp4'
    subprocess.run([ffmpeg,'-y','-i',str(silent),'-i',str(wav),'-c:v','copy','-c:a','aac',
                    '-b:a','128k','-shortest','-movflags','+faststart',str(result)],
                   check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    silent.unlink();wav.unlink()
    finale(7.8).save(OUT/'xpersiva-portal-video-cover.png',optimize=True)


if __name__=='__main__':
    static_assets()
    video()
    print('Generated',OUT)
