package com.foobnix.pdf.search.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.pdf.search.activity.msg.MessagePageXY;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.pdf.search.activity.msg.TextWordsMessage;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.List;
public class PageView extends View {

    public static int MIN = Dips.dpToPx(15);
    static volatile boolean isFirstZoomInOut = true;
    static volatile boolean prevLock = false;
    static Paint rect = new Paint();

    static {
        rect.setColor(Color.DKGRAY);
        rect.setStrokeWidth(Dips.dpToPx(1));
        rect.setStyle(Style.STROKE);

    }

    int drawableHeight, drawableWidth;
    GestureDetector gestureDetector;
    Scroller scroller;
    SimpleGestureListener imageGestureListener;
    Paint paintWrods = new Paint();
    float x, y, xInit, yInit, cx, cy, distance = 0;
    ClickUtils clickUtils;
    BrightnessHelper brightnessHelper;
    int dp1 = Dips.dpToPx(1);
    private BitmapDrawable imageDrawable;
    private Handler handler;
    Runnable scrolling = new Runnable() {

        @Override
        public void run() {
            if (scroller.isFinished()) {
                return;
            }
            final boolean more = scroller.computeScrollOffset();
            final int xx = scroller.getCurrX();
            final int yy = scroller.getCurrY();

            final float dx = xx - x;
            final float dy = yy - y;

            imageMatrix().postTranslate(dx, dy);
            y = yy;
            x = xx;
            invalidate();

            if (more) {
                handler.post(scrolling);
            }

        }
    };

    private boolean isReadyForMove = false;
    private boolean isLongPress = false;

    public boolean isReadyForMove() {
        return isReadyForMove;
    }

    public void setReadyForMove(boolean readyForMove) {
        isReadyForMove = readyForMove;
    }

    public boolean isLongPress() {
        return isLongPress;
    }

    public void setLongPress(boolean longPress) {
        isLongPress = longPress;
    }

    public boolean isIgronerClick() {
        return isIgronerClick;
    }

    public void setIgronerClick(boolean igronerClick) {
        isIgronerClick = igronerClick;
    }

    public int getIsMoveNextPrev() {
        return isMoveNextPrev;
    }

    public void setIsMoveNextPrev(int isMoveNextPrev) {
        this.isMoveNextPrev = isMoveNextPrev;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    private boolean isIgronerClick = false;
    private int isMoveNextPrev = 0;
    private int pageNumber;

    public PageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        handler = new Handler();
        scroller = new Scroller(getContext(), new AccelerateDecelerateInterpolator());
        imageGestureListener = new SimpleGestureListener(this);
        gestureDetector = new GestureDetector(context, imageGestureListener);

        paintWrods.setStrokeWidth(Dips.dpToPx(1));
        paintWrods.setTextSize(30);

        EventBus.getDefault().register(this);
        clickUtils = new ClickUtils();
        brightnessHelper = new BrightnessHelper(context);
        setSaveEnabled(false);
    }

    public RectF transform(RectF origin, int number) {
        RectF r = new RectF(origin);
        r.left = r.left * drawableWidth;
        r.right = r.right * drawableWidth;

        r.top = r.top * drawableHeight;
        r.bottom = r.bottom * drawableHeight;

        if (number == 1) {
            r.left = r.left / 2;
            r.right = r.right / 2;
        } else if (number == 2) {
            r.left = drawableWidth / 2 + r.left / 2;
            r.right = drawableWidth / 2 + r.right / 2;
        }

        return r;
    }

    public TextWord[][] getPageText(int number) {
        return getPageText(pageNumber, number);
    }

    public TextWord[][] getPageText(int pageNumber, int number) {
        try {
            if (AppSP.get().isDouble && number != 0) {

                int page = pageNumber * 2;
                if (AppSP.get().isDoubleCoverAlone) {
                    page--;
                }

                TextWord[][] t = null;
                if (number == 1) {
                    t = PageState.get().pagesText.get(page);
                } else if (number == 2) {
                    t = PageState.get().pagesText.get(page + 1);
                }

                for (int i = 0; i < t.length; i++) {
                    for (int j = 0; j < t[i].length; j++) {
                        // TextWord textWord = t[i][j];
                        // textWord.number = number;
                        t[i][j].number = number;
                    }
                }

                return t;
            } else {
                return PageState.get().pagesText.get(pageNumber);
            }
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public synchronized Pair<List<PageLink>, List<Annotation>> getPageLinks(int number) {
        if (AppSP.get().isCut || AppSP.get().isCrop) {
            return new Pair(Collections.emptyList(), Collections.emptyList());
        }
        return getPageLinksInner(number);

    }

    public synchronized Pair<List<PageLink>, List<Annotation>> getPageLinksInner(int number) {

        try {
            List<PageLink> t = null;
            List<Annotation> a = null;

            if (AppSP.get().isDouble && number != 0) {

                int page = pageNumber * 2;
                if (AppSP.get().isDoubleCoverAlone) {
                    page--;
                }
                if (number == 1) {
                    t = PageState.get().pagesLinks.get(page);
                    a = PageState.get().pagesAnnotation.get(page);
                } else if (number == 2) {
                    t = PageState.get().pagesLinks.get(page + 1);
                    a = PageState.get().pagesAnnotation.get(page + 1);
                }

                if (t != null) {
                    for (PageLink l : t) {
                        LOG.d("PageLinks targetPage after", l.targetPage);
                        l.number = number;
                        LOG.d("PageLinks targetPage before", l.targetPage);
                    }
                }
            } else {
                t = PageState.get().pagesLinks.get(pageNumber);
                a = PageState.get().pagesAnnotation.get(pageNumber);
            }

            if (t == null) {
                t = Collections.emptyList();
            }
            if (a == null) {
                a = Collections.emptyList();
            }
            return new Pair(t, a);
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }

    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Matrix imageMatrix() {
        return PageState.get().getMatrix();
    }

    @Subscribe
    public void onInvalidate(InvalidateMessage event) {
        invalidate();
    }

    @Subscribe
    public void onSelectTextByAnchors(MessagePageXY event) {
        if (pageNumber == event.getPage() && MessagePageXY.TYPE_SELECT_TEXT == event.getType()) {
            selectText(event.getX1(), event.getY1(), event.getX(), event.getY());
        }

    }

    ;

    @Subscribe
    public void onAutoFit(MessageAutoFit event) {
        LOG.d("onAutoFit recive");
        if (pageNumber == event.getPage()) {
            LOG.d("onAutoFit recive", event.getPage(), pageNumber);
            autoFit();
            invalidate();
            isFirstZoomInOut = true;
        }
    }

    ;

    @Subscribe
    public void onMovePage(MovePageAction event) {
        if (pageNumber != event.getPage()) {
            return;
        }
        int k = Dips.dpToPx(3);
        float scale = 0.03f;

        final float values[] = new float[9];
        imageMatrix().getValues(values);
        float mScale = values[Matrix.MSCALE_X];

        int w = (drawableWidth) / 2;
        int h = (drawableHeight) / 2;

        if (MovePageAction.CENTER == event.getAction()) {
            LOG.d("Action center", event.getPage());
            PageState.get().isAutoFit = true;
            onAutoFit(new MessageAutoFit(event.getPage()));
            return;
        }

        if (MovePageAction.LEFT == event.getAction()) {
            imageMatrix().postTranslate(-1 * k, 0);
        } else if (MovePageAction.RIGHT == event.getAction()) {
            imageMatrix().postTranslate(k, 0);
        } else if (MovePageAction.UP == event.getAction()) {
            imageMatrix().postTranslate(0, k * -1);
        } else if (MovePageAction.DOWN == event.getAction()) {
            imageMatrix().postTranslate(0, k);
        } else if (MovePageAction.ZOOM_PLUS == event.getAction()) {
            imageMatrix().postScale(1 + scale, 1 + scale, w, h);
        } else if (MovePageAction.ZOOM_MINUS == event.getAction()) {
            imageMatrix().postScale(1 - scale, 1 - scale, w, h);
        }
        LOG.d("MMM SCALE", mScale);

        PageState.get().isAutoFit = false;
        invalidateAndMsg();

    }

    @Subscribe
    public void onCenterHorizontally(MessageCenterHorizontally event) {
        LOG.d("onCenterHorizontally recive");
        if (pageNumber == event.getPage()) {
            LOG.d("onAutoFit recive", event.getPage(), pageNumber);
            centerHorizontally();
            invalidate();
        }
    }

    @Subscribe
    public void onTextWords(TextWordsMessage event) {
        if (event.getPage() == pageNumber) {
            // pageText = event.getMessages();
            selectText(xInit, yInit, xInit, yInit);
        }
    }

    public void invalidateAndMsg() {
        EventBus.getDefault().post(new InvalidateMessage());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LOG.d("ImagePageFragment", "onDetachedFromWindow");
//        if (bitmap != null && !bitmap.isRecycled()) {
//            LOG.d("recycle onDetachedFromWindow");
//            bitmap.recycle();
//            bitmap = null;
//        }
        imageDrawable = null;
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        imageGestureListener.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && MagicHelper.getBgColor() == Color.BLACK */) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(MagicHelper.ligtherColor(MagicHelper.getBgColor()));
            }

            final int saveCount = canvas.getSaveCount();
            canvas.save();

            canvas.concat(imageMatrix());

            if (imageDrawable != null) {
                imageDrawable.draw(canvas);
            }

            if (PageState.get().isShowCuttingLine && AppSP.get().isCut == false) {
                int offset = drawableWidth * AppState.get().cutP / 100;
                canvas.drawLine(offset, 0, offset, drawableHeight, paintWrods);
            }

            List<TextWord> selectedWords = PageState.get().getSelectedWords(pageNumber);
            if (selectedWords != null) {
                for (TextWord tw : selectedWords) {
                    drawWord(canvas, tw);
                }
            }

            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && !TempHolder.get().isTextFormat */) {
                canvas.drawRect(-dp1, 0, drawableWidth + dp1, drawableHeight, rect);
            }

            if (!AppSP.get().isCut && !AppSP.get().isCrop) {

                paintWrods.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
                paintWrods.setAlpha(60);

                if (!BookCSS.get().isTextFormat()) {
                    if (AppSP.get().isDouble) {
                        for (PageLink pl : getPageLinks(1).first) {
                            drawLink(canvas, pl);
                        }

                        for (PageLink pl : getPageLinks(2).first) {
                            drawLink(canvas, pl);
                        }
                    } else {
                        for (PageLink pl : getPageLinks(0).first) {
                            drawLink(canvas, pl);
                        }
                    }
                }
            }

            canvas.restoreToCount(saveCount);

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void addBitmap(Bitmap bitmap) {
        imageDrawable = new BitmapDrawable(getResources(), bitmap);
        drawableHeight = bitmap.getHeight();
        drawableWidth = bitmap.getWidth();
        imageDrawable.setBounds(0, 0, drawableWidth, drawableHeight);

        autoFit();
        invalidate();

        LOG.d("addBitmap", bitmap.getWidth(), bitmap.getHeight(), "Real WH:", getWidth(), getHeight());
    }

    public void recycle() {
        try {
            if (imageDrawable != null && imageDrawable.getBitmap() != null) {
                imageDrawable.getBitmap().recycle();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void centerHorizontally() {
        float[] f = new float[9];
        imageMatrix().getValues(f);
        float tx = f[Matrix.MTRANS_X];
        float sx = f[Matrix.MSCALE_X];

        imageMatrix().postTranslate(getWidth() / 2 - tx - drawableWidth * sx / 2, 0);
        LOG.d("centerHorizontally", getWidth(), tx, drawableWidth * sx);
    }

    public void autoFit() {
        if (!PageState.get().isAutoFit) {
            return;
        }

        final int w = getWidth();
        final int h = getHeight();
        final float scaleH = (float) h / drawableHeight;
        final float scaleW = (float) w / drawableWidth;

        imageMatrix().reset();
        if (scaleH < scaleW) {
            LOG.d("image pre scale scaleH", scaleH);
            imageMatrix().preScale(scaleH, scaleH);
            imageMatrix().postTranslate(Math.abs(getWidth() - drawableWidth * scaleH) / 2, 0);
        } else {
            LOG.d("image pre scale scaleW", scaleW);
            imageMatrix().preScale(scaleW, scaleW);
            imageMatrix().postTranslate(0, Math.abs(getHeight() - drawableHeight * scaleW) / 2);
        }
    }

    float centerX(final MotionEvent event) {
        return (event.getX() + event.getX(1)) / 2;
    }

    float centerY(final MotionEvent event) {
        return (event.getY() + event.getY(1)) / 2;
    }

    float distance(final MotionEvent event) {
        final float x1 = event.getX();
        final float y1 = event.getY();

        final float x2 = event.getX(1);
        final float y2 = event.getY(1);

        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public float getMyScale() {
        final int w = getWidth();
        final int h = getHeight();

        LOG.d("WxH", w, h);
        LOG.d("image WxH", drawableWidth, drawableHeight);

        final float scaleH = (float) h / drawableHeight;
        final float scaleW = (float) w / drawableWidth;
        float min = Math.min(scaleH, scaleW);
        LOG.d("scale min", min, scaleH, scaleW);
        return min;

    }

    public void drawWord(Canvas c, TextWord t) {
        RectF o = transform(t, t.number);
        c.drawRect(o, paintWrods);
    }

    public void drawLink(Canvas c, PageLink pl) {
        RectF o = transform(pl.sourceRect, pl.number);
        c.drawLine(o.left, o.bottom, o.right, o.bottom, paintWrods);
        // c.drawText("" + pl.targetPage, o.right, o.bottom, paintWrods);
    }

    public Pair<PageLink, Annotation> getPageLinkClicked(float x1, float y1) {

        RectF tr = new RectF();
        imageMatrix().mapRect(tr);

        float x = x1 - tr.left;
        float y = y1 - tr.top;

        float[] f = new float[9];
        imageMatrix().getValues(f);

        float scaleX = f[Matrix.MSCALE_X];

        x = x / scaleX;
        y = y / scaleX;

        RectF tapRect = new RectF(x, y, x, y);

        x1 = x1 - tr.left;
        x1 = x1 / scaleX;

        int firstNumber = 0;
        if (AppSP.get().isDouble) {
            firstNumber = x1 < drawableWidth / 2 ? 1 : 2;
        }
        //


        //


        PageLink p = null;
        Annotation a = null;
        List<PageLink> pageLinks = getPageLinks(firstNumber).first;
        List<Annotation> annotations = getPageLinks(firstNumber).second;

        LOG.d("getPageLinkClicked", x1, "w", drawableWidth, firstNumber, "links", pageLinks.size());

        for (PageLink link : pageLinks) {
            if (link == null) {
                continue;
            }
            RectF wordRect = transform(link.sourceRect, firstNumber);
            boolean intersects = RectF.intersects(wordRect, tapRect);
            if (intersects) {
                p = link;
                break;
            }
        }
        for (Annotation link : annotations) {
            if (link == null) {
                continue;
            }
            RectF wordRect = transform(link, firstNumber);
            boolean intersects = RectF.intersects(wordRect, tapRect);
            if (intersects) {
                a = link;
                LOG.d("Link Clicked", a.text);
                break;
            }
        }

        return new Pair(p, a);

    }

    public String selectText(float x1, float y1, float xInit, float yInit) {
        if (!AppSP.get().isDouble && getPageText(0) == null) {
            LOG.d("get pag No page text", pageNumber);
            return null;
        }

        boolean single = Math.abs(x1 - xInit) < MIN && Math.abs(y1 - yInit) < MIN;

        RectF tr = new RectF();
        imageMatrix().mapRect(tr);

        float x = x1 - tr.left;
        float y = y1 - tr.top;

        xInit = xInit - tr.left;
        yInit = yInit - tr.top;

        float[] f = new float[9];
        imageMatrix().getValues(f);

        float scaleX = f[Matrix.MSCALE_X];

        x = x / scaleX;
        y = y / scaleX;

        xInit = xInit / scaleX;
        yInit = yInit / scaleX;

        RectF tapRect = new RectF(xInit, yInit, x, y);
        if (yInit > y) {
            tapRect.sort();
        }

        PageState.get().cleanSelectedWords();

        StringBuilder build = new StringBuilder();

        boolean isHyphenWorld = false;
        TextWord prevWord = null;

        int firstNumber = 0;
        if (AppSP.get().isDouble) {
            firstNumber = xInit < drawableWidth / 2 ? 1 : 2;
        }
        TempHolder.get().textFromPage = firstNumber;

        LOG.d("firstNumber", firstNumber);
        TextWord[][] pageText = getPageText(firstNumber);
        if (pageText == null) {
            return null;
        }
        for (TextWord line[] : pageText) {
            if (line == null) {
                continue;
            }
            final TextWord current[] = line;
            for (TextWord textWord : current) {
                if (textWord == null) {
                    continue;
                }
                if (!BookCSS.get().isTextFormat() && (textWord.left < 0 || textWord.top < 0)) {
                    continue;
                }

                RectF wordRect = transform(textWord, firstNumber);
                if (single) {
                    boolean intersects = RectF.intersects(wordRect, tapRect);
                    if (intersects || isHyphenWorld) {
                        LOG.d("ADD TEXT", textWord);

                        if (prevWord != null && prevWord.w.endsWith("-") && !isHyphenWorld) {
                            build.append(prevWord.w.replace("-", ""));
                            PageState.get().addWord(pageNumber, prevWord);
                        }

                        if (!isHyphenWorld) {
                            PageState.get().addWord(pageNumber, textWord);
                        }

                        if (isHyphenWorld && TxtUtils.isNotEmpty(textWord.getWord())) {
                            PageState.get().addWord(pageNumber, textWord);
                            isHyphenWorld = false;
                        }
                        if (textWord.getWord().endsWith("-")) {
                            isHyphenWorld = true;
                        }
                        build.append(textWord.getWord() + " ");
                    }
                } else {
                    if (y > yInit) {
                        if (wordRect.top < tapRect.top && wordRect.bottom > tapRect.top && wordRect.right > tapRect.left) {
                            PageState.get().addWord(pageNumber, textWord);
                            build.append(textWord.getWord() + TxtUtils.space());
                        } else if (wordRect.top < tapRect.bottom && wordRect.bottom > tapRect.bottom && wordRect.left < tapRect.right) {
                            PageState.get().addWord(pageNumber, textWord);
                            build.append(textWord.getWord() + TxtUtils.space());
                        } else if (wordRect.top > tapRect.top && wordRect.bottom < tapRect.bottom) {
                            PageState.get().addWord(pageNumber, textWord);
                            build.append(textWord.getWord() + TxtUtils.space());
                        }
                    } else if (RectF.intersects(wordRect, tapRect)) {
                        PageState.get().addWord(pageNumber, textWord);
                        if (AppState.get().selectingByLetters) {
                            build.append(textWord.w);
                        } else {
                            build.append(textWord.w.trim() + " ");
                        }
                    }
                }

                if (TxtUtils.isNotEmpty(textWord.w)) {
                    prevWord = textWord;
                }

            }
            String k;
            if (AppState.get().selectingByLetters && current.length >= 2 && !(k = current[current.length - 1].getWord()).equals(" ") && !k.equals("-")) {
                build.append(" ");
            }
        }


        String txt = build.toString();
        if (txt.endsWith("- ")) {
            try {
                if (firstNumber == 0) {
                    TextWord[][] texts = getPageText(pageNumber + 1, 0);
                    if (texts[0].length > 1) {
                        txt += texts[0][1].w;
                    } else {
                        txt += texts[0][0].w;
                    }
                } else if (firstNumber == 1) {
                    txt += getPageText(pageNumber, 2)[0][1].w;
                } else {
                    txt += getPageText(pageNumber + 1, 1)[0][1].w;
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        txt = TxtUtils.filterString(txt);


        AppState.get().selectedText = txt;
        invalidate();
        return txt;
    }

}
