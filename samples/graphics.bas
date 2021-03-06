10 SCREEN "PuffinBASIC 2D Graphics", 800, 600
20 LINE (100, 100) - (200, 200), "B"
30 FOR I% = 10 TO 50 STEP 10
40   CIRCLE (150, 150), I%, I%
50 NEXT I%
60 COLOR 255, 0, 0
70 LINE (200, 200) - (250, 300), "BF"
80 COLOR 0, 255, 255
90 FONT "Georgia", "bi", 32
100 DRAWSTR "Graphics with PuffinBASIC", 10, 400
110 DIM A%(101, 101)
120 GET (100, 100) - (201, 201), A%
130 PUT (250, 250), A%
140 DIM B%(32, 32)
150 LOADIMG "samples/enemy1.png", B%
160 FOR I% = 1 TO 5
170   PUT (400, 100 * I%), B%
180 NEXT
190 COLOR 255, 255, 0
200 DRAW "M600,400; UN50; RN50; DB50; F100"
210 COLOR 255, 255, 255
220 CIRCLE (700, 100), 10, 20
230 COLOR 255, 0, 255
240 PAINT (700, 100), 255, 255, 255
250 CIRCLE (700, 400), 50, 50, 0, 90
260 CIRCLE (700, 500), 50, 50, 90, 180, "F"
1000 SLEEP 5000
