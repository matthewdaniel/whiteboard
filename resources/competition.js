'use strict';

var config = require('./config');
var debug = require('debug')('awwapp:drawing');
var Queue = require('d3-queue');
var _ = require('../frontend/js/lodash.custom');
var ImageCache = require('./ImageCache');
var rotateCoordinates = require('./math').rotateCoordinates;

var MAX_DECIMAL_POINTS = config.MAX_DECIMAL_POINTS;


var Drawing = module.exports = function Drawing(context, Image, prepImage, isLoading, isOnBackend, options, lockContext, redrawTrigger = null) {
    isLoading = isLoading || function() {};
    var imageCache = new ImageCache(isOnBackend);

    var ctx, lockCtx, drawCtx;
    var MAX_CANVAS_DIMENSIONS = config.MAX_CANVAS_DIMENSIONS;
    var isPanningOrZooming = false;
    var firefoxCanvas, firefoxContext, isFirefox, isDesktopSafari;
    if (typeof navigator !== "undefined") {
        if (navigator.userAgent.indexOf('Firefox') !== -1) isFirefox = true;
        if (navigator.vendor &&
            navigator.vendor.indexOf('Apple') > -1 &&
            navigator.userAgent.indexOf('CriOS') === -1 &&
            navigator.userAgent.indexOf('FxiOS') === -1 &&
            navigator.userAgent.indexOf('Macintosh') > -1) isDesktopSafari = true;
        if (navigator.appVersion.indexOf('Win') !== -1 ||
            navigator.userAgent.indexOf('Firefox') !== -1 ||
            navigator.userAgent.indexOf('CrOS') !== -1) {
            firefoxCanvas = document.createElement('canvas');
            firefoxContext = firefoxCanvas.getContext('2d');
        }
    }

    var lineSmoothing = (!options || options.lineSmoothing === undefined) ? true : options.lineSmoothing;
    const imagePlaceholders = options && options.imagePlaceholders;
    const imagesInTransit = {}

    if (imagePlaceholders) {
        prepImage = (src, cb) => {
            if (src.substring(0, 5) === "data:") {
                if (src === PLACEHOLDER_IMAGE) console.log("OMG ASKING ABOUT PLACEHOLDER");
                return cb(null, src);
            }

            if (!imagesInTransit[src]) {
                imagesInTransit[src] = true;
                createImage(src, (err, img) => {
                    imagesInTransit[src] = false;
                    if (err) {
                        debug("Couldn't load image %s in background", src, err);
                        return;
                    }
                    imageCache.addToCache({img, src});
                    debug('Missing image loaded, triggering redraw', src);
                    if (redrawTrigger) redrawTrigger();
                });
            }

            return cb(null, imageCache.PLACEHOLDER_IMAGE);
        }
    } else {
        prepImage = prepImage || function(src, cb) { return cb(null, src); };
    }

    function setFirefoxCanvasSize(width, height) {
        if (!firefoxCanvas) return;
        if (width) firefoxCanvas.width = width;
        if (height) firefoxCanvas.height = height;
    }

    function setPanningZooming(zooming) {
        isPanningOrZooming = zooming;
    }

    function getPanningZooming() {
        return isPanningOrZooming;
    }

    function _prepCtx(op) {
        ctx.fillStyle = op.color || config.DEFAULT_COLOR;
        ctx.strokeStyle = op.color || config.DEFAULT_COLOR;
        ctx.lineWidth = op.size || config.DEFAULT_SIZES.draw;
        _setComposite(op.eraser && !op.text);
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
    }

    function _doDrawShape(op, erase, index, mousePos, selectCallback) {

        var firstPoint = op.points[0];

        if (op.points.length === 1) {
            ctx.beginPath();
            _prepCtx(op);
            ctx.arc(firstPoint[0], firstPoint[1],
                ctx.lineWidth / 2, 0, 2 * Math.PI, true);
            ctx.fill();
        } else {
            if (isPanningOrZooming || !lineSmoothing) {
                _prepCtx(op);
                ctx.beginPath();
                for (var i = 0; i < op.points.length - 1; i++) {
                    ctx.moveTo(op.points[i][0], op.points[i][1]);
                    ctx.lineTo(op.points[i + 1][0], op.points[i + 1][1]);
                }
                ctx.stroke();
            } else {
                ctx.beginPath();
                _prepCtx(op);
                ctx.moveTo(firstPoint[0], firstPoint[1]);

                var lastPoint = firstPoint;
                for (var j = 0; j < op.points.length; j++) {
                    var middle = _middle(lastPoint, op.points[j]);
                    ctx.quadraticCurveTo(lastPoint[0], lastPoint[1],
                        middle[0], middle[1]);
                    lastPoint = op.points[j];
                }
                ctx.stroke();

                if (op.arrowPoints) drawArrow(op.arrowPoints);
            }

            if (selectCallback) {
                if (ctx.isPointInStroke(mousePos[0], mousePos[1])) {
                    selectCallback(op, index);
                }
                return;
            }
            // _prepCtx(op);
            // ctx.stroke();
        }

    }

    function drawArrow(arrowPoints) {
        if (_.flatten(arrowPoints).includes(null) || _.flatten(arrowPoints).includes(undefined) || _.flatten(arrowPoints).includes(undefined)) return;
        ctx.beginPath();
        ctx.moveTo(arrowPoints[0][0], arrowPoints[0][1]);
        ctx.lineTo(arrowPoints[1][0], arrowPoints[1][1]);
        ctx.lineTo(arrowPoints[2][0], arrowPoints[2][1]);
        ctx.fill();
    }

    function drawEraserWithoutBeginningPath(op) {
        if (!draw.queue) draw.queue = Queue.queue(1);

        _prepCtx(op);

        if (op.points.length === 1) {
            var firstPoint = op.points[0];
            ctx.arc(firstPoint[0], firstPoint[1],
                ctx.lineWidth / 2, 0, 2 * Math.PI, true);
            ctx.fill();
        } else if (op.op === 'eraseArea') {
            ctx.moveTo(op.points[0][0], op.points[0][1]);
            for (var i = 1; i < op.points.length; i++) {
                ctx.lineTo(op.points[i][0], op.points[i][1]);
            }
            ctx.fill();
        } else {
            for (var j = 0; j < op.points.length - 1; j++) {
                ctx.moveTo(op.points[j][0], op.points[j][1]);
                ctx.lineTo(op.points[j + 1][0], op.points[j + 1][1]);
            }
            ctx.stroke();
        }

    }

    function _drawShape(op, index, mousePos, selectCallback) {
        if (!op.points || !op.points.length) return;

        if (draw.queue) {
            draw.queue = draw.queue.defer(function(cb) {
                _doDrawShape(op, undefined, index, mousePos, selectCallback);
                cb();
            });
        } else {
            _doDrawShape(op, undefined, index, mousePos, selectCallback);
        }
    }

    function _doDrawEllipse(op) {
        if (!op.x || !op.y || !op.r || op.scale.length !== 2) return;

        ctx.beginPath();
        _prepCtx(op);

        try {
            ctx.ellipse(op.x, op.y, op.r * op.scale[0], op.r * op.scale[1], 0, 0, 2 * Math.PI);
        } catch (e) {
            ctx.save();
            ctx.scale(op.scale[0], op.scale[1]);
            ctx.arc(op.x / op.scale[0], op.y / op.scale[1], op.r, 0, 2 * Math.PI, true);
            ctx.restore();
        }


        if (op.filled) {
            ctx.fill();
        } else {
            ctx.stroke();
        }
    }

    function _drawEllipse(op) {
        if (draw.queue) {
            draw.queue = draw.queue.defer(function(cb) {
                _doDrawEllipse(op);
                cb();
            });
        } else {
            _doDrawEllipse(op);
        }
    }

    function _doDrawStraightLine(op, index, mousePos, selectCallback) {
        if (op.points.length !== 2) return;

        ctx.beginPath();
        _prepCtx(op);

        ctx.moveTo(op.points[0][0], op.points[0][1]);
        ctx.lineTo(op.points[1][0], op.points[1][1]);

        if (selectCallback) {
            if (ctx.isPointInStroke(mousePos[0], mousePos[1])) {
                selectCallback(op, index);
            }
            return;
        }
        ctx.stroke();

        if (op.arrowPoints && !isPanningOrZooming) drawArrow(op.arrowPoints);
    }

    function _drawStraightLine(op, index, mousePos, selectCallback) {
        if (draw.queue) {
            draw.queue = draw.queue.defer(function(cb) {
                _doDrawStraightLine(op, index, mousePos, selectCallback);
                cb();
            });
        } else {
            _doDrawStraightLine(op, index, mousePos, selectCallback);
        }
    }

    function _doDrawNative(op) {
        var method = op.op;
        _prepCtx(op);
        ctx.beginPath();

        if (method === 'arc') {
            try {
                ctx.moveTo(op.x, op.y);
                ctx.arc(op.x, op.y, op.rad, op.start, op.end, !!op.clockwise);
            } catch (e) {}
            if (op.stroke) {
                ctx.stroke();
            } else {
                ctx.fill();
            }
        } else if (method === 'rectangle') {
            try {
                if (op.filled) {
                    ctx.fillRect(op.x, op.y, op.w, op.h);
                } else {
                    ctx.lineJoin = 'miter';
                    ctx.strokeRect(op.x, op.y, op.w, op.h);
                }
            } catch (e) {}
        } else if (method === 'quadratic') {
            try {
                ctx.moveTo(op.x0, op.y0);
                ctx.quadraticCurveTo(op.cpx, op.cpy, op.x, op.y);
                ctx.stroke();
            } catch (e) {}
        } else if (method === 'bezier') {
            try {
                ctx.moveTo(op.x0, op.y0);
                ctx.bezierCurveTo(op.cp1x, op.cp1y, op.cp2x, op.cp2y,
                                  op.x, op.y);
                ctx.stroke();
            } catch (e) {}
        }
    }

    function _drawNative(op) {
        if (draw.queue) {
            draw.queue = draw.queue.defer(function(cb) {
                _doDrawNative(op);
                cb();
            });
        } else {
            _doDrawNative(op);
        }
    }

    function _doEraseArea(op, index, mousePos, selectCallback) {
        _prepCtx(op);
        ctx.beginPath();
        ctx.moveTo(op.points[0][0], op.points[0][1]);
        for (var i = 1; i < op.points.length; i++) {
            ctx.lineTo(op.points[i][0], op.points[i][1]);
        }

        if (selectCallback) {
            if (ctx.isPointInPath(mousePos[0], mousePos[1])) {
                selectCallback(op, index);
            }
            return;
        }

        ctx.fill();
    }

    function _eraseArea(op, index, mousePos, selectCallback) {
        if (!op.points || !op.points.length || op.points.length < 2) {
            return;
        }

        if (draw.queue) {
            draw.queue = draw.queue.defer(function(cb) {
                _doEraseArea(op, index, mousePos, selectCallback);
                cb();
            });
        } else {
            _doEraseArea(op, index, mousePos, selectCallback);
        }
    }


    function _setComposite(isEraser) {
        ctx.globalCompositeOperation = isEraser ? 'destination-out' : 'source-over';
    }

    function _drawWrappedText(text, point, lineHeight, textWidth, font, isPostit) {
        var x = point[0];
        var y = point[1];

        var paragraphs = text.split('\n');
        for (var i = 0; i < paragraphs.length; i++) {
            if (!firefoxCanvas) {
                // regular text draw
                ctx.fillText(paragraphs[i], x, y - (isDesktopSafari ? lineHeight : 0));
            } else {
                firefoxContext.font = font.substring(0, font.indexOf('1px')) + '10px' + font.substring(font.indexOf('1px') + 3);
                var paragraph = paragraphs[i];
                var paragraphWidth = 0;
                var charWidths = [];
                // first calculate the width of entire paragraph so it can be positioned as middle align
                for (var j = 0; j < paragraph.length; j++) {
                    var charWidth = firefoxContext.measureText(paragraph[j]).width / 10;
                    charWidths.push(charWidth);
                    paragraphWidth += charWidth;
                }
                x = point[0] + (isPostit ? (textWidth - paragraphWidth) / 2 : 0);

                for (j = 0; j < paragraph.length; j++) {
                    ctx.fillText(paragraph[j], x, y);
                    x += charWidths[j];
                }
            }

            y += lineHeight;
        }
    }

    function _calcLineHeight(fontSize, cssLineHeight) {
        if (!cssLineHeight) return;

        if (~cssLineHeight.toString().indexOf('px')) {
            return parseInt(cssLineHeight);
        }

        return parseFloat(cssLineHeight) * fontSize;
    }

    function _doDrawText(op) {

        if ((!op.text && !op.isPostit) || !op.textPoint || op.textPoint.length !== 2) return;

        if (!op.fontWeight) op.fontWeight = 'normal';
        if (!op.fontStyle) op.fontStyle = 'normal';

        var lineHeightPx = op.isPostit ? op.lineHeight : _calcLineHeight(op.fontSize, op.lineHeight);
        var textPoint = JSON.parse(JSON.stringify(op.textPoint));
        // this is because of Canvas library bug that can't set postit bold
        if (isOnBackend && op.isPostit) {
            op.fontStyle = '';
            op.fontWeight = 'normal';
        }

        ctx.save();
        ctx.translate(op.textPoint[0] * (1 - op.fontSize), op.textPoint[1] * (1 - op.fontSize));
        ctx.scale(op.fontSize, op.fontSize);
        var scalingRatio = 1 / op.fontSize;

        var font = op.font || [
                op.fontStyle,
                op.fontWeight,
                '1px',
                op.fontFamily
            ].join(' ');
        lineHeightPx *= scalingRatio;

        ctx.font = font;
        ctx.textAlign = op.isPostit && !firefoxCanvas ? 'center' : 'left';
        ctx.textBaseline = 'bottom';

        _prepCtx(op);

        if (op.isPostit) {
            if (!isPanningOrZooming) {
                ctx.save();
                ctx.shadowColor = config.POSTIT.SHADOW_COLOR;
                ctx.shadowBlur = config.POSTIT.SHADOW_BLUR;
                ctx.shadowOffsetY = config.POSTIT.SHADOW_OFFSET;
                ctx.shadowOffsetX = config.POSTIT.SHADOW_OFFSET;
                ctx.fillRect(op.textPoint[0], op.textPoint[1], op.maxWidth * scalingRatio, op.maxHeight * scalingRatio);
                ctx.restore();
            } else {
                ctx.fillRect(op.textPoint[0], op.textPoint[1], op.maxWidth * scalingRatio, op.maxHeight * scalingRatio);
            }
            ctx.fillStyle = config.POSTIT.TEXT_COLOR;
            // in firefox we set initial text position by aligning text to left and calculating line width in _drawWrappedText
            // else we calculate it here
            textPoint = [op.textPoint[0] + (firefoxCanvas ? 0 : (op.maxWidth / 2 * scalingRatio)),
                op.textPoint[1] + (op.maxHeight * config.POSTIT.PADDING_TOP) * scalingRatio];

        }

        var safariOffset = 0;
        if(ctx.webkitBackingStorePixelRatio < 2) {
            // this is a fix for iOs devices where the pixel desity fucks up line height and text offset for postit
            safariOffset = config.SAFARI_STRANGE_PIXEL_RATIO_CONST; // probably should be something with window.devicePixelRatio
        }
        textPoint[1] = textPoint[1] + safariOffset + lineHeightPx + (isFirefox ? lineHeightPx * config.STRANGE_FIREFOX_CONST : 0);

        _drawWrappedText(op.text, textPoint, lineHeightPx, op.maxWidth * scalingRatio, font, op.isPostit);
        ctx.restore();
    }

    function _drawText(op) {
        if (draw.queue) {
            draw.queue.defer(function(cb) {
                _doDrawText(op);
                cb();
            });
        } else {
            _doDrawText(op);
        }
    }

    function createImage(src, cb, op) {
        var img = new Image();

        var host = (typeof window !== 'undefined') ? window.location.host : '';
        if (src.substr(0, 5) !== 'data:' && !src.match(host)) {
            img.crossOrigin = 'anonymous';
        }

        img.onload = function () {
            img.onload = null;
            img.onerror = null;
            cb(null, img);
        };

        img.onerror = function(err) {
            img.onload = null;
            img.onerror = null;
            isLoading(-1);
            debug('debug', 'Image load error', err);
            if (cb) cb(err);
        };

        img.src = src;
        return img;
    }

    function drawLoadedImage(img, left, top, width, height, rotation, ctx) {
        ctx.save();
        _setComposite();
        ctx.translate(left + width / 2, top + height / 2);
        if (rotation) {
            ctx.rotate(rotation);
        }
        if (width && height)
            ctx.drawImage(img, -1 * width / 2, -1 * height / 2, width, height);
        else
            ctx.drawImage(img, -1 * width / 2, -1 * height / 2);
        ctx.restore();
        draw.queue = null;
        isLoading(-1);
    }

    function _loadImage(src, cb) {

        prepImage(src, function(err, loadedSrc) {
            if (err) return cb(err);
            createImage(loadedSrc, cb);
        });
    }

    function _drawImage(op) {
        if (!draw.queue) draw.queue = Queue.queue(1);

        // we must not use global context because it can be changed while image is being downloaded
        var currentContext = ctx;
        draw.queue.defer(function(cb) {
            isLoading(1);
            var imageInCache = imageCache.getImage(op.src);
            if (imageInCache) {
                drawLoadedImage(imageInCache.img, op.left, op.top, op.w, op.h, op.r, currentContext);
                cb(null, imageInCache.img);
                return;
            }

            _loadImage(op.src, function(err, imgEl) {
                if (err) {
                    isLoading(-1);
                    return cb(null);
                }

                drawLoadedImage(imgEl, op.left, op.top, op.w, op.h, op.r, currentContext);
                imageCache.addToCache({
                    src: op.src,
                    img: imgEl
                });
                cb(err, imgEl);
            });
        });

    }

    var additionalCanvasOffset = [0, 0];
    function setAdditionalCanvasOffset(offset) {
        additionalCanvasOffset = offset;
    }

    function getAdditionalCanvasOffset() {
        return additionalCanvasOffset;
    }

    function doClear() {
        [contexts['true'], contexts['false']].forEach(function (ctx) {
            if (draw.queue) {
                draw.queue.defer(function(cb) {
                    ctx.clearRect(-MAX_CANVAS_DIMENSIONS[0]/2 - additionalCanvasOffset[0],
                        -MAX_CANVAS_DIMENSIONS[1]/2 - additionalCanvasOffset[1],
                        MAX_CANVAS_DIMENSIONS[0],
                        MAX_CANVAS_DIMENSIONS[1]);
                    cb();
                });
            } else {
                ctx.clearRect(-MAX_CANVAS_DIMENSIONS[0]/2 - additionalCanvasOffset[0],
                    -MAX_CANVAS_DIMENSIONS[1]/2 - additionalCanvasOffset[1],
                    MAX_CANVAS_DIMENSIONS[0],
                    MAX_CANVAS_DIMENSIONS[1]);
            }
        });
    }

    function doOp(op, index, mousePos, selectCallback) {

        ctx = contexts[op.lock];
        if (op.delete || op.editedOpid) {
            return;
        }

        else if (op.op === 'line') {
            _drawShape(op, index, mousePos, selectCallback);
        }

        else if (op.op === 'text') {
            _drawText(op);
        }

        else if (op.op === 'image') {
            _drawImage(op);
        }

        else if (op.op === 'lineStraight') {
            _drawStraightLine(op, index, mousePos, selectCallback);
        }

        else if (op.op === 'ellipse') {
            _drawEllipse(op);
        }

        else if (op.op === 'eraseArea') {
            _eraseArea(op, index, mousePos, selectCallback);
        }

        else if (op.op === 'clear') {
            doClear();
        }

        else if (~['arc', 'rectangle', 'bezier', 'quadratic'].indexOf(op.op)) {
            _drawNative(op);
        }

        else if (options && options.customOps[op.op]) {
            if (!options.customOps[op.op].doOp) throw new Error('doOp function not initialized for custom operation ' + op.op);
            options.customOps[op.op].doOp(op);
        }

    }

    function doOps(ops) {
        if (!ops.length) return;
        ops.forEach(doOp);
        ctx = drawCtx;
    }

    function _middle(p1, p2) {
        return [
            p1[0] + (p2[0] - p1[0]) / 2,
            p1[1] + (p2[1] - p1[1]) / 2
        ];
    }

    function doPan(x, y, ops, zoomLevel) {
        try {
            zoomContext(drawCtx, zoomLevel, x, y);
            zoomContext(lockCtx, zoomLevel, x, y);
            doOps(ops);
            drawCtx.restore();
            lockCtx.restore();
        } catch (err) {
            debug('error', 'Error while doing doPan with data: ' + JSON.stringify({
                    x: x,
                    y: y,
                    opsLength: ops ? ops.length : null,
                    err: JSON.stringify(err)
                }));
            throw err;
        }
    }

    function zoomContext(ctxToZoom, zoomLevel, x, y) {
        ctx = ctxToZoom;
        _setComposite(false);
        ctx.setTransform(zoomLevel, 0, 0, zoomLevel, x, y);
        ctx.save();
        ctx.setTransform(zoomLevel, 0, 0, zoomLevel, 0, 0);
        doClear();
        ctx.restore();
        ctx.save();
        doClear();
    }



    function drawLine(first, second, op) {
        ctx.beginPath();
        _prepCtx(op);
        ctx.moveTo(first[0], first[1]);
        if (lineSmoothing) {
            var midPoint = _middle(first, second);
            ctx.quadraticCurveTo(first[0], first[1], midPoint[0], midPoint[1]);
            ctx.stroke();
            return midPoint;
        } else {
            ctx.lineTo(second[0], second[1]);
            ctx.stroke();
            return second;
        }
    }

    function drawCircle(point, op) {
        ctx.beginPath();
        _prepCtx(op);
        ctx.moveTo(point[0], point[1]);
        ctx.arc(point[0], point[1], ctx.lineWidth / 2, 0, 2 * Math.PI, true);
        ctx.fill();
    }

    function isPointInDrawnAreas(x, y) {
        // isPointInStroke makes it really slow - what to do
        return /*ctx.isPointInStroke(x, y) || */ctx.isPointInPath(x, y);
    }

    function beginPath() {
        ctx.beginPath();
    }

    function closePath() {
        ctx.closePath();
    }

    drawCtx = context;
    lockCtx = lockContext ? lockContext : context;

    drawCtx.imageSmoothingEnabled = true;
    lockCtx.imageSmoothingEnabled = true;

    ctx = lockCtx;
    _prepCtx({});
    ctx = drawCtx;
    _prepCtx({});

    var contexts = {
        true: lockCtx,
        false: drawCtx,
        undefined: drawCtx
    };

    var q = Queue.queue(1);

    var draw = {
        drawLine: drawLine,
        drawCircle: drawCircle,
        doOps: doOps,
        doOp: doOp,
        doPan: doPan,
        queue: q,
        doClear: doClear,
        isPointInDrawnAreas: isPointInDrawnAreas,
        beginPath: beginPath,
        closePath: closePath,
        drawEraserWithoutBeginningPath: drawEraserWithoutBeginningPath,
        setFirefoxCanvasSize: setFirefoxCanvasSize,
        setPanningZooming: setPanningZooming,
        getPanningZooming: getPanningZooming,
        setAdditionalOffset: setAdditionalCanvasOffset,
        getAdditionalCanvasOffset: getAdditionalCanvasOffset,
        roundToDecPoints: Drawing.roundToDecPoints
    };

    return draw;

};

Drawing.getOpsBoundingBox = function(ops) {
    var opsToReturn = ops.reduce(
        function(m, op) {
            if (op.delete || op.editedOpid || op.op === 'undo' || op.op === 'redo' || op.op === 'lock' || op.op === 'gotoPage') return m;
            var opDim = Drawing.getMaxDimensions(op);
            return [[
                Math.min(m[0][0], opDim[0][0]),
                Math.min(m[0][1], opDim[0][1])
            ], [
                Math.max(m[1][0], opDim[1][0]),
                Math.max(m[1][1], opDim[1][1])
            ]];
        }, [[config.MAX_CANVAS_DIMENSIONS[0], config.MAX_CANVAS_DIMENSIONS[1]],
            [-config.MAX_CANVAS_DIMENSIONS[0], -config.MAX_CANVAS_DIMENSIONS[1]]]);
    return opsToReturn;
}

Drawing.getMaxDimensions = function(op) {
    let left = 0, right = 0, top = 0, bottom = 0;

    if (op.points && op.points.length) {

        let pad = op.size || 0 / 2;

        left = Math.min(...op.points.map(p => p[0])) - pad,
        right = Math.max(...op.points.map(p => p[0])) - pad,
        top = Math.min(...op.points.map(p => p[1])) + pad,
        bottom = Math.max(...op.points.map(p => p[1])) + pad;

    } else if (op.op === 'rectangle') {

        let pad = op.filled ? op.size / 2 : 0;

        left = op.x - pad;
        top = op.y - pad;
        right = op.x + op.w + pad;
        bottom = op.y + op.h + pad;

    } else if (op.op === 'ellipse') {

        let pad = op.filled ? op.size / 2 : 0;

        left = op.x - op.r * op.scale[0] - pad;
        top = op.y - op.r * op.scale[1] - pad;
        right = op.x + op.r * op.scale[0] + pad;
        bottom = op.y + op.r * op.scale[1] + pad;

    } else if (op.op === 'text') {

        left = op.textPoint[0];
        top = op.textPoint[1];
        right = op.textPoint[0] + op.maxWidth + (op.postitShadowSize || 0);
        bottom = op.textPoint[1] + op.maxHeight + (op.postitShadowSize || 0);

    } else if (op.op === 'image') {

        if (op.r) {
            let cx = op.left + op.w / 2,
                cy = op.top + op.h / 2,
                corners = [
                    rotateCoordinates(op.left, op.top, cx, cy, op.r), // top left
                    rotateCoordinates(op.left + op.w, op.top, cx, cy, op.r), // top right
                    rotateCoordinates(op.left, op.top + op.h, cx, cy, op.r), // bottom left
                    rotateCoordinates(op.left + op.w, op.top + op.h, cx, cy, op.r), // bottom right
                ];

            left = Math.min(...corners.map(c => c.x)),
            right = Math.max(...corners.map(c => c.x)),
            top = Math.min(...corners.map(c => c.y)),
            bottom = Math.max(...corners.map(c => c.y));
        } else {
            left = op.left;
            top = op.top;
            right = op.left + op.w;
            bottom = op.top + op.h;
        }

    }

    return [[left, top], [right, bottom]];
};

Drawing.getOpOutline = function(op) {
    let dim = this.getMaxDimensions(op);

    return {
        x: dim[0][0],
        y: dim[0][1],
        width: dim[1][0] - dim[0][0],
        height: dim[1][1] - dim[0][1]
    };
};

Drawing.setNewPositionForMovedOp = function(op, startPos, endPos, initPos) {

    var dx = endPos[0] - startPos[0];
    var dy = endPos[1] - startPos[1];
    var selectedOpName = op.op;
    if (selectedOpName === 'rectangle' || selectedOpName === 'ellipse') {
        op.x = (initPos ? initPos[0] : op.x) + dx;
        op.y = (initPos ? initPos[1] : op.y) + dy;
    } else if (selectedOpName === 'image') {
        op.left = (initPos ? initPos[0] : op.left) + dx;
        op.top = (initPos ? initPos[1] : op.top) + dy;
    } else if (selectedOpName === 'text') {
        var textPoint = (initPos ? initPos : op.textPoint);
        op.textPoint = [textPoint[0] + dx, textPoint[1] + dy];
    } else if (op.points) {
        // since we only have init position of the first point in the line, we have to modify dx to show difference
        // for all points
        var realDx = dx,
            realDy = dy;
        var pointsInLine = op.points;
        if (initPos) {
            realDx = dx - (pointsInLine[0][0] - initPos[0]);
            realDy = dy - (pointsInLine[0][1] - initPos[1]);
        }
        for (var i = 0; i < pointsInLine.length; i++) {
            op.points[i] = [pointsInLine[i][0] + realDx, pointsInLine[i][1] + realDy];
        }

        if (op.arrowPoints) {
            for (i = 0; i < op.arrowPoints.length; i++) {
                op.arrowPoints[i] = [op.arrowPoints[i][0] + realDx, op.arrowPoints[i][1] + realDy];
            }
        }
    }

    return op;
};

Drawing.roundToDecPoints = function(number, decimalPoints) {
    if (!decimalPoints) decimalPoints = MAX_DECIMAL_POINTS;
    var multiplier = Math.pow(10, decimalPoints);
    return Math.round(number * multiplier) / multiplier;
};

Drawing.repositionOpByMiddlePoint = function (op, dx, dy) {

    if (op.points && op.points.length) {
        for (var i = 0; i < op.points.length; i++) {
            op.points[i][0] += dx;
            op.points[i][1] += dy;
        }

        if (op.arrowPoints) {
            for (i = 0; i < op.arrowPoints.length; i++) {
                op.arrowPoints[i][0] += dx;
                op.arrowPoints[i][1] += dy;
            }
        }
    } else if (op.op === 'rectangle' || op.op === 'ellipse') {
        op.x += dx;
        op.y += dy;
    } else if (op.op === 'text') {
        op.textPoint[0] += dx;
        op.textPoint[1] += dy;
    } else if (op.op === 'image') {
        op.left += dx;
        op.top += dy;
    }

    return op;
};

Drawing.getOpOnPosition = function (ops, point, drawingInstanceMarker, cb) {
    var opOnPoint,
      setOpOnPoint = op => opOnPoint = op;

    for (var i = ops.length - 1; i >= 0; i--) {
        var op = ops[i];

        if (config.SELECTABLE_OPS.includes(op.op)) {
            if (op.points) {

                drawingInstanceMarker.doClear();
                try {
                    drawingInstanceMarker.doOp(op, i, point, setOpOnPoint);
                } catch (err) {
                    this.dispatcher.emit('flash-message', 'This feature is not available in Internet Explorer or Edge, for the best experience use Chrome or Firefox', 'error', 10);
                }
                if (opOnPoint !== undefined) {
                    // if the first op in line is eraser, then user clicked on the void from eraser so clicked op is nothing
                    if (opOnPoint.eraser) {
                        opOnPoint = undefined;
                    }
                    break;
                }

            } else if (op.op === 'rectangle') {
                if (op.filled) {
                    if ((point[0] >= op.x && point[0] <= op.x + op.w) &&
                        (point[1] >= op.y && point[1] <= op.y + op.h)) {
                        opOnPoint = op;
                        break;
                    }
                } else {
                    if (((point[0] >= op.x - op.size && point[0] <= op.x + op.size) &&
                        (point[1] >= op.y && point[1] <= op.y + op.h)) ||
                        ((point[0] >= op.x && point[0] <= op.x + op.w) &&
                            (point[1] >= op.y - op.size && point[1] <= op.y + op.size)) ||
                        ((point[0] >= op.x + op.w - op.size && point[0] <= op.x + op.w + op.size) &&
                            (point[1] >= op.y && point[1] <= op.y + op.h)) ||
                        ((point[0] >= op.x && point[0] <= op.x + op.w) &&
                            (point[1] >= op.y + op.h - op.size && point[1] <= op.y + op.h + op.size))) {

                        opOnPoint = op;
                        break;
                    }
                }
            } else if (op.op === 'ellipse') {
                if (op.filled) {
                    var ellipseEquasion = ((Math.pow(point[0] - op.x, 2) / Math.pow(op.r * op.scale[0], 2)) + (Math.pow(point[1] - op.y, 2) / Math.pow(op.r * op.scale[1], 2)));
                    if (ellipseEquasion <= 1) {
                        opOnPoint = op;
                        break;
                    }
                } else {
                    var ellipseEquasionOuter = ((Math.pow(point[0] - op.x, 2) / Math.pow((op.r + op.size / 2) * op.scale[0], 2)) +
                        (Math.pow(point[1] - op.y, 2) / Math.pow((op.r + op.size / 2) * op.scale[1], 2)));
                    var ellipseEquasionInner = ((Math.pow(point[0] - op.x, 2) / Math.pow((op.r - op.size / 2) * op.scale[0], 2)) +
                        (Math.pow(point[1] - op.y, 2) / Math.pow((op.r - op.size / 2) * op.scale[1], 2)));
                    if (ellipseEquasionOuter <= 1 && ellipseEquasionInner > 1) {
                        opOnPoint = op;
                        break;
                    }
                }
            } else if (op.op === 'text') {
                if ((point[0] >= op.textPoint[0] && point[0] <= op.textPoint[0] + op.maxWidth) &&
                    (point[1] >= op.textPoint[1] && point[1] <= op.textPoint[1] + op.maxHeight)) {
                    opOnPoint = op;
                    break;
                }
            } else if (op.op === 'image') {

                function getRotatedCoordinates(x, y) {
                    if (!op.w || !op.h || !op.r) return {x: x, y: y};

                    let cx = op.left + op.w / 2,
                        cy = op.top + op.h / 2;

                    // rotate from screen coordinate system to image coordinate system
                    return rotateCoordinates(x, y, cx, cy, op.r);
                }

                let p = getRotatedCoordinates(point[0], point[1]);

                if ((p.x >= op.left && p.x <= op.left + op.w) &&
                    (p.y >= op.top && p.y <= op.top + op.h)) {
                    opOnPoint = op;
                    break;
                }
            }
        }

    }
    return opOnPoint;
};

Drawing.getLineFromPointsY = function(points, x) {
    return (
        Math.max(-1000000, (
            (points[1][1] - points[0][1])/(points[1][0] - points[0][0])
        ) || 0)
    ) * (x - points[0][0]) + points[0][1];
};

Drawing.getLineCoef = function(points) {
    return ( (points[1][1] - points[0][1])/(points[1][0] - points[0][0]) ) || 0;
};

Drawing.getVerticalLineY = function(referencePoint, coef, x) {
    return (referencePoint[0] - x)/coef + referencePoint[1];
};

Drawing.distanceBetweenTwoPoints = function(points) {
    return Math.sqrt(Math.pow(points[1][0] - points[0][0], 2) + Math.pow(points[1][1] - points[0][1], 2));
};

Drawing.getXFromCoefAndOffset = function(refPoint, k, z) {
    return Math.sqrt( Math.pow(z, 2)/( Math.pow(k, 2) + 1 ) ) + refPoint[0];
};

Drawing.getArrowPoints = function (size, lastTwoPoints) {
    var tipXOffset = config.ARROW_TIP_X_OFFSET * size,
        wingsXOffset = config.ARROW_WINGS_X_OFFSET * size,
        lineCoef = Drawing.getLineCoef(lastTwoPoints);

    var arrowTipX = lastTwoPoints[1][0] - lastTwoPoints[0][0] > 0 ?
        Drawing.getXFromCoefAndOffset(lastTwoPoints[1], lineCoef, tipXOffset) :
        2 * lastTwoPoints[1][0] - Drawing.getXFromCoefAndOffset(lastTwoPoints[1], lineCoef, tipXOffset);
    var arrowTipY = lastTwoPoints[1][0] - lastTwoPoints[0][0] === 0 ?
        lastTwoPoints[1][1] + (lastTwoPoints[1][1] - lastTwoPoints[0][1] > 0 ? tipXOffset : -tipXOffset) :
        Drawing.getLineFromPointsY(lastTwoPoints, arrowTipX);

    var arrowWing1X = Drawing.getXFromCoefAndOffset(lastTwoPoints[1], -1/lineCoef, wingsXOffset);
    var arrowWing1Y = lastTwoPoints[1][1] - lastTwoPoints[0][1] === 0 ?
        lastTwoPoints[1][1] - wingsXOffset :
        Drawing.getVerticalLineY(lastTwoPoints[1], lineCoef, arrowWing1X);

    var arrowWing2X = 2 * lastTwoPoints[1][0] - Drawing.getXFromCoefAndOffset(lastTwoPoints[1], -1/lineCoef, wingsXOffset);
    var arrowWing2Y = lastTwoPoints[1][1] - lastTwoPoints[0][1] === 0 ?
        lastTwoPoints[1][1] + wingsXOffset :
        Drawing.getVerticalLineY(lastTwoPoints[1], lineCoef, arrowWing2X);

    return [
        [arrowTipX, arrowTipY],
        [arrowWing1X, arrowWing1Y],
        [arrowWing2X, arrowWing2Y]
    ];
};

Drawing.doUndo = function (opToUndo, ops) {
    if (!opToUndo) return;

    var opPosition,
        opids = _.map(ops, op => op.opid);

    opToUndo.delete = true;

    if (opToUndo.op === 'text' && opToUndo.editedOpid) {

        opPosition = opids.indexOf(opToUndo.editedOpid);
        Drawing.revertEditedText(ops[opPosition], opToUndo);

    } else if (opToUndo.op === 'move' || opToUndo.op === 'delete' || opToUndo.op === 'lock') {
        _.each(opToUndo.ops, function (opData) {
            if (!opData) return;

            opPosition = opids.indexOf(opData.opid);
            if (opPosition === -1) return;

            if (opToUndo.op === 'move' && opData.pos) {

                ops[opPosition] = Drawing.revertMovedOp(opToUndo, opData, ops[opPosition]);

            } else if (opToUndo.op === 'delete') {

                ops[opPosition]['delete'] = false;

            } else if (opToUndo.op === 'lock') {

                ops[opPosition]['lock'] = !opToUndo.lock;

            }

        });

    }
};

Drawing.doRedo = function (redoOp, ops, opsChangingFunction) {
    var undoOpToRevert = ops.find(op => op.opid === redoOp.undoOpid);
    if (!undoOpToRevert) return console.log('Can\'t find undo op to revert');
    var opToRedo = ops.find(op => op.opid === undoOpToRevert.undoOpid);
    if (!opToRedo) return console.log('Can\'t find op to redo');

    opToRedo.delete = false;
    if (['delete', 'lock'].includes(opToRedo.op)) {
        if (opsChangingFunction) return opsChangingFunction(ops.filter(op => opToRedo.ops.find(o => o.opid === op.opid)), opToRedo.op, true);
        ops.forEach(function (op) {
            if (opToRedo.ops.find(o => o.opid === op.opid)) op[opToRedo.op] = true;
        });
    } else if (['move'].includes(opToRedo.op)) {
        _.each(opToRedo.ops, function (opData) {
            if (!opData) return;
            var index = ops.findIndex(op => op.opid === opData.opid);
            if (index > -1) ops[index] = Drawing.setNewPositionForMovedOp(ops[index], opToRedo.points[0], opToRedo.points[opToRedo.points.length - 1], opData.pos);
        });
    } else if (opToRedo.op === 'text' && opToRedo.editedOpid) Drawing.alterText(opToRedo, ops);
};

Drawing.revertEditedText = function(opToAlter, alteringOp) {

    opToAlter.textPoint = alteringOp.initData.textPoint;
    opToAlter.fontSize = alteringOp.initData.fontSize;
    opToAlter.color = alteringOp.initData.color;
    opToAlter.maxWidth = alteringOp.initData.maxWidth;
    opToAlter.maxHeight = alteringOp.initData.maxHeight;
    opToAlter.text = alteringOp.initData.text;
    opToAlter.lineHeight = alteringOp.initData.lineHeight;

};

Drawing.revertMovedOp = function(moveOp, opData, opToMove) {
    var startingPoint = moveOp.points[0];
    var endingPoint = moveOp.points[moveOp.points.length - 1];
    var initPos = [
        opData.pos[0] + (endingPoint[0] - startingPoint[0]),
        opData.pos[1] + (endingPoint[1] - startingPoint[1])
    ];
    return Drawing.setNewPositionForMovedOp(
        opToMove,
        endingPoint,
        startingPoint,
        initPos
    );
};



Drawing.alterText = function(alteringOp, boardOps) {

    _.each(boardOps, function (op) {
        if (op.opid === alteringOp.editedOpid) {
            op.textPoint = alteringOp.textPoint;
            op.fontSize = alteringOp.fontSize;
            op.color = alteringOp.color;
            op.maxWidth = alteringOp.maxWidth;
            op.maxHeight = alteringOp.maxHeight;
            op.text = alteringOp.text;
            op.lineHeight = alteringOp.lineHeight;
            return false;
        }
    });

};


