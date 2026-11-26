"""Render design-only F1 controller mockups for Xpersiva."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


OUT = Path(__file__).resolve().parent
S = 2  # Supersampling keeps arcs, type and controls smooth.
BG = '#090B1E'
PANEL = '#171B35'
PANEL_EDGE = '#303755'
WHITE = '#F7F8FF'
MUTED = '#A7AEC8'
VIOLET = '#6D4AFF'
LILAC = '#A993FF'
CYAN = '#42E3F2'
GAS = '#39DEB4'
BRAKE = '#FF6878'


def fnt(size: int, bold: bool = False):
    windows = Path('C:/Windows/Fonts/segoeuib.ttf' if bold else 'C:/Windows/Fonts/segoeui.ttf')
    fallback = 'DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf'
    return ImageFont.truetype(str(windows) if windows.exists() else fallback, size * S)


class Canvas:
    def __init__(self, w: int, h: int):
        self.w, self.h = w, h
        self.image = Image.new('RGB', (w*S, h*S), BG)
        self.d = ImageDraw.Draw(self.image)
        self.glow(w*.50, h*.47, min(w,h)*.52, '#151632')

    def b(self, box):
        return tuple(round(v*S) for v in box)

    def glow(self, x, y, radius, inner):
        base=(9,11,30); target=tuple(int(inner[i:i+2],16) for i in (1,3,5))
        for r in range(round(radius), 0, -12):
            strength=.012 + .018 * (radius-r)/radius
            color=tuple(round(base[k]*(1-strength)+target[k]*strength) for k in range(3))
            self.d.ellipse(self.b((x-r,y-r,x+r,y+r)),fill=color)

    def rect(self, box, radius, fill, outline=None, width=1):
        self.d.rounded_rectangle(self.b(box),radius=radius*S,fill=fill,outline=outline,width=width*S)

    def line(self, points, fill, width=1):
        self.d.line([(round(x*S),round(y*S)) for x,y in points],fill=fill,width=width*S,joint='curve')

    def circle(self, x, y, radius, fill=None, outline=None, width=1):
        self.d.ellipse(self.b((x-radius,y-radius,x+radius,y+radius)),fill=fill,outline=outline,width=width*S)

    def arc(self, x, y, radius, start, end, color, width):
        self.d.arc(self.b((x-radius,y-radius,x+radius,y+radius)),start,end,fill=color,width=width*S)

    def text(self, x, y, value, size, color=WHITE, bold=False, anchor='lt'):
        self.d.text((round(x*S),round(y*S)),value,font=fnt(size,bold),fill=color,anchor=anchor)

    def centered(self, x, y, value, size, color=WHITE, bold=False):
        self.text(x,y,value,size,color,bold,anchor='mt')

    def polygon(self, points, fill):
        self.d.polygon([(round(x*S),round(y*S)) for x,y in points],fill=fill)

    def save(self, name):
        self.image.resize((self.w,self.h),Image.Resampling.LANCZOS).save(OUT/name,optimize=True)


def brand(c: Canvas, x, y):
    # Small Xpersiva signature, separated from the game controls.
    c.arc(x+29,y+29,28,204,318,CYAN,3)
    c.arc(x+29,y+29,28,24,138,VIOLET,3)
    c.line([(x+14,y+13),(x+44,y+45)],LILAC,10)
    c.line([(x+44,y+13),(x+14,y+45)],VIOLET,10)
    c.text(x+77,y+2,'xpersiva',36,WHITE,True)


def status(c: Canvas, x, y):
    c.rect((x,y,x+220,y+56),28,'#132C2B',outline='#265B53',width=2)
    c.circle(x+29,y+28,7,GAS)
    c.text(x+49,y+15,'CONECTADO',22,'#CCF9EA',True)


def pedal(c: Canvas, box, label, subtitle, color, gas=False):
    x0,y0,x1,y1=box
    c.rect(box,42,PANEL,outline=PANEL_EDGE,width=2)
    c.rect((x0+18,y0+18,x1-18,y0+27),5,color)
    c.centered((x0+x1)/2,y0+67,label,48,WHITE,True)
    c.centered((x0+x1)/2,y0+130,subtitle,24,MUTED)
    cx=(x0+x1)/2; cy=(y0+y1)/2+35
    if gas:
        c.rect((cx-73,cy-138,cx+73,cy+105),30,'#1D3C3F',outline=color,width=5)
        for yy in (cy-65,cy-20,cy+25):
            c.line([(cx-39,yy),(cx+39,yy)],color,8)
    else:
        c.rect((cx-104,cy-96,cx+104,cy+90),29,'#422A3D',outline=color,width=5)
        for yy in (cy-42,cy+3,cy+48):
            c.line([(cx-63,yy),(cx+63,yy)],color,8)
    c.centered(cx,y1-86,'MANTENER PULSADO',22,color,True)
    c.centered(cx,y1-55,'Suelta para dejar de '+('acelerar' if gas else 'frenar'),19,MUTED)


def wheel(c: Canvas, cx, cy, radius, compact=False):
    c.circle(cx,cy,radius,fill='#11162E',outline='#30385F',width=5)
    c.arc(cx,cy,radius-12,205,265,VIOLET,14)
    c.arc(cx,cy,radius-12,275,335,CYAN,14)
    for degrees in range(205,336,13):
        angle=math.radians(degrees)
        r1=radius-38; r2=radius-27
        c.line([(cx+r1*math.cos(angle),cy+r1*math.sin(angle)),
                (cx+r2*math.cos(angle),cy+r2*math.sin(angle))],MUTED,3)
    c.line([(cx-radius*.47,cy),(cx+radius*.47,cy)],'#566189',5)
    c.circle(cx,cy,67,fill='#272351',outline=VIOLET,width=4)
    c.line([(cx-31,cy-31),(cx+31,cy+31)],LILAC,12)
    c.line([(cx+31,cy-31),(cx-31,cy+31)],VIOLET,12)
    c.text(cx-radius*.70,cy-17,'←',32,CYAN,True,anchor='mt')
    c.text(cx+radius*.70,cy-17,'→',32,CYAN,True,anchor='mt')
    c.centered(cx,cy+95 if not compact else cy+84,'CENTRADO',23,GAS,True)


def landscape():
    c=Canvas(1920,1080)
    brand(c,72,58)
    status(c,838,59)
    c.text(1730,70,'SALIR  ×',24,MUTED,True)
    c.line([(72,151),(1848,151)],'#283050',2)

    pedal(c,(70,217,476,865),'FRENO','Pulgar izquierdo',BRAKE)
    pedal(c,(1444,217,1850,865),'ACELERAR','Pulgar derecho',GAS,gas=True)

    c.centered(960,205,'INCLINA EL MÓVIL PARA GIRAR',27,CYAN,True)
    c.centered(960,252,'TU MÓVIL ES EL VOLANTE',43,WHITE,True)
    wheel(c,960,573,236)
    c.rect((812,858,1108,925),22,'#211B45',outline='#6555BA',width=2)
    c.centered(960,876,'CENTRAR VOLANTE',24,LILAC,True)
    c.centered(960,981,'Inclina para dirigir  ·  Usa los pedales con los pulgares',22,MUTED)
    c.save('mando-f1-horizontal.png')


def portrait():
    c=Canvas(1080,1920)
    brand(c,64,72)
    status(c,785,75)
    c.line([(62,169),(1018,169)],'#283050',2)
    c.centered(540,244,'INCLINA PARA GIRAR',30,CYAN,True)
    c.centered(540,291,'Tu móvil es el volante',45,WHITE,True)
    wheel(c,540,686,280,compact=True)
    c.rect((350,1030,730,1110),26,'#211B45',outline='#6555BA',width=2)
    c.centered(540,1050,'CENTRAR VOLANTE',27,LILAC,True)
    pedal(c,(57,1190,514,1735),'FRENO','Pulgar izquierdo',BRAKE)
    pedal(c,(566,1190,1023,1735),'ACELERAR','Pulgar derecho',GAS,gas=True)
    c.centered(540,1810,'Gira el móvil  ·  Mantén pulsado gas o freno',22,MUTED)
    c.save('mando-f1-vertical.png')


if __name__=='__main__':
    OUT.mkdir(exist_ok=True)
    landscape()
    portrait()
    print('Design previews created in',OUT)
